package gg.moonflower.etched.common.menu;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.download.SoundDownloadSource;
import gg.moonflower.etched.api.sound.download.SoundSourceManager;
import gg.moonflower.etched.common.item.*;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.ClientboundInvalidEtchUrlPacket;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import gg.moonflower.etched.core.registry.EtchedItems;
import gg.moonflower.etched.core.registry.EtchedMenus;
import gg.moonflower.etched.core.registry.EtchedSounds;
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
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author Ocelot, Jackson
 */
public class EtchingMenu extends AbstractContainerMenu {

    public static final ResourceLocation EMPTY_SLOT_MUSIC_DISC = new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_disc");
    public static final ResourceLocation EMPTY_SLOT_MUSIC_LABEL = new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_label");
    private static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile("\\s*;\\s*");
    private static final Cache<String, CompletableFuture<TrackData[]>> DATA_CACHE = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();
    private static final boolean IGNORE_CACHE = false;
    private static final Set<String> VALID_FORMATS;

    static {
        ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<>();
        builder.add("audio/wav", "audio/x-wav", "audio/opus", "application/ogg", "audio/ogg", "audio/mpeg", "application/octet-stream");
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
    private int urlId;
    private long lastSoundTime;
    private CompletableFuture<?> currentRequest;
    private int currentRequestId;

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
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_SLOT_MUSIC_DISC);
            }
        });
        this.labelSlot = this.addSlot(new Slot(this.input, 1, 62, 43) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SimpleMusicLabelItem;
            }

            @Override
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

        for (Map.Entry<String, String> entry : map.entrySet()) {
            httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException(httpURLConnection.getResponseCode() + " " + httpURLConnection.getResponseMessage());
        }

        String contentType = httpURLConnection.getContentType();
        if (!VALID_FORMATS.contains(CONTENT_TYPE_PATTERN.split(contentType.toLowerCase(Locale.ROOT))[0])) {
            throw new IOException("Unsupported Content-Type: " + contentType);
        }
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
        super.slotsChanged(container);
    }

    private void setupResultSlot() {
        Level level = this.player.level();
        if (level.isClientSide()) {
            return;
        }
        if (this.currentRequest != null && !this.currentRequest.isDone() && this.urlId == this.currentRequestId) {
            return;
        }

        EtchedMessages.PLAY.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.player), new ClientboundInvalidEtchUrlPacket(""));
        this.resultSlot.set(ItemStack.EMPTY);
        if (this.labelIndex.get() >= 0 && this.labelIndex.get() < EtchedMusicDiscItem.LabelPattern.values().length) {
            ItemStack discStack = this.discSlot.getItem();
            ItemStack labelStack = this.labelSlot.getItem();

            if (discStack.getItem() == EtchedItems.ETCHED_MUSIC_DISC.get() || (!discStack.isEmpty() && !labelStack.isEmpty())) {
                if (this.url == null && !discStack.isEmpty()) {
                    this.url = PlayableRecord.getStackAlbum(discStack).map(TrackData::getUrl).orElse(null);
                }
                if (!TrackData.isValidURL(this.url)) {
                    return;
                }

                int currentId = this.currentRequestId = this.urlId;
                this.currentRequest = CompletableFuture.supplyAsync(() -> {
                    ItemStack resultStack = new ItemStack(EtchedItems.ETCHED_MUSIC_DISC.get());
                    resultStack.setCount(1);

                    int discColor = 0x515151;
                    int primaryLabelColor = 0xFFFFFF;
                    int secondaryLabelColor = 0xFFFFFF;
                    TrackData[] data = new TrackData[]{TrackData.EMPTY};
                    if (discStack.getItem() == EtchedItems.ETCHED_MUSIC_DISC.get()) {
                        discColor = EtchedMusicDiscItem.getDiscColor(discStack);
                        primaryLabelColor = EtchedMusicDiscItem.getLabelPrimaryColor(discStack);
                        secondaryLabelColor = EtchedMusicDiscItem.getLabelSecondaryColor(discStack);
                        data = PlayableRecord.getStackMusic(discStack).orElse(data);
                    }
                    if (data.length == 1 && !labelStack.isEmpty()) {
                        data[0] = data[0].withTitle(MusicLabelItem.getTitle(labelStack)).withArtist(MusicLabelItem.getAuthor(labelStack));
                    }
                    if (SoundSourceManager.isValidUrl(this.url)) {
                        try {
                            if (IGNORE_CACHE) {
                                DATA_CACHE.invalidateAll();
                            }
                            data = DATA_CACHE.get(this.url, () -> SoundSourceManager.resolveTracks(this.url, null, Proxy.NO_PROXY)).join();
                        } catch (Exception e) {
                            if (!level.isClientSide()) {
                                EtchedMessages.PLAY.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.player), new ClientboundInvalidEtchUrlPacket(e instanceof CompletionException ? e.getCause().getMessage() : e.getMessage()));
                            }
                            if (e instanceof CompletionException) {
                                throw (CompletionException) e;
                            }
                            throw new CompletionException(e);
                        }
                    } else if (!TrackData.isLocalSound(this.url)) {
                        try {
                            checkStatus(this.url);
                            data = new TrackData[]{data[0].withUrl(this.url)};
                        } catch (UnknownHostException e) {
                            if (!level.isClientSide()) {
                                EtchedMessages.PLAY.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.player), new ClientboundInvalidEtchUrlPacket("Unknown host: " + this.url));
                            }
                            throw new CompletionException("Invalid URL", e);
                        } catch (Exception e) {
                            if (!level.isClientSide()) {
                                EtchedMessages.PLAY.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.player), new ClientboundInvalidEtchUrlPacket(e.getLocalizedMessage()));
                            }
                            throw new CompletionException("Invalid URL", e);
                        }
                    }
                    if (discStack.getItem() instanceof BlankMusicDiscItem) {
                        discColor = ((BlankMusicDiscItem) discStack.getItem()).getColor(discStack);
                    }
                    if (labelStack.getItem() instanceof MusicLabelItem) {
                        primaryLabelColor = MusicLabelItem.getLabelColor(labelStack);
                        secondaryLabelColor = primaryLabelColor;
                    } else if (labelStack.getItem() instanceof ComplexMusicLabelItem) {
                        primaryLabelColor = ComplexMusicLabelItem.getPrimaryColor(labelStack);
                        secondaryLabelColor = ComplexMusicLabelItem.getSecondaryColor(labelStack);
                    }

                    for (int i = 0; i < data.length; i++) {
                        TrackData trackData = data[i];
                        if (trackData.getArtist().equals(TrackData.EMPTY.getArtist())) {
                            trackData = trackData.withArtist(MusicLabelItem.getAuthor(labelStack));
                        }
                        if (TrackData.isLocalSound(this.url)) {
                            trackData = trackData.withUrl(new ResourceLocation(this.url).toString());
                        }
                        data[i] = trackData;
                    }

                    EtchedMusicDiscItem.setMusic(resultStack, data);
                    EtchedMusicDiscItem.setColor(resultStack, discColor, primaryLabelColor, secondaryLabelColor);
                    EtchedMusicDiscItem.setPattern(resultStack, EtchedMusicDiscItem.LabelPattern.values()[this.labelIndex.get()]);

                    return resultStack;
                }, HttpUtil.DOWNLOAD_EXECUTOR).thenAcceptAsync(resultStack -> {
                    if (this.urlId == currentId && !ItemStack.matches(resultStack, this.resultSlot.getItem()) && !ItemStack.matches(resultStack, this.discSlot.getItem())) {
                        this.resultSlot.set(resultStack);
                        this.urlId++;
                        this.urlId %= 1000;
                        this.broadcastChanges();
                    }
                }, level.getServer()).exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
            }
        }
    }

    public int getLabelIndex() {
        return this.labelIndex.get();
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
            this.setupResultSlot();
        }
    }
}