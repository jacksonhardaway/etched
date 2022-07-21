package gg.moonflower.etched.api.sound;

import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.sound.stream.MonoWrapper;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.core.Etched;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ocelot
 */
public class AbstractOnlineSoundInstance extends AbstractSoundInstance {

    private final String url;
    private final String subtitle;
    private final int attenuationDistance;
    private final DownloadProgressListener progressListener;
    private final AudioSource.AudioFileType type;
    private final boolean stereo;

    public AbstractOnlineSoundInstance(String url, @Nullable String subtitle, int attenuationDistance, SoundSource source, DownloadProgressListener progressListener, AudioSource.AudioFileType type, boolean stereo) {
        super(new ResourceLocation(Etched.MOD_ID, DigestUtils.sha1Hex(url)), source);
        this.url = url;
        this.subtitle = subtitle;
        this.attenuationDistance = attenuationDistance;
        this.progressListener = progressListener;
        this.type = type;
        this.stereo = stereo;
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager soundManager) {
        WeighedSoundEvents weighedSoundEvents = new WeighedSoundEvents(this.getLocation(), this.subtitle);
        weighedSoundEvents.addSound(new OnlineSound(this.getLocation(), this.url, this.attenuationDistance, this.progressListener, this.type, this.stereo));
        this.sound = weighedSoundEvents.getSound();
        return weighedSoundEvents;
    }

    public AbstractOnlineSoundInstance setLoop(boolean loop) {
        this.looping = loop;
        return this;
    }

    public static class OnlineSound extends Sound implements SoundStreamModifier {

        private final String url;
        private final DownloadProgressListener progressListener;
        private final AudioSource.AudioFileType type;
        private final boolean stereo;

        public OnlineSound(ResourceLocation location, String url, int attenuationDistance, DownloadProgressListener progressListener, AudioSource.AudioFileType type, boolean stereo) {
            super(location.toString(), 1.0F, 1.0F, 1, Type.FILE, true, false, attenuationDistance);
            this.url = url;
            this.progressListener = progressListener;
            this.type = type;
            this.stereo = stereo;
        }

        public String getURL() {
            return url;
        }

        public DownloadProgressListener getProgressListener() {
            return progressListener;
        }

        public AudioSource.AudioFileType getAudioFileType() {
            return type;
        }

        @Override
        public AudioStream modifyStream(AudioStream stream) {
            return this.stereo ? stream : new MonoWrapper(stream);
        }
    }
}