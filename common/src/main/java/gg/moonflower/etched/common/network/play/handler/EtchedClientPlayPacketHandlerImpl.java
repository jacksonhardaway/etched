package gg.moonflower.etched.common.network.play.handler;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.OnlineRecordSoundInstance;
import gg.moonflower.etched.api.sound.StopListeningSound;
import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.client.screen.AlbumJukeboxScreen;
import gg.moonflower.etched.client.screen.EtchingScreen;
import gg.moonflower.etched.client.screen.RadioScreen;
import gg.moonflower.etched.common.block.AlbumJukeboxBlock;
import gg.moonflower.etched.common.block.RadioBlock;
import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import gg.moonflower.etched.common.entity.MinecartJukebox;
import gg.moonflower.etched.common.network.play.*;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.mixin.client.GuiAccessor;
import gg.moonflower.etched.core.mixin.client.LevelRendererAccessor;
import gg.moonflower.pollen.api.network.packet.PollinatedPacketContext;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.MinecartSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.DoubleSupplier;

@ApiStatus.Internal
public class EtchedClientPlayPacketHandlerImpl implements EtchedClientPlayPacketHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Component RADIO = new TranslatableComponent("sound_source." + Etched.MOD_ID + ".radio");
    private static final Int2ObjectArrayMap<SoundInstance> ENTITY_PLAYING_SOUNDS = new Int2ObjectArrayMap<>();

    @Nullable
    public static SoundInstance getEntitySound(int entity) {
        return ENTITY_PLAYING_SOUNDS.get(entity);
    }

    public static SoundInstance getEtchedRecord(String url, Component title, Entity entity, boolean stream) {
        return new OnlineRecordSoundInstance(url, entity, new MusicDownloadListener(title, entity::getX, entity::getY, entity::getZ) {
            @Override
            public void onSuccess() {
                if (!entity.isAlive() || !ENTITY_PLAYING_SOUNDS.containsKey(entity.getId())) {
                    this.clearComponent();
                } else {
                    if (PlayableRecord.canShowMessage(entity.getX(), entity.getY(), entity.getZ()))
                        PlayableRecord.showMessage(title);
                }
            }

            @Override
            public void onFail() {
                PlayableRecord.showMessage(new TranslatableComponent("record." + Etched.MOD_ID + ".downloadFail", title));
            }
        }, stream ? AudioSource.AudioFileType.STREAM : AudioSource.AudioFileType.FILE);
    }

    private static SoundInstance getEtchedRecord(String url, Component title, ClientLevel level, BlockPos pos, boolean stream) {
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingRecords();
        return new OnlineRecordSoundInstance(url, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new MusicDownloadListener(title, () -> pos.getX() + 0.5, () -> pos.getY() + 0.5, () -> pos.getZ() + 0.5) {
            @Override
            public void onSuccess() {
                if (!playingRecords.containsKey(pos)) {
                    this.clearComponent();
                } else {
                    if (level.getBlockState(pos.above()).isAir() && PlayableRecord.canShowMessage(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5))
                        PlayableRecord.showMessage(title);
                    if (level.getBlockState(pos).is(Blocks.JUKEBOX))
                        for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(3.0D)))
                            livingEntity.setRecordPlayingNearby(pos, true);
                }
            }

            @Override
            public void onFail() {
                PlayableRecord.showMessage(new TranslatableComponent("record." + Etched.MOD_ID + ".downloadFail", title));
            }
        }, stream ? AudioSource.AudioFileType.STREAM : AudioSource.AudioFileType.FILE);
    }

    private static void playRecord(BlockPos pos, SoundInstance sound) {
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingRecords();
        playingRecords.put(pos, sound);
        soundManager.play(sound);
    }

    private static void playNextRecord(ClientLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AlbumJukeboxBlockEntity))
            return;

        AlbumJukeboxBlockEntity jukebox = (AlbumJukeboxBlockEntity) blockEntity;
        jukebox.next();
        playAlbum((AlbumJukeboxBlockEntity) blockEntity, level, pos, true);
    }

    private static void playJukeboxRecord(BlockPos pos, TrackData[] tracks, int track) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        if (track >= tracks.length) {
            if (level.getBlockState(pos).is(Blocks.JUKEBOX))
                for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(3.0D)))
                    livingEntity.setRecordPlayingNearby(pos, false);
            return;
        }

        TrackData trackData = tracks[track];
        if (trackData.getUrl() == null || !TrackData.isValidURL(trackData.getUrl())) {
            playJukeboxRecord(pos, tracks, track + 1);
            return;
        }
        playRecord(pos, StopListeningSound.create(getEtchedRecord(trackData.getUrl(), trackData.getDisplayName(), level, pos, false), () -> Minecraft.getInstance().tell(() -> {
            if (!((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingRecords().containsKey(pos))
                return;
            playJukeboxRecord(pos, tracks, track + 1);
        })));
    }

    private static void playEntityRecord(ItemStack record, int entityId, int track, boolean loop) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        Entity entity = level.getEntity(entityId);
        if (entity == null)
            return;

        Optional<SoundInstance> sound = ((PlayableRecord) record.getItem()).createEntitySound(record, entity, track);
        if (!sound.isPresent()) {
            if (loop && track != 0)
                playEntityRecord(record, entityId, 0, true);
            return;
        }

        SoundInstance entitySound = ENTITY_PLAYING_SOUNDS.remove(entity.getId());
        if (entitySound != null) {
            if (entitySound instanceof StopListeningSound)
                ((StopListeningSound) entitySound).stopListening();
            Minecraft.getInstance().getSoundManager().stop(entitySound);
        }

        entitySound = StopListeningSound.create(sound.get(), () -> Minecraft.getInstance().tell(() -> {
            ENTITY_PLAYING_SOUNDS.remove(entityId);
            playEntityRecord(record, entityId, track + 1, loop);
        }));

        ENTITY_PLAYING_SOUNDS.put(entityId, entitySound);
        Minecraft.getInstance().getSoundManager().play(entitySound);
    }

    public static void playBoombox(Entity entity, ItemStack record) {
        SoundInstance soundInstance = ENTITY_PLAYING_SOUNDS.remove(entity.getId());
        if (soundInstance != null) {
            if (soundInstance instanceof StopListeningSound)
                ((StopListeningSound) soundInstance).stopListening();
            Minecraft.getInstance().getSoundManager().stop(soundInstance);
        }
        if (!record.isEmpty())
            playEntityRecord(record, entity.getId(), 0, true);
    }

    /**
     * Plays the records on an album jukebox in order.
     *
     * @param url   The URL of the stream
     * @param level The level to play records in
     * @param pos   The position of the jukebox
     */
    public static void playRadio(@Nullable String url, ClientLevel level, BlockPos pos) {
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingRecords();

        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(RadioBlock.POWERED)) // Something must already be playing since it would otherwise be -1 and a change would occur
            return;

        SoundInstance soundInstance = playingRecords.get(pos);
        if (soundInstance != null) {
            if (soundInstance instanceof StopListeningSound)
                ((StopListeningSound) soundInstance).stopListening();
            soundManager.stop(soundInstance);
            playingRecords.remove(pos);
        }

        if (state.getValue(RadioBlock.POWERED))
            return;

        if (url != null && TrackData.isValidURL(url))
            playRecord(pos, getEtchedRecord(url, RADIO, level, pos, true));
    }

    /**
     * Plays the records on an album jukebox in order.
     *
     * @param jukebox The jukebox to play records
     * @param level   The level to play records in
     * @param pos     The position of the jukebox
     * @param force   Whether to force the jukebox to play
     */
    public static void playAlbum(AlbumJukeboxBlockEntity jukebox, ClientLevel level, BlockPos pos, boolean force) {
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingRecords();

        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(AlbumJukeboxBlock.POWERED) || !state.getValue(AlbumJukeboxBlock.POWERED) && !force && !jukebox.recalculatePlayingIndex(false)) // Something must already be playing since it would otherwise be -1 and a change would occur
            return;

        SoundInstance soundInstance = playingRecords.get(pos);
        if (soundInstance != null) {
            if (soundInstance instanceof StopListeningSound)
                ((StopListeningSound) soundInstance).stopListening();
            soundManager.stop(soundInstance);
            playingRecords.remove(pos);
        }

        if (level.getBlockState(pos).getValue(AlbumJukeboxBlock.POWERED))
            jukebox.stopPlaying();

        if (jukebox.getPlayingIndex() < 0) // Nothing can be played inside the jukebox
            return;

        ItemStack disc = jukebox.getItem(jukebox.getPlayingIndex());
        SoundInstance sound = null;
        if (disc.getItem() instanceof RecordItem) {
            if (level.getBlockState(pos.above()).isAir() && PlayableRecord.canShowMessage(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5))
                PlayableRecord.showMessage(((RecordItem) disc.getItem()).getDisplayName());
            sound = StopListeningSound.create(SimpleSoundInstance.forRecord(((RecordItem) disc.getItem()).getSound(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), () -> Minecraft.getInstance().tell(() -> playNextRecord(level, pos)));
        } else if (disc.getItem() instanceof PlayableRecord) {
            Optional<TrackData[]> optional = PlayableRecord.getStackMusic(disc);
            if (optional.isPresent()) {
                TrackData[] tracks = optional.get();
                TrackData track = jukebox.getTrack() < 0 || jukebox.getTrack() >= tracks.length ? tracks[0] : tracks[jukebox.getTrack()];
                if (TrackData.isValidURL(track.getUrl())) {
                    sound = StopListeningSound.create(getEtchedRecord(track.getUrl(), track.getDisplayName(), level, pos, false), () -> Minecraft.getInstance().tell(() -> playNextRecord(level, pos)));
                }
            }
        }

        if (sound == null)
            return;

        playRecord(pos, sound);

        if (disc.getItem() instanceof RecordItem && level.getBlockState(pos).is(Blocks.JUKEBOX))
            for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(3.0D)))
                livingEntity.setRecordPlayingNearby(pos, true);
    }

    @Override
    public void handlePlayMusicPacket(ClientboundPlayMusicPacket pkt, PollinatedPacketContext ctx) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingRecords();

        BlockPos pos = pkt.getPos();
        SoundInstance soundInstance = playingRecords.get(pos);
        ctx.enqueueWork(() -> {
            if (soundInstance != null) {
                soundManager.stop(soundInstance);
                playingRecords.remove(pos);
            }

            if (pkt.getTracks().length == 0)
                return;

            playJukeboxRecord(pos, pkt.getTracks(), 0);
        });
    }

    @Override
    public void handleAddMinecartJukeboxPacket(ClientboundAddMinecartJukeboxPacket pkt, PollinatedPacketContext ctx) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        ctx.enqueueWork(() -> {
            MinecartJukebox entity = new MinecartJukebox(level, pkt.getX(), pkt.getY(), pkt.getZ());
            int i = pkt.getId();
            entity.setPacketCoordinates(pkt.getX(), pkt.getY(), pkt.getZ());
            entity.moveTo(pkt.getX(), pkt.getY(), pkt.getZ());
            entity.xRot = (float) (pkt.getxRot() * 360) / 256.0F;
            entity.yRot = (float) (pkt.getyRot() * 360) / 256.0F;
            entity.setId(i);
            entity.setUUID(pkt.getUUID());
            level.putNonPlayerEntity(i, entity);
            Minecraft.getInstance().getSoundManager().play(new MinecartSoundInstance(entity));
        });
    }

    @Override
    public void handlePlayEntityMusicPacket(ClientboundPlayEntityMusicPacket pkt, PollinatedPacketContext ctx) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        int entityId = pkt.getEntityId();
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        SoundInstance soundInstance = ENTITY_PLAYING_SOUNDS.get(entityId);
        ctx.enqueueWork(() -> {
            if (soundInstance != null) {
                if (soundInstance instanceof StopListeningSound)
                    ((StopListeningSound) soundInstance).stopListening();
                if (pkt.getAction() == ClientboundPlayEntityMusicPacket.Action.RESTART && soundManager.isActive(soundInstance))
                    return;
                soundManager.stop(soundInstance);
                ENTITY_PLAYING_SOUNDS.remove(entityId);
            }

            if (pkt.getAction() == ClientboundPlayEntityMusicPacket.Action.STOP)
                return;

            Entity entity = level.getEntity(entityId);
            if (entity == null) {
                LOGGER.error("Server sent sound for nonexistent entity: " + entityId);
                return;
            }

            ItemStack record = pkt.getRecord();
            if (!PlayableRecord.isPlayableRecord(record)) {
                LOGGER.error("Server sent invalid music disc: " + record);
                return;
            }

            Optional<SoundInstance> sound = ((PlayableRecord) record.getItem()).createEntitySound(record, entity, 0);
            if (!sound.isPresent()) {
                LOGGER.error("Server sent invalid music disc: " + record);
                return;
            }

            SoundInstance entitySound = StopListeningSound.create(sound.get(), () -> Minecraft.getInstance().tell(() -> {
                ENTITY_PLAYING_SOUNDS.remove(entityId);
                playEntityRecord(record, entityId, 1, false);
            }));

            ENTITY_PLAYING_SOUNDS.put(entityId, entitySound);
            soundManager.play(entitySound);
        });
    }

    @Override
    public void handleSetInvalidEtch(ClientboundInvalidEtchUrlPacket pkt, PollinatedPacketContext ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof EtchingScreen) {
                EtchingScreen screen = (EtchingScreen) Minecraft.getInstance().screen;
                screen.setReason(pkt.getException());
            }
        });
    }

    @Override
    public void handleSetUrl(ClientboundSetUrlPacket pkt, PollinatedPacketContext ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof RadioScreen) {
                RadioScreen screen = (RadioScreen) Minecraft.getInstance().screen;
                screen.receiveUrl(pkt.getUrl());
            }
        });
    }

    @Override
    public void handleSetAlbumJukeboxTrack(SetAlbumJukeboxTrackPacket pkt, PollinatedPacketContext ctx) {
        Minecraft minecraft = Minecraft.getInstance();
        ctx.enqueueWork(() -> {
            if (minecraft.level != null && minecraft.screen instanceof AlbumJukeboxScreen) {
                AlbumJukeboxScreen screen = (AlbumJukeboxScreen) minecraft.screen;
                BlockPos pos = screen.getMenu().getPos();
                if (screen.getMenu().setPlayingTrack(minecraft.level, pkt))
                    EtchedClientPlayPacketHandlerImpl.playAlbum((AlbumJukeboxBlockEntity) Objects.requireNonNull(minecraft.level.getBlockEntity(pos)), minecraft.level, pos, true);
            }
        });
    }

    public static class DownloadTextComponent extends BaseComponent {

        private String text;
        private FormattedCharSequence visualOrderText;
        private Language decomposedWith;

        public DownloadTextComponent() {
            this.text = "";
        }

        @Override
        public String getContents() {
            return text;
        }

        @Override
        public TextComponent plainCopy() {
            return new TextComponent(this.text);
        }

        @Environment(EnvType.CLIENT)
        public FormattedCharSequence getVisualOrderText() {
            Language language = Language.getInstance();
            if (this.decomposedWith != language) {
                this.visualOrderText = language.getVisualOrder(this);
                this.decomposedWith = language;
            }

            return this.visualOrderText;
        }

        @Override
        public String toString() {
            return "TextComponent{text='" + this.text + '\'' + ", siblings=" + this.siblings + ", style=" + this.getStyle() + '}';
        }

        public void setText(String text) {
            this.text = text;
            this.decomposedWith = null;
        }
    }

    private static abstract class MusicDownloadListener implements DownloadProgressListener {

        private final Component title;
        private final DoubleSupplier x;
        private final DoubleSupplier y;
        private final DoubleSupplier z;
        private final BlockPos.MutableBlockPos pos;
        private float size;
        private Component requesting;
        private DownloadTextComponent component;

        protected MusicDownloadListener(Component title, DoubleSupplier x, DoubleSupplier y, DoubleSupplier z) {
            this.title = title;
            this.x = x;
            this.y = y;
            this.z = z;
            this.pos = new BlockPos.MutableBlockPos();
        }

        private BlockPos.MutableBlockPos getPos() {
            return this.pos.set(this.x.getAsDouble(), this.y.getAsDouble(), this.z.getAsDouble());
        }

        private void setComponent(Component text) {
            if (this.component == null && (Minecraft.getInstance().level == null || !Minecraft.getInstance().level.getBlockState(this.getPos().move(Direction.UP)).isAir() || !PlayableRecord.canShowMessage(this.x.getAsDouble(), this.y.getAsDouble(), this.z.getAsDouble())))
                return;

            if (this.component == null) {
                this.component = new DownloadTextComponent();
                Minecraft.getInstance().gui.setOverlayMessage(this.component, true);
                ((GuiAccessor) Minecraft.getInstance().gui).setOverlayMessageTime(Short.MAX_VALUE);
            }
            this.component.setText(text.getString());
        }

        protected void clearComponent() {
            if (((GuiAccessor) Minecraft.getInstance().gui).getOverlayMessageString() == this.component) {
                ((GuiAccessor) Minecraft.getInstance().gui).setOverlayMessageTime(60);
                this.component = null;
            }
        }

        @Override
        public void progressStartRequest(Component component) {
            this.requesting = component;
            this.setComponent(component);
        }

        @Override
        public void progressStartDownload(float size) {
            this.size = size;
            this.requesting = null;
            this.progressStagePercentage(0);
        }

        @Override
        public void progressStagePercentage(int percentage) {
            if (this.requesting != null) {
                this.setComponent(this.requesting.copy().append(" " + percentage + "%"));
            } else if (this.size != 0) {
                this.setComponent(new TranslatableComponent("record." + Etched.MOD_ID + ".downloadProgress", String.format(Locale.ROOT, "%.2f", percentage / 100.0F * this.size), String.format(Locale.ROOT, "%.2f", this.size), this.title));
            }
        }

        @Override
        public void progressStartLoading() {
            this.requesting = null;
            this.setComponent(new TranslatableComponent("record." + Etched.MOD_ID + ".loading", this.title));
        }

        @Override
        public void onFail() {
            Minecraft.getInstance().gui.setOverlayMessage(new TranslatableComponent("record." + Etched.MOD_ID + ".downloadFail", this.title), true);
        }
    }
}
