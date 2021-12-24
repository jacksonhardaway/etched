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
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @author Jackson
 */
public class EtchingMenu extends AbstractContainerMenu {

    public static final ResourceLocation EMPTY_SLOT_MUSIC_DISC = new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_disc");
    public static final ResourceLocation EMPTY_SLOT_MUSIC_LABEL = new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_label");
    private static final Logger LOGGER = LogManager.getLogger();
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
    private String cachedAuthor;
    private String cachedTitle;
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
                return stack.getItem() instanceof MusicLabelItem;
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
            public ItemStack onTake(Player player, ItemStack stack) {
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
                return super.onTake(player, stack);
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
        HttpURLConnection httpURLConnection = null;

        try {
            URL uRL = new URL(url);
            httpURLConnection = (HttpURLConnection) uRL.openConnection(Proxy.NO_PROXY);
            httpURLConnection.setInstanceFollowRedirects(true);
            Map<String, String> map = SoundDownloadSource.getDownloadHeaders();

            for (Map.Entry<String, String> entry : map.entrySet())
                httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());

            if (httpURLConnection.getResponseCode() != 200) {
                IOUtils.closeQuietly(httpURLConnection.getInputStream());
                throw new IOException(httpURLConnection.getResponseCode() + " " + httpURLConnection.getResponseMessage());
            }

            String contentType = httpURLConnection.getContentType();
            if (!VALID_FORMATS.contains(contentType))
                throw new IOException("Unsupported Content-Type: " + contentType);
        } catch (Throwable e) {
            if (httpURLConnection != null) {
                try {
                    LOGGER.error(IOUtils.toString(httpURLConnection.getErrorStream(), StandardCharsets.UTF_8));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            throw new IOException(e);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, level, this.input));
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
        if (slot != null && slot.hasItem()) {
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
                    int labelColor = 0xFFFFFF;
                    String author = this.player.getDisplayName().getString();
                    String title = null;
                    if (discStack.getItem() == EtchedItems.ETCHED_MUSIC_DISC.get()) {
                        discColor = EtchedMusicDiscItem.getPrimaryColor(discStack);
                        labelColor = EtchedMusicDiscItem.getSecondaryColor(discStack);
                        author = EtchedMusicDiscItem.getMusic(discStack).map(EtchedMusicDiscItem.MusicInfo::getAuthor).orElse(null);
                        title = EtchedMusicDiscItem.getMusic(discStack).map(EtchedMusicDiscItem.MusicInfo::getTitle).orElse(null);
                    }
                    if (!labelStack.isEmpty() && labelStack.hasCustomHoverName())
                        title = labelStack.getHoverName().getString();
                    if (SoundSourceManager.isValidUrl(this.url)) {
                        if (this.cachedAuthor == null || this.cachedTitle == null) {
                            try {
                                SoundSourceManager.resolveTrack(this.url, null, Proxy.NO_PROXY).ifPresent(track -> {
                                    this.cachedAuthor = track.getFirst();
                                    this.cachedTitle = track.getSecond();
                                });
                            } catch (Exception e) {
                                this.cachedAuthor = null;
                                this.cachedTitle = null;

                                if (!this.player.level.isClientSide())
                                    EtchedMessages.PLAY.sendTo((ServerPlayer) this.player, new ClientboundInvalidEtchUrlPacket(e.getMessage()));
                                throw new CompletionException("Failed to connect to SoundCloud API", e);
                            }
                        }
                        author = this.cachedAuthor;
                        title = this.cachedTitle;
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
                    if (labelStack.getItem() instanceof MusicLabelItem)
                        labelColor = ((MusicLabelItem) labelStack.getItem()).getColor(labelStack);

                    EtchedMusicDiscItem.MusicInfo info = new EtchedMusicDiscItem.MusicInfo();
                    info.setAuthor(author != null ? author : this.player.getDisplayName().getString());
                    if (title != null)
                        info.setTitle(title);
                    info.setUrl(EtchedMusicDiscItem.isLocalSound(this.url) ? new ResourceLocation(this.url).toString() : this.url);

                    EtchedMusicDiscItem.setMusic(resultStack, info);
                    EtchedMusicDiscItem.setColor(resultStack, discColor, labelColor);
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
            this.cachedAuthor = null;
            this.cachedTitle = null;
            this.setupResultSlot();
        }
    }
}
