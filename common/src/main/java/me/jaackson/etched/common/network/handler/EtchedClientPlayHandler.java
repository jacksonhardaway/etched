package me.jaackson.etched.common.network.handler;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import me.jaackson.etched.Etched;
import me.jaackson.etched.EtchedRegistry;
import me.jaackson.etched.client.screen.EtchingScreen;
import me.jaackson.etched.client.sound.JukeboxMinecartSoundInstance;
import me.jaackson.etched.client.sound.OnlineRecordSoundInstance;
import me.jaackson.etched.client.sound.StopListeningSound;
import me.jaackson.etched.client.sound.download.DownloadProgressListener;
import me.jaackson.etched.common.block.AlbumJukeboxBlock;
import me.jaackson.etched.common.blockentity.AlbumJukeboxBlockEntity;
import me.jaackson.etched.common.entity.MinecartJukebox;
import me.jaackson.etched.common.item.EtchedMusicDiscItem;
import me.jaackson.etched.common.network.ClientboundAddMinecartJukeboxPacket;
import me.jaackson.etched.common.network.ClientboundInvalidEtchUrlPacket;
import me.jaackson.etched.common.network.ClientboundPlayMinecartJukeboxMusicPacket;
import me.jaackson.etched.common.network.ClientboundPlayMusicPacket;
import me.jaackson.etched.mixin.client.GuiAccessor;
import me.jaackson.etched.mixin.client.LevelRendererAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.MinecartSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class EtchedClientPlayHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Int2ObjectArrayMap<SoundInstance> ENTITY_PLAYING_SOUNDS = new Int2ObjectArrayMap<>();

    public static void handlePlayMusicPacket(ClientboundPlayMusicPacket pkt) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingRecords();

        BlockPos pos = pkt.getPos();
        SoundInstance soundInstance = playingRecords.get(pos);
        if (soundInstance != null) {
            soundManager.stop(soundInstance);
            playingRecords.remove(pos);
        }

        if (!EtchedMusicDiscItem.isValidURL(pkt.getUrl())) {
            LOGGER.error("Server sent invalid music URL: " + pkt.getUrl());
            return;
        }

        playRecord(pos, new StopListeningSound(getEtchedRecord(pkt.getUrl(), pkt.getTitle(), level, pos), () -> Minecraft.getInstance().tell(() -> {
            if (level.getBlockState(pos).is(Blocks.JUKEBOX))
                for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(3.0D)))
                    livingEntity.setRecordPlayingNearby(pos, false);
        })));
    }

    public static void handleAddMinecartJukeboxPacket(ClientboundAddMinecartJukeboxPacket pkt) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

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
    }

    public static void handlePlayMinecartJukeboxPacket(ClientboundPlayMinecartJukeboxMusicPacket pkt) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        int entityId = pkt.getEntityId();
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        SoundInstance soundInstance = ENTITY_PLAYING_SOUNDS.get(entityId);
        if (soundInstance != null) {
            if (soundInstance instanceof StopListeningSound)
                ((StopListeningSound) soundInstance).stopListening();
            if (pkt.getAction() == ClientboundPlayMinecartJukeboxMusicPacket.Action.RESTART && soundManager.isActive(soundInstance))
                return;
            soundManager.stop(soundInstance);
            ENTITY_PLAYING_SOUNDS.remove(entityId);
        }

        if (pkt.getAction() == ClientboundPlayMinecartJukeboxMusicPacket.Action.STOP)
            return;

        Entity entity = level.getEntity(pkt.getEntityId());
        if (!(entity instanceof MinecartJukebox))
            return;

        if (pkt.getUrl() != null) {
            if (!EtchedMusicDiscItem.isValidURL(pkt.getUrl())) {
                LOGGER.error("Server sent invalid music URL: " + pkt.getUrl());
                return;
            }

            SoundInstance sound = getEtchedRecord(pkt.getUrl(), pkt.getTitle(), (MinecartJukebox) entity);
            ENTITY_PLAYING_SOUNDS.put(entityId, sound);
            soundManager.play(sound);
        } else {
            Item record = Registry.ITEM.byId(pkt.getRecordId());
            if (!(record instanceof RecordItem)) {
                LOGGER.error("Server sent invalid music disc: " + record);
                return;
            }

            Minecraft.getInstance().gui.setNowPlaying(((RecordItem) record).getDisplayName());
            SoundInstance sound = new JukeboxMinecartSoundInstance(((RecordItem) record).getSound(), (MinecartJukebox) entity);
            ENTITY_PLAYING_SOUNDS.put(entityId, sound);
            soundManager.play(sound);
        }
    }

    public static void handleSetInvalidEtch(ClientboundInvalidEtchUrlPacket pkt) {
        if (Minecraft.getInstance().screen instanceof EtchingScreen) {
            EtchingScreen screen = (EtchingScreen) Minecraft.getInstance().screen;
            screen.setReason(pkt.getException());
        }
    }

    private static SoundInstance getEtchedRecord(String url, Component title, MinecartJukebox jukebox) {
        return new OnlineRecordSoundInstance(url, jukebox, new MusicDownloadListener(title) {
            @Override
            public void onSuccess() {
                if (!jukebox.isAlive() || !ENTITY_PLAYING_SOUNDS.containsKey(jukebox.getId())) {
                    this.clearComponent();
                } else {
                    Minecraft.getInstance().gui.setNowPlaying(title);
                }
            }
        });
    }

    private static SoundInstance getEtchedRecord(String url, Component title, ClientLevel level, BlockPos pos) {
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingRecords();
        return new OnlineRecordSoundInstance(url, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new MusicDownloadListener(title) {
            @Override
            public void onSuccess() {
                if (!playingRecords.containsKey(pos)) {
                    this.clearComponent();
                } else {
                    Minecraft.getInstance().gui.setNowPlaying(title);
                    if (level.getBlockState(pos).is(Blocks.JUKEBOX))
                        for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(3.0D)))
                            livingEntity.setRecordPlayingNearby(pos, true);
                }
            }
        });
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

    /**
     * Plays the records in an album jukebox in order.
     *
     * @param jukebox The jukebox to play records
     * @param level   The level to play records in
     * @param pos     The position of the jukebox
     * @param force   Whether or not to force the jukebox to play
     */
    public static void playAlbum(AlbumJukeboxBlockEntity jukebox, ClientLevel level, BlockPos pos, boolean force) {
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingRecords();

        if (!level.getBlockState(pos).getValue(AlbumJukeboxBlock.POWERED) && !jukebox.recalculatePlayingIndex() && !force) // Something must already be playing since it would otherwise be -1 and a change would occur
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
        if (disc.getItem() == EtchedRegistry.ETCHED_MUSIC_DISC.get()) {
            Optional<EtchedMusicDiscItem.MusicInfo> optional = EtchedMusicDiscItem.getMusic(disc);
            if (optional.isPresent()) {
                EtchedMusicDiscItem.MusicInfo music = optional.get();
                if (EtchedMusicDiscItem.isValidURL(music.getUrl())) {
                    sound = new StopListeningSound(getEtchedRecord(music.getUrl(), music.getDisplayName(), level, pos), () -> Minecraft.getInstance().tell(() -> playNextRecord(level, pos)));
                }
            }
        }
        if (disc.getItem() instanceof RecordItem) {
            Minecraft.getInstance().gui.setNowPlaying(((RecordItem) disc.getItem()).getDisplayName());
            sound = new StopListeningSound(SimpleSoundInstance.forRecord(((RecordItem) disc.getItem()).getSound(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), () -> Minecraft.getInstance().tell(() -> playNextRecord(level, pos)));
        }

        if (sound == null)
            return;

        playRecord(pos, sound);

        if (disc.getItem() instanceof RecordItem && level.getBlockState(pos).is(Blocks.JUKEBOX))
            for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(3.0D)))
                livingEntity.setRecordPlayingNearby(pos, true);
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
        private float size;
        private Component requesting;
        private DownloadTextComponent component;

        protected MusicDownloadListener(Component title) {
            this.title = title;
        }

        private void setComponent(Component text) {
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
                this.setComponent(new TranslatableComponent("record." + Etched.MOD_ID + ".downloadProgress", String.format(Locale.ROOT, "%.2f", percentage / 100.0F * this.size), String.format(Locale.ROOT, "%.2f", this.size), title));
            }
        }

        @Override
        public void onFail() {
            Minecraft.getInstance().gui.setOverlayMessage(new TranslatableComponent("record." + Etched.MOD_ID + ".downloadFail", this.title), true);
        }
    }
}
