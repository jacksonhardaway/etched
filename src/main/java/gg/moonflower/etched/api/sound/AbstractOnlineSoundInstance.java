package gg.moonflower.etched.api.sound;

import com.mojang.blaze3d.audio.OggAudioStream;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.sound.stream.MonoWrapper;
import gg.moonflower.etched.api.sound.stream.RawAudioStream;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.api.util.Mp3InputStream;
import gg.moonflower.etched.api.util.SeekingStream;
import gg.moonflower.etched.api.util.WaveDataReader;
import gg.moonflower.etched.client.sound.EmptyAudioStream;
import gg.moonflower.etched.client.sound.SoundCache;
import gg.moonflower.etched.core.Etched;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
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

    public AbstractOnlineSoundInstance(String url, @Nullable String subtitle, int attenuationDistance, SoundSource source, DownloadProgressListener progressListener, AudioSource.AudioFileType type, boolean stereo) {
        super(new ResourceLocation(Etched.MOD_ID, DigestUtils.sha1Hex(url)), source, SoundInstance.createUnseededRandom());
        this.url = url;
        this.subtitle = subtitle;
        this.attenuationDistance = attenuationDistance;
        this.progressListener = progressListener;
        this.type = type;
        this.stereo = Etched.CLIENT_CONFIG.forceStereo.get() || stereo;
    }

    private static AudioStream getStream(AudioStream stream, Sound sound) {
        return sound instanceof SoundStreamModifier ? ((SoundStreamModifier) sound).modifyStream(stream) : new MonoWrapper(stream);
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager soundManager) {
        WeighedSoundEvents weighedSoundEvents = new WeighedSoundEvents(this.getLocation(), this.subtitle);
        weighedSoundEvents.addSound(new OnlineSound(this.getLocation(), this.url, this.attenuationDistance, this.progressListener, this.type, this.stereo));
        this.sound = weighedSoundEvents.getSound(this.random);
        return weighedSoundEvents;
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
            WeighedSoundEvents weighedSoundEvents = Minecraft.getInstance().getSoundManager().getSoundEvent(new ResourceLocation(onlineSound.getURL()));
            if (weighedSoundEvents == null) {
                CompletableFuture<AudioStream> future = new CompletableFuture<>();
                future.completeExceptionally(new FileNotFoundException("Unable to play unknown soundEvent: " + sound.getPath()));
                return future;
            }

            return loader.getStream(weighedSoundEvents.getSound(this.random).getPath(), repeatInstantly).thenApply(MonoWrapper::new).handleAsync((stream, throwable) -> {
                if (throwable != null) {
                    LOGGER.error("Failed to load audio from client: {}", onlineSound.getURL(), throwable);
                    onlineSound.getProgressListener().onFail();
                    return EmptyAudioStream.INSTANCE;
                }
                onlineSound.getProgressListener().onSuccess();
                return stream;
            }, Util.backgroundExecutor());
        }

        return SoundCache.getAudioStream(onlineSound.getURL(), onlineSound.getProgressListener(), onlineSound.getAudioFileType()).thenCompose(AudioSource::openStream).thenApplyAsync(stream -> {
            onlineSound.getProgressListener().progressStartLoading();
            try {
                InputStream is = new BufferedInputStream(stream, 8192);

                // Try loading as OGG
                try {
                    is.mark(4192);
                    return getStream(repeatInstantly ? new LoopingAudioStream(OggAudioStream::new, is) : new OggAudioStream(is), sound);
                } catch (Exception e) {
                    LOGGER.debug("Failed to load as OGG", e);
                    is.reset();

                    // Try loading as WAV
                    try {
                        is.mark(4192);
                        AudioInputStream ais = WaveDataReader.getAudioInputStream(is);
                        AudioFormat format = ais.getFormat();
                        return getStream(repeatInstantly ? new LoopingAudioStream(input -> new RawAudioStream(format, input), ais) : new RawAudioStream(format, ais), sound);
                    } catch (Exception e1) {
                        LOGGER.debug("Failed to load as WAV", e1);
                        is.reset();

                        // Try loading as MP3
                        try {
                            Mp3InputStream mp3InputStream = new Mp3InputStream(is);
                            return getStream(repeatInstantly ? new LoopingAudioStream(input -> new RawAudioStream(mp3InputStream.getFormat(), input), mp3InputStream) : new RawAudioStream(mp3InputStream.getFormat(), mp3InputStream), sound);
                        } catch (Exception e2) {
                            LOGGER.debug("Failed to load as MP3", e2);
                            UnsupportedAudioFileException cause = new UnsupportedAudioFileException("Could not load as OGG, WAV, OR MP3");

                            try {
                                is.close();
                            } catch (Exception e3) {
                                // Pass the exception along
                                cause.addSuppressed(e3);
                            }

                            cause.addSuppressed(e);
                            cause.addSuppressed(e1);
                            cause.addSuppressed(e2);
                            throw new CompletionException(cause);
                        }
                    }
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, Util.backgroundExecutor()).handleAsync((stream, throwable) -> {
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
        }, Util.backgroundExecutor());
    }

    public static class OnlineSound extends Sound implements SoundStreamModifier {

        private final String url;
        private final DownloadProgressListener progressListener;
        private final AudioSource.AudioFileType type;
        private final boolean stereo;

        public OnlineSound(ResourceLocation location, String url, int attenuationDistance, DownloadProgressListener progressListener, AudioSource.AudioFileType type, boolean stereo) {
            super(location.toString(), ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Type.FILE, true, false, attenuationDistance);
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