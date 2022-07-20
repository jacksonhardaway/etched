package gg.moonflower.etched.core.hook;

import com.mojang.blaze3d.audio.OggAudioStream;
import com.mojang.logging.LogUtils;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.AbstractOnlineSoundInstance;
import gg.moonflower.etched.api.sound.SoundStreamModifier;
import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.sound.stream.MonoWrapper;
import gg.moonflower.etched.api.sound.stream.RawAudioStream;
import gg.moonflower.etched.api.util.HeaderInputStream;
import gg.moonflower.etched.api.util.Mp3InputStream;
import gg.moonflower.etched.api.util.SeekingStream;
import gg.moonflower.etched.api.util.WaveDataReader;
import gg.moonflower.etched.client.sound.EmptyAudioStream;
import gg.moonflower.etched.client.sound.SoundCache;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class SoundEngineHook {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static CompletableFuture<AudioStream> getSoundStream(SoundBufferLibrary soundBufferLibrary, Sound sound, boolean loop) {
        if (!(sound instanceof AbstractOnlineSoundInstance.OnlineSound onlineSound))
            return soundBufferLibrary.getStream(sound.getPath(), loop);

        if (TrackData.isLocalSound(onlineSound.getURL())) {
            WeighedSoundEvents weighedSoundEvents = Minecraft.getInstance().getSoundManager().getSoundEvent(new ResourceLocation(onlineSound.getURL()));
            if (weighedSoundEvents == null) {
                CompletableFuture<AudioStream> future = new CompletableFuture<>();
                future.completeExceptionally(new FileNotFoundException("Unable to play unknown soundEvent: " + sound.getPath()));
                return future;
            }

            return soundBufferLibrary.getStream(weighedSoundEvents.getSound().getPath(), loop).thenApply(MonoWrapper::new).handleAsync((stream, e) -> {
                if (e != null) {
                    e.printStackTrace();
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
                byte[] readHeader = new byte[8192]; // 8KB starting buffer
                int read = IOUtils.read(stream, readHeader);

                InputStream is;
                if (read < readHeader.length) {
                    byte[] header = new byte[read];
                    System.arraycopy(readHeader, 0, header, 0, header.length);
                    is = new HeaderInputStream(header, stream);
                } else {
                    is = new HeaderInputStream(readHeader, stream);
                }

                // Try loading as OGG
                try {
                    return SoundEngineHook.getStream(loop ? new LoopingAudioStream(OggAudioStream::new, is) : new OggAudioStream(is), sound);
                } catch (Exception e) {
                    LOGGER.debug("Failed to load as OGG", e);
                    ((SeekingStream) is).beginning();

                    // Try loading as WAV
                    try {
                        AudioInputStream ais = WaveDataReader.getAudioInputStream(is);
                        AudioFormat format = ais.getFormat();
                        return SoundEngineHook.getStream(loop ? new LoopingAudioStream(input -> new RawAudioStream(format, input), ais) : new RawAudioStream(format, ais), sound);
                    } catch (Exception e1) {
                        LOGGER.debug("Failed to load as WAV", e1);
                        ((SeekingStream) is).beginning();

                        // Try loading as MP3
                        try {
                            Mp3InputStream mp3InputStream = new Mp3InputStream(is);
                            return SoundEngineHook.getStream(loop ? new LoopingAudioStream(input -> new RawAudioStream(mp3InputStream.getFormat(), input), mp3InputStream) : new RawAudioStream(mp3InputStream.getFormat(), mp3InputStream), sound);
                        } catch (Exception e2) {
                            LOGGER.debug("Failed to load as MP3", e2);
                            IOUtils.closeQuietly(is);
                            e.printStackTrace();
                            e1.printStackTrace();
                            e2.printStackTrace();
                            throw new CompletionException(new UnsupportedAudioFileException("Could not load as OGG, WAV, OR MP3"));
                        }
                    }
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, Util.backgroundExecutor()).handleAsync((stream, e) -> {
            if (e != null) {
                e.printStackTrace();
                onlineSound.getProgressListener().onFail();
                return EmptyAudioStream.INSTANCE;
            }
            onlineSound.getProgressListener().onSuccess();
            return stream;
        }, Util.backgroundExecutor());
    }

    private static AudioStream getStream(AudioStream stream, Sound sound) {
        return sound instanceof SoundStreamModifier ? ((SoundStreamModifier) sound).modifyStream(stream) : new MonoWrapper(stream);
    }
}
