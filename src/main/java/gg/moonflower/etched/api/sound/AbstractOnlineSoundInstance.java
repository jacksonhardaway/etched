package gg.moonflower.etched.api.sound;

import dev.ryanhcode.sable.companion.SableCompanion;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.sound.stream.MonoWrapper;
import gg.moonflower.etched.api.sound.stream.AsyncAudioStream;
import gg.moonflower.etched.api.sound.stream.RawAudioStream;
import gg.moonflower.etched.api.sound.stream.RestartingAudioStream;
import gg.moonflower.etched.api.util.AacInputStream;
import gg.moonflower.etched.api.util.AsyncInputStream;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.api.util.Mp3InputStream;
import gg.moonflower.etched.api.util.StreamingInputStream;
import gg.moonflower.etched.api.util.WaveDataReader;
import gg.moonflower.etched.client.sound.EmptyAudioStream;
import gg.moonflower.etched.client.sound.SoundCache;
import gg.moonflower.etched.core.Etched;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.ConstantFloat;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @author Ocelot
 */
public class AbstractOnlineSoundInstance extends AbstractSoundInstance {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String url;
    private final String subtitle;
    private final int attenuationDistance;
    private final DownloadProgressListener progressListener;
    private final AudioSource.AudioFileType type;
    private final boolean stereo;
    private float gain;
    private float rangeScale;

    public AbstractOnlineSoundInstance(String url, @Nullable String subtitle, int attenuationDistance, SoundSource source, DownloadProgressListener progressListener, AudioSource.AudioFileType type, boolean stereo) {
        super(Etched.etchedPath(DigestUtils.sha1Hex(url)), source, SoundInstance.createUnseededRandom());
        this.url = url;
        this.subtitle = subtitle;
        this.attenuationDistance = attenuationDistance;
        this.progressListener = progressListener;
        this.type = type;
        this.stereo = Etched.CLIENT_CONFIG.forceStereo.get() || stereo;
        this.gain = 1.0F;
        this.rangeScale = 1.0F;
    }

    private static AudioStream getStream(AudioStream stream, Sound sound) {
        return sound instanceof SoundStreamModifier ? ((SoundStreamModifier) sound).modifyStream(stream) : new MonoWrapper(stream);
    }

    /**
     * Detects the format of the audio data and opens a decoder for it.
     *
     * @param stream The raw audio data
     * @return A stream of decoded audio
     * @throws CompletionException If the data is not in a supported format
     */
    private static AudioStream openAudioStream(InputStream stream) {
        try {
            InputStream is = new BufferedInputStream(stream);
            assert is.markSupported();
            is.mark(8192);

            // Try loading as OGG
            try {
                return new JOrbisAudioStream(is);
            } catch (Exception e) {
                is.reset();

                // Try loading as WAV
                try {
                    AudioInputStream ais = WaveDataReader.getAudioInputStream(is);
                    return new RawAudioStream(ais.getFormat(), ais);
                } catch (Exception e1) {
                    is.reset();

                    // Try loading as AAC. This must happen before MP3 since the MP3 decoder can read past the mark while searching for a header
                    try {
                        AacInputStream aacInputStream = new AacInputStream(is);
                        return new RawAudioStream(aacInputStream.getFormat(), aacInputStream);
                    } catch (Exception e2) {
                        is.reset();

                        // Try loading as MP3
                        try {
                            Mp3InputStream mp3InputStream = new Mp3InputStream(is);
                            return new RawAudioStream(mp3InputStream.getFormat(), mp3InputStream);
                        } catch (Exception e3) {
                            UnsupportedAudioFileException cause = new UnsupportedAudioFileException("Could not load as OGG, WAV, AAC, OR MP3");

                            try {
                                is.close();
                            } catch (Exception e4) {
                                // Pass the exception along
                                cause.addSuppressed(e4);
                            }

                            cause.addSuppressed(e);
                            cause.addSuppressed(e1);
                            cause.addSuppressed(e2);
                            cause.addSuppressed(e3);
                            throw new CompletionException(cause);
                        }
                    }
                }
            }
        } catch (CompletionException e) {
            throw e;
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager soundManager) {
        WeighedSoundEvents weighedSoundEvents = new WeighedSoundEvents(this.getLocation(), this.subtitle);
        // The sound engine scales the range by the volume, so divide it back out to keep the range
        int attenuationDistance = Math.max(1, Math.round(this.getRange() / Math.max(this.getVolume(), 1.0F)));
        weighedSoundEvents.addSound(new OnlineSound(this.getLocation(), this.url, attenuationDistance, this.progressListener, this.type, this.stereo));
        this.sound = weighedSoundEvents.getSound(this.random);
        return weighedSoundEvents;
    }

    /**
     * Ticking sounds fade with distance by themselves, since the sound engine only sets the range when a sound starts.
     * That lets the range change while playing.
     */
    private boolean isFadedManually() {
        return this instanceof TickableSoundInstance && !this.relative && this.attenuation == Attenuation.LINEAR;
    }

    @Override
    public Attenuation getAttenuation() {
        return this.isFadedManually() ? Attenuation.NONE : this.attenuation;
    }

    @Override
    public float getVolume() {
        // Minecraft clamps the volume times the category volume to 1, which would hide the gain of loud records.
        // Capping the volume where that clamp starts applies the gain after it instead
        float volume = this.getCategoryVolume() > 0.0F ? Math.min(this.volume, 1.0F / this.getCategoryVolume()) : this.volume;
        return volume * this.gain * this.getDistanceFade();
    }

    private float getCategoryVolume() {
        return this.source == SoundSource.MASTER ? 1.0F : Minecraft.getInstance().options.getSoundSourceVolume(this.source);
    }

    /**
     * @return The same linear fade the sound engine uses, measured from the camera
     */
    private float getDistanceFade() {
        Minecraft client = Minecraft.getInstance();
        Camera camera = client.gameRenderer.getMainCamera();
        if (!this.isFadedManually() || client.level == null || !camera.isInitialized()) {
            return 1.0F;
        }

        // Sounds on Sable sub-levels are stored far away from where they are heard
        double distance = Math.sqrt(SableCompanion.INSTANCE.distanceSquaredWithSubLevels(client.level, camera.getPosition(), this.x, this.y, this.z));
        return (float) Math.max(0.0, 1.0 - distance / this.getRange());
    }

    @Override
    public boolean canStartSilent() {
        // Quiet or far away records still have to start so they can be heard later
        return this.isFadedManually() ? this.getCategoryVolume() > 0.0F : this.gain <= 0.0F;
    }

    /**
     * Sets how loud this sound plays without changing how far away it can be heard.
     *
     * @param gain The volume multiplier from 0 to 1
     * @since 5.5.0
     */
    public void setGain(float gain) {
        this.gain = Mth.clamp(gain, 0.0F, 1.0F);
    }

    /**
     * @return The volume multiplier from 0 to 1
     * @since 5.5.0
     */
    public float getGain() {
        return this.gain;
    }

    /**
     * Scales how far away this sound can be heard. Only ticking sounds can change their range while playing.
     *
     * @param rangeScale The multiplier for the default range
     * @since 5.5.0
     */
    public void setRangeScale(float rangeScale) {
        this.rangeScale = Math.max(rangeScale, 0.0F);
    }

    /**
     * @return How far away this sound can be heard in blocks
     * @since 5.5.0
     */
    public float getRange() {
        return Math.max(this.attenuationDistance * Math.max(this.volume, 1.0F) * this.rangeScale, 1.0F);
    }

    public AbstractOnlineSoundInstance setLoop(boolean loop) {
        this.looping = loop;
        return this;
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary loader, Sound sound, boolean repeatInstantly) {
        if (!(sound instanceof OnlineSound onlineSound)) {
            return super.getStream(loader, sound, repeatInstantly);
        }

        if (TrackData.isLocalSound(onlineSound.getURL())) {
            ResourceLocation name = ResourceLocation.tryParse(onlineSound.getURL());
            if (name != null) {
                WeighedSoundEvents weighedSoundEvents = Minecraft.getInstance().getSoundManager().getSoundEvent(name);
                if (weighedSoundEvents == null) {
                    return CompletableFuture.failedFuture(new FileNotFoundException("Unable to play unknown soundEvent: " + sound.getPath()));
                }

                return loader.getStream(weighedSoundEvents.getSound(this.random).getPath(), repeatInstantly).thenApply(MonoWrapper::new).handleAsync((stream, throwable) -> {
                    if (throwable != null) {
                        LOGGER.error("Failed to load audio from client: {}", onlineSound.getURL(), throwable);
                        onlineSound.getProgressListener().onFail();
                        return EmptyAudioStream.INSTANCE;
                    }
                    onlineSound.getProgressListener().onSuccess();
                    return stream;
                }, Util.nonCriticalIoPool());
            }
        }

        return SoundCache.getAudioStream(onlineSound.getURL(), onlineSound.getProgressListener(), onlineSound.getAudioFileType()).thenComposeAsync(source -> source.openStream().thenApplyAsync(stream -> { // The source is cached, so opening it must not run on the calling thread
            onlineSound.getProgressListener().progressStartLoading();
            // Live streams never end, so looping would replay them from the start on any EOF
            boolean live = stream instanceof AsyncInputStream;
            AudioStream audioStream = openAudioStream(stream);
            if (repeatInstantly && !live) {
                audioStream = new RestartingAudioStream(audioStream, () -> {
                    try {
                        return openAudioStream(source.openStream().join());
                    } catch (CompletionException e) {
                        throw new IOException("Failed to restart audio from url: " + onlineSound.getURL(), e.getCause());
                    }
                });
            }
            audioStream = getStream(audioStream, sound);

            // Data from the network must never be waited for on the sound thread
            if (live || stream instanceof StreamingInputStream) {
                audioStream = new AsyncAudioStream(audioStream, Util.nonCriticalIoPool());
            }
            return audioStream;
        }, Util.nonCriticalIoPool()), Util.nonCriticalIoPool()).handleAsync((stream, throwable) -> {
            if (throwable != null) {
                if (throwable instanceof CompletionException e) {
                    throwable = e.getCause();
                }

                LOGGER.error("Failed to load audio from url: {}", onlineSound.getURL(), throwable);
                onlineSound.getProgressListener().onFail();
                return EmptyAudioStream.INSTANCE;
            }
            onlineSound.getProgressListener().onSuccess();
            return stream;
        }, Util.nonCriticalIoPool());
    }

    public static class OnlineSound extends Sound implements SoundStreamModifier {

        private final String url;
        private final DownloadProgressListener progressListener;
        private final AudioSource.AudioFileType type;
        private final boolean stereo;

        public OnlineSound(ResourceLocation location, String url, int attenuationDistance, DownloadProgressListener progressListener, AudioSource.AudioFileType type, boolean stereo) {
            super(location, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Type.FILE, true, false, attenuationDistance);
            this.url = url;
            this.progressListener = progressListener;
            this.type = type;
            this.stereo = stereo;
        }

        public String getURL() {
            return this.url;
        }

        public DownloadProgressListener getProgressListener() {
            return this.progressListener;
        }

        public AudioSource.AudioFileType getAudioFileType() {
            return this.type;
        }

        @Override
        public AudioStream modifyStream(AudioStream stream) {
            return this.stereo ? stream : new MonoWrapper(stream);
        }
    }
}