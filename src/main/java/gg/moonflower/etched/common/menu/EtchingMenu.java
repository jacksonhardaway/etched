package gg.moonflower.etched.common.menu;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.download.SoundDownloadSource;
import gg.moonflower.etched.api.sound.download.SoundSourceManager;
import gg.moonflower.etched.common.component.DiscAppearanceComponent;
import gg.moonflower.etched.common.component.MusicLabelComponent;
import gg.moonflower.etched.common.component.MusicTrackComponent;
import gg.moonflower.etched.common.item.MusicLabelItem;
import gg.moonflower.etched.common.network.play.ClientboundInvalidEtchUrlPacket;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.registry.*;
import net.minecraft.ResourceLocationException;
import net.minecraft.Util;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author Ocelot, Jackson
 */
public class EtchingMenu extends AbstractContainerMenu implements UrlMenu {

    public static final ResourceLocation EMPTY_SLOT_MUSIC_DISC = Etched.etchedPath("item/empty_etching_table_slot_music_disc");
    public static final ResourceLocation EMPTY_SLOT_MUSIC_LABEL = Etched.etchedPath("item/empty_etching_table_slot_music_label");
    private static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile("\\s*;\\s*");
    private static final Cache<String, CompletableFuture<TrackData[]>> DATA_CACHE = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();
    private static final boolean IGNORE_CACHE = false;
    private static final Set<String> VALID_FORMATS;

    static {
        ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<>();
        builder.add("audio/wav", "audio/x-wav", "audio/opus", "application/ogg", "audio/ogg", "audio/mpeg", "audio/mp3", "application/octet-stream", "application/binary");
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
                return stack.getItem() instanceof MusicLabelItem;
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
                stack.getItem().onCraftedBy(stack, player.level(), player);
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

    private static void checkStatus(String url) throws IOException, URISyntaxException {
        URL uri = new URI(url).toURL();
        HttpURLConnection httpURLConnection = (HttpURLConnection) uri.openConnection(Proxy.NO_PROXY);
        if (!uri.getHost().equals("www.dropbox.com")) { // Hack for dropbox returning the wrong content type for head requests
            httpURLConnection.setRequestMethod("HEAD");
        }
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
        if (index >= 0 && index < DiscAppearanceComponent.LabelPattern.values().length) {
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
            DiscAppearanceComponent discAppearance = discStack.get(EtchedComponents.DISC_APPEARANCE);
            if (discAppearance != null) {
                this.labelIndex.set(discAppearance.pattern().ordinal());
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

        PacketDistributor.sendToPlayer((ServerPlayer) this.player, new ClientboundInvalidEtchUrlPacket(""));
        this.resultSlot.set(ItemStack.EMPTY);
        if (this.labelIndex.get() >= 0 && this.labelIndex.get() < DiscAppearanceComponent.LabelPattern.values().length) {
            ItemStack discStack = this.discSlot.getItem().copy();
            ItemStack labelStack = this.labelSlot.getItem().copy();

            if (discStack.is(EtchedItems.ETCHED_MUSIC_DISC.get()) || (!discStack.isEmpty() && !labelStack.isEmpty())) {
                TrackData recordData = TrackData.EMPTY;
                Optional<TrackData> optional = PlayableRecord.getAlbum(discStack);
                if (optional.isPresent()) {
                    recordData = optional.get();
                    if (this.url == null) {
                        this.url = recordData.url();
                    }
                }
                if (!TrackData.isValidURL(this.url)) {
                    return;
                }

                MusicLabelComponent label = labelStack.get(EtchedComponents.MUSIC_LABEL);
                if (label != null) {
                    recordData = recordData.withArtist(label.artist()).withTitle(label.title());
                }

                TrackData album = recordData;
                int currentId = this.currentRequestId = this.urlId;
                this.currentRequest = CompletableFuture.supplyAsync(() -> {
                    ItemStack resultStack;
                    if (discStack.is(EtchedItems.ETCHED_MUSIC_DISC.get())) {
                        resultStack = discStack.copyWithCount(1);
                        resultStack.remove(EtchedComponents.ALBUM);
                    } else {
                        resultStack = new ItemStack(EtchedItems.ETCHED_MUSIC_DISC.get());
                    }

                    int discColor = 0x515151;
                    int primaryLabelColor = 0xFFFFFF;
                    int secondaryLabelColor = 0xFFFFFF;

                    DiscAppearanceComponent discAppearance = resultStack.get(EtchedComponents.DISC_APPEARANCE);
                    if (discAppearance != null) {
                        discColor = discAppearance.discColor();
                        primaryLabelColor = discAppearance.labelPrimaryColor();
                        secondaryLabelColor = discAppearance.labelSecondaryColor();
                    }

                    if (label != null) {
                        primaryLabelColor = label.primaryColor();
                        secondaryLabelColor = label.secondaryColor();
                    }

                    TrackData[] data = new TrackData[]{album};

                    if (SoundSourceManager.isValidUrl(this.url)) {
                        try {
                            if (IGNORE_CACHE) {
                                DATA_CACHE.invalidateAll();
                            }
                            TrackData[] tracks = DATA_CACHE.get(this.url, () -> SoundSourceManager.resolveTracks(this.url, null, Proxy.NO_PROXY)).join();
                            if (tracks.length == 1) {
                                data = tracks;
                            } else {
                                resultStack.set(EtchedComponents.ALBUM, tracks[0]);
                                data = Arrays.copyOfRange(tracks, 1, tracks.length);
                            }
                        } catch (Exception e) {
                            PacketDistributor.sendToPlayer((ServerPlayer) this.player, new ClientboundInvalidEtchUrlPacket(e instanceof CompletionException ? e.getCause().getMessage() : e.getMessage()));
                            if (e instanceof CompletionException ex) {
                                throw ex;
                            }
                            throw new CompletionException(e);
                        }
                    } else if (!TrackData.isLocalSound(this.url)) {
                        try {
                            checkStatus(this.url);
                            data[0] = data[0].withUrl(this.url);
                        } catch (UnknownHostException e) {
                            PacketDistributor.sendToPlayer((ServerPlayer) this.player, new ClientboundInvalidEtchUrlPacket("Unknown host: " + this.url));
                            throw new CompletionException("Invalid URL", e);
                        } catch (Exception e) {
                            PacketDistributor.sendToPlayer((ServerPlayer) this.player, new ClientboundInvalidEtchUrlPacket(e.getMessage()));
                            throw new CompletionException("Invalid URL", e);
                        }
                    } else {
                        try {
                            data[0] = data[0].withUrl(ResourceLocation.parse(this.url).toString());
                        } catch (ResourceLocationException e) {
                            PacketDistributor.sendToPlayer((ServerPlayer) this.player, new ClientboundInvalidEtchUrlPacket(e.getMessage()));
                            throw new CompletionException("Invalid Location", e);
                        }
                    }

                    DyedItemColor discStackColor = discStack.get(DataComponents.DYED_COLOR);
                    if (discStackColor != null) {
                        discColor = discStackColor.rgb();
                    }

                    for (int i = 0; i < data.length; i++) {
                        TrackData trackData = data[i];
                        if (trackData.artist().equals(TrackData.EMPTY.artist())) {
                            data[i] = trackData.withArtist(album.artist());
                        }
                    }

                    resultStack.set(EtchedComponents.MUSIC, new MusicTrackComponent(Arrays.asList(data)));
                    resultStack.set(EtchedComponents.DISC_APPEARANCE, new DiscAppearanceComponent(DiscAppearanceComponent.LabelPattern.values()[this.labelIndex.get()], discColor, primaryLabelColor, secondaryLabelColor));

                    return resultStack;
                }, Util.nonCriticalIoPool()).thenAcceptAsync(resultStack -> {
                    if (this.urlId == currentId &&
                            !ItemStack.matches(resultStack, this.discSlot.getItem()) &&
                            ItemStack.matches(discStack, this.discSlot.getItem()) &&
                            ItemStack.matches(labelStack, this.labelSlot.getItem())
                    ) {
                        this.resultSlot.set(resultStack);
                        this.urlId++;
                        this.urlId %= 1000;
                        this.broadcastChanges();
                    }
                }, level.getServer()).exceptionally(e -> {
                    Etched.LOGGER.debug("Error setting disc URL", e);
                    return null;
                });
            }
        }
    }

    public int getLabelIndex() {
        return this.labelIndex.get();
    }

    @Override
    public void setUrl(String string) {
        if (!Objects.equals(this.url, string)) {
            this.url = string;
            this.urlId++;
            this.urlId %= 1000;
            this.setupResultSlot();
        }
    }
}