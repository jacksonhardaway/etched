package gg.moonflower.etched.common.menu;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import gg.moonflower.etched.api.sound.download.SoundDownloadSource;
import gg.moonflower.etched.api.sound.download.SoundSourceManager;
import gg.moonflower.etched.common.item.BlankMusicDiscItem;
import gg.moonflower.etched.common.item.EtchedMusicDiscItem;
import gg.moonflower.etched.common.item.MusicLabelItem;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.ClientboundInvalidEtchUrlPacket;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import gg.moonflower.etched.core.registry.EtchedItems;
import gg.moonflower.etched.core.registry.EtchedMenus;
import gg.moonflower.etched.core.registry.EtchedSounds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @author Jackson
 */
public class EtchingMenu extends AbstractContainerMenu {

    public static final ResourceLocation EMPTY_SLOT_MUSIC_DISC = new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_disc");
    public static final ResourceLocation EMPTY_SLOT_MUSIC_LABEL = new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_label");
    private static final Set<String> VALID_FORMATS;

    static {
        ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<>();
        builder.add("audio/wav", "audio/opus", "application/ogg", "audio/ogg", "audio/mpeg", "application/octet-stream");
        VALID_FORMATS = builder.build();
    }

    private final ContainerLevelAccess access;
    private final DataSlot labelIndex;
    private final Slot discSlot;
    private final Slot labelSlot;
    private final Slot resultSlot;
    private final Container input;
    private final Container result;
    private final Player player;
    private String url;
    private SoundDownloadSource.TrackData cachedData;
    private int urlId;
    private long lastSoundTime;

    public EtchingMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public EtchingMenu(int id, Inventory inventory, ContainerLevelAccess containerLevelAccess) {
        super(EtchedMenus.ETCHING_MENU.get(), id);
        this.player = inventory.player;
        this.labelIndex = DataSlot.standalone();
        this.input = new SimpleContainer(2) {
            @Override
            public void setChanged() {
                super.setChanged();
                EtchingMenu.this.slotsChanged(this);
            }
        };
        this.result = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
            }
        };

        this.access = containerLevelAccess;

        this.discSlot = this.addSlot(new Slot(this.input, 0, 44, 43) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == EtchedItems.BLANK_MUSIC_DISC.get() || stack.getItem() == EtchedItems.ETCHED_MUSIC_DISC.get();
            }

            @Override
            @Environment(EnvType.CLIENT)
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_SLOT_MUSIC_DISC);
            }
        });
        this.labelSlot = this.addSlot(new Slot(this.input, 1, 62, 43) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof MusicLabelItem || stack.getItem() == EtchedItems.COMPLEX_MUSIC_LABEL.get();
            }

            @Override
            @Environment(EnvType.CLIENT)
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_SLOT_MUSIC_LABEL);
            }
        });

        this.resultSlot = this.addSlot(new Slot(this.result, 0, 116, 43) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                EtchingMenu.this.discSlot.remove(1);
                EtchingMenu.this.labelSlot.remove(1);
                if (!EtchingMenu.this.discSlot.hasItem() || !EtchingMenu.this.labelSlot.hasItem()) {
                    EtchingMenu.this.labelIndex.set(0);
                }

                EtchingMenu.this.setupResultSlot();
                EtchingMenu.this.broadcastChanges();

                containerLevelAccess.execute((level, pos) -> {
                    long l = level.getGameTime();
                    if (EtchingMenu.this.lastSoundTime != l) {
                        level.playSound(null, pos, EtchedSounds.UI_ETCHER_TAKE_RESULT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                        EtchingMenu.this.lastSoundTime = l;
                    }

                });
                super.onTake(player, stack);
            }
        });

        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(inventory, x + y * 9 + 9, 8 + x * 18, 98 + y * 18));
            }
        }
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(inventory, x, 8 + x * 18, 156));
        }

        this.addDataSlot(this.labelIndex);
    }

    private static void checkStatus(String url) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection(Proxy.NO_PROXY);
        httpURLConnection.setRequestMethod("HEAD");
        httpURLConnection.setInstanceFollowRedirects(true);
        Map<String, String> map = SoundDownloadSource.getDownloadHeaders();

        for (Map.Entry<String, String> entry : map.entrySet())
            httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());

        if (httpURLConnection.getResponseCode() != 200) {
            throw new IOException(httpURLConnection.getResponseCode() + " " + httpURLConnection.getResponseMessage());
        }

        String contentType = httpURLConnection.getContentType();
        if (!VALID_FORMATS.contains(contentType))
            throw new IOException("Unsupported Content-Type: " + contentType);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.input));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, EtchedBlocks.ETCHING_TABLE.get());
    }

    @Override
    public boolean clickMenuButton(Player player, int index) {
        if (index >= 0 && index < EtchedMusicDiscItem.LabelPattern.values().length) {
            this.labelIndex.set(index);
            this.setupResultSlot();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (index < 3) {
                if (!this.moveItemStackTo(itemStack2, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 0, 3, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemStack2);
        }

        return itemStack;
    }

    @Override
    public void slotsChanged(Container container) {
        ItemStack discStack = this.discSlot.getItem();
        ItemStack labelStack = this.labelSlot.getItem();
        ItemStack resultStack = this.resultSlot.getItem();

        if (resultStack.isEmpty() && labelStack.isEmpty()) {
            if (!discStack.isEmpty() && discStack.getItem() == EtchedItems.ETCHED_MUSIC_DISC.get()) {
                this.labelIndex.set(EtchedMusicDiscItem.getPattern(discStack).ordinal());
            } else {
                this.labelIndex.set(0);
            }
        }

        this.setupResultSlot();
        this.broadcastChanges();
    }

    private void setupResultSlot() {
        if (!this.player.level.isClientSide())
            EtchedMessages.PLAY.sendTo((ServerPlayer) this.player, new ClientboundInvalidEtchUrlPacket(""));
        this.resultSlot.set(ItemStack.EMPTY);
        if (this.labelIndex.get() >= 0 && this.labelIndex.get() < EtchedMusicDiscItem.LabelPattern.values().length && this.url != null && EtchedMusicDiscItem.isValidURL(this.url)) {
            ItemStack discStack = this.discSlot.getItem();
            ItemStack labelStack = this.labelSlot.getItem();

            if (discStack.getItem() == EtchedItems.ETCHED_MUSIC_DISC.get() || (!discStack.isEmpty() && !labelStack.isEmpty())) {
                int currentId = this.urlId;
                CompletableFuture.supplyAsync(() -> {
                    ItemStack resultStack = new ItemStack(EtchedItems.ETCHED_MUSIC_DISC.get());
                    resultStack.setCount(1);

                    int discColor = 0x515151;
                    int primaryLabelColor = 0xFFFFFF;
                    int secondaryLabelColor = 0xFFFFFF;
                    String author = this.player.getDisplayName().getString();
                    String title = null;
                    boolean album = false;
                    if (discStack.getItem() == EtchedItems.ETCHED_MUSIC_DISC.get()) {
                        discColor = EtchedMusicDiscItem.getDiscColor(discStack);
                        primaryLabelColor = EtchedMusicDiscItem.getLabelPrimaryColor(discStack);
                        secondaryLabelColor = EtchedMusicDiscItem.getLabelSecondaryColor(discStack);
                        Optional<EtchedMusicDiscItem.MusicInfo> musicInfo = EtchedMusicDiscItem.getMusic(discStack);
                        author = musicInfo.map(EtchedMusicDiscItem.MusicInfo::getAuthor).orElse(null);
                        title = musicInfo.map(EtchedMusicDiscItem.MusicInfo::getTitle).orElse(null);
                        album = musicInfo.map(EtchedMusicDiscItem.MusicInfo::isAlbum).orElse(false);
                    }
                    if (!labelStack.isEmpty() && labelStack.hasCustomHoverName())
                        title = labelStack.getHoverName().getString();
                    if (SoundSourceManager.isValidUrl(this.url)) {
                        if (this.cachedData == null) {
                            try {
                                SoundSourceManager.resolveTrack(this.url, null, Proxy.NO_PROXY).ifPresent(data -> this.cachedData = data);
                            } catch (Exception e) {
                                this.cachedData = null;

                                if (!this.player.level.isClientSide())
                                    EtchedMessages.PLAY.sendTo((ServerPlayer) this.player, new ClientboundInvalidEtchUrlPacket(e.getMessage()));
                                throw new CompletionException(e);
                            }
                        }
                        if (this.cachedData != null) {
                            author = this.cachedData.getArtist();
                            title = this.cachedData.getTitle();
                            album = this.cachedData.isAlbum();
                        } else {
                            author = null;
                            title = null;
                        }
                    } else if (!EtchedMusicDiscItem.isLocalSound(this.url)) {
                        try {
                            checkStatus(this.url);
                        } catch (UnknownHostException e) {
                            if (!this.player.level.isClientSide())
                                EtchedMessages.PLAY.sendTo((ServerPlayer) this.player, new ClientboundInvalidEtchUrlPacket("Unknown host: " + this.url));
                            throw new CompletionException("Invalid URL", e);
                        } catch (Exception e) {
                            if (!this.player.level.isClientSide())
                                EtchedMessages.PLAY.sendTo((ServerPlayer) this.player, new ClientboundInvalidEtchUrlPacket(e.getLocalizedMessage()));
                            throw new CompletionException("Invalid URL", e);
                        }
                    }
                    if (discStack.getItem() instanceof BlankMusicDiscItem)
                        discColor = ((BlankMusicDiscItem) discStack.getItem()).getColor(discStack);
                    if (labelStack.getItem() instanceof MusicLabelItem || labelStack.getItem() == EtchedItems.COMPLEX_MUSIC_LABEL.get()) {
                        primaryLabelColor = MusicLabelItem.getPrimaryColor(labelStack);
                        secondaryLabelColor = MusicLabelItem.getSecondaryColor(labelStack);
                    }

                    EtchedMusicDiscItem.MusicInfo info = new EtchedMusicDiscItem.MusicInfo();
                    info.setAuthor(author != null ? author : this.player.getDisplayName().getString());
                    if (title != null)
                        info.setTitle(title);
                    info.setAlbum(album);
                    info.setUrl(EtchedMusicDiscItem.isLocalSound(this.url) ? new ResourceLocation(this.url).toString() : this.url);

                    EtchedMusicDiscItem.setMusic(resultStack, info);
                    EtchedMusicDiscItem.setColor(resultStack, discColor, primaryLabelColor, secondaryLabelColor);
                    EtchedMusicDiscItem.setPattern(resultStack, EtchedMusicDiscItem.LabelPattern.values()[this.labelIndex.get()]);

                    return resultStack;
                }, HttpUtil.DOWNLOAD_EXECUTOR).thenAcceptAsync(resultStack -> {
                    if (this.urlId == currentId && !ItemStack.matches(resultStack, this.resultSlot.getItem()) && !ItemStack.matches(resultStack, this.discSlot.getItem())) {
                        this.resultSlot.set(resultStack);
                    }
                }).exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
            }
        }
    }

    public int getLabelIndex() {
        return labelIndex.get();
    }

    /**
     * Sets the URL for the resulting stack to the specified value.
     *
     * @param string The new URL
     */
    public void setUrl(String string) {
        if (!Objects.equals(this.url, string)) {
            this.url = string;
            this.urlId++;
            this.urlId %= 1000;
            this.cachedData = null;
            this.setupResultSlot();
        }
    }
}
