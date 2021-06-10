package me.jaackson.etched.common.network.handler;

import me.jaackson.etched.Etched;
import me.jaackson.etched.EtchedRegistry;
import me.jaackson.etched.client.sound.DownloadProgressListener;
import me.jaackson.etched.client.sound.OnlineRecordSoundInstance;
import me.jaackson.etched.client.sound.StopListeningSound;
import me.jaackson.etched.common.blockentity.AlbumJukeboxBlockEntity;
import me.jaackson.etched.common.item.EtchedMusicDiscItem;
import me.jaackson.etched.common.network.ClientboundPlayMusicPacket;
import me.jaackson.etched.mixin.client.GuiAccessor;
import me.jaackson.etched.mixin.client.LevelRendererAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class EtchedClientPlayHandler {

    private static final Logger LOGGER = LogManager.getLogger();

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

        playRecord(level, pos, getEtchedRecord(pkt.getUrl(), pkt.getTitle(), pos));
    }

    private static SoundInstance getEtchedRecord(String url, Component title, BlockPos pos) {
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingRecords();
        SoundInstance soundInstance = new OnlineRecordSoundInstance(url, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundSource.RECORDS, new DownloadProgressListener() {
            private float size;
            private Component requesting;
            private DownloadTextComponent component;

            private void setComponent(Component text) {
                if (this.component == null) {
                    this.component = new DownloadTextComponent();
                    Minecraft.getInstance().gui.setOverlayMessage(this.component, true);
                    ((GuiAccessor) Minecraft.getInstance().gui).setOverlayMessageTime(Short.MAX_VALUE);
                }
                this.component.setText(text.getString());
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
            public void onSuccess() {
                if (!playingRecords.containsKey(pos)) {
                    if (((GuiAccessor) Minecraft.getInstance().gui).getOverlayMessageString() == this.component) {
                        ((GuiAccessor) Minecraft.getInstance().gui).setOverlayMessageTime(60);
                        this.component = null;
                    }
                } else {
                    Minecraft.getInstance().gui.setNowPlaying(title);
                }
            }

            @Override
            public void onFail() {
                Minecraft.getInstance().gui.setOverlayMessage(new TranslatableComponent("record." + Etched.MOD_ID + ".downloadFail", title), true);
            }
        });
        return soundInstance;
    }

    private static void playRecord(ClientLevel level, BlockPos pos, SoundInstance sound) {
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingRecords();

        playingRecords.put(pos, sound);
        soundManager.play(sound);

        for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(3.0D)))
            livingEntity.setRecordPlayingNearby(pos, true);
    }

    private static void playNextRecord(BlockPos pos) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

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

        if (!force && !jukebox.recalculatePlayingIndex()) // Something must already be playing since it would otherwise be -1 and a change would occur
            return;

        SoundInstance soundInstance = playingRecords.get(pos);
        if (soundInstance != null) {
            soundManager.stop(soundInstance);
            playingRecords.remove(pos);
        }

        if (jukebox.getPlayingIndex() < 0) // Nothing can be played inside the jukebox
            return;

        ItemStack disc = jukebox.getItem(jukebox.getPlayingIndex());
        SoundInstance sound = null;
        if (disc.getItem() == EtchedRegistry.ETCHED_MUSIC_DISC.get()) {
            Optional<EtchedMusicDiscItem.MusicInfo> optional = EtchedMusicDiscItem.getMusic(disc);
            if (optional.isPresent()) {
                EtchedMusicDiscItem.MusicInfo music = optional.get();
                if (EtchedMusicDiscItem.isValidURL(optional.get().getUrl())) {
                    sound = new StopListeningSound(getEtchedRecord(music.getUrl(), music.getDisplayName(), pos), () -> Minecraft.getInstance().execute(() -> playNextRecord(pos)));
                }
            }
        }
        if (disc.getItem() instanceof RecordItem) {
            sound = new StopListeningSound(SimpleSoundInstance.forRecord(((RecordItem) disc.getItem()).getSound(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), () -> Minecraft.getInstance().execute(() -> playNextRecord(pos)));
        }

        if (sound == null)
            return;

        playRecord(level, pos, sound);
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
}
