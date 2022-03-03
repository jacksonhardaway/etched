package gg.moonflower.etched.core.mixin.client;

import com.mojang.blaze3d.audio.OggAudioStream;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.AbstractOnlineSoundInstance;
import gg.moonflower.etched.api.sound.SoundStopListener;
import gg.moonflower.etched.api.sound.SoundStreamModifier;
import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.sound.stream.MonoWrapper;
import gg.moonflower.etched.api.sound.stream.RawAudioStream;
import gg.moonflower.etched.api.util.HeaderInputStream;
import gg.moonflower.etched.api.util.SeekingStream;
import gg.moonflower.etched.api.util.WaveDataReader;
import gg.moonflower.etched.client.sound.EmptyAudioStream;
import gg.moonflower.etched.client.sound.SoundCache;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Shadow
    @Final
    private static Logger LOGGER;

    @Unique
    private Sound sound;

    @Inject(method = "tickNonPaused", at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    public void onSoundRemoved(CallbackInfo ci, Iterator<?> iterator, Map.Entry<?, ?> entry, ChannelAccess.ChannelHandle channelHandle2, SoundInstance soundInstance) {
        if (soundInstance instanceof SoundStopListener)
            ((SoundStopListener) soundInstance).onStop();
    }

    @Inject(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundBufferLibrary;getStream(Lnet/minecraft/resources/ResourceLocation;Z)Ljava/util/concurrent/CompletableFuture;", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD)
    public void captureSound(SoundInstance soundInstance, CallbackInfo ci, WeighedSoundEvents weighedSoundEvents, ResourceLocation resourceLocation, Sound sound) {
        this.sound = sound;
    }

    @Redirect(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundBufferLibrary;getStream(Lnet/minecraft/resources/ResourceLocation;Z)Ljava/util/concurrent/CompletableFuture;"))
    public CompletableFuture<AudioStream> redirectSoundStream(SoundBufferLibrary soundBufferLibrary, ResourceLocation resourceLocation, boolean loop) {
        if (!(this.sound instanceof AbstractOnlineSoundInstance.OnlineSound))
            return soundBufferLibrary.getStream(resourceLocation, loop);

        AbstractOnlineSoundInstance.OnlineSound onlineSound = (AbstractOnlineSoundInstance.OnlineSound) this.sound;
        if (TrackData.isLocalSound(onlineSound.getURL())) {
            WeighedSoundEvents weighedSoundEvents = Minecraft.getInstance().getSoundManager().getSoundEvent(new ResourceLocation(onlineSound.getURL()));
            if (weighedSoundEvents == null)
                throw new CompletionException(new FileNotFoundException("Unable to play unknown soundEvent: " + resourceLocation));

            return soundBufferLibrary.getStream(weighedSoundEvents.getSound().getPath(), loop).thenApplyAsync(MonoWrapper::new, Util.backgroundExecutor()).handleAsync((stream, e) -> {
                if (e != null) {
                    e.printStackTrace();
                    onlineSound.getProgressListener().onFail();
                    return EmptyAudioStream.INSTANCE;
                }
                onlineSound.getProgressListener().onSuccess();
                return stream;
            }, Util.backgroundExecutor());
        }

        return SoundCache.getAudioStream(onlineSound.getURL(), onlineSound.getProgressListener(), onlineSound.getAudioFileType()).thenComposeAsync(AudioSource::openStream, Util.backgroundExecutor()).thenApplyAsync(stream -> {
            onlineSound.getProgressListener().progressStartLoading();
            try {
                byte[] readHeader = new byte[16384]; // 16KB starting buffer
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
                    return this.getStream(loop ? new LoopingAudioStream(OggAudioStream::new, is) : new OggAudioStream(is));
                } catch (Exception e) {
                    LOGGER.debug("Failed to load as OGG", e);
                    ((SeekingStream) is).beginning();

                    // Try loading as WAV
                    try {
                        AudioInputStream ais = WaveDataReader.getAudioInputStream(is);
                        AudioFormat format = ais.getFormat();
                        return this.getStream(loop ? new LoopingAudioStream(input -> new RawAudioStream(format, input), ais) : new RawAudioStream(format, ais));
                    } catch (Exception e1) {
                        LOGGER.debug("Failed to load as WAV", e1);
                        ((SeekingStream) is).beginning();

                        // Try loading as MP3
                        try {
                            fr.delthas.javamp3.Sound sound = new fr.delthas.javamp3.Sound(new BufferedInputStream(is));
                            AudioFormat format = sound.getAudioFormat();
                            return this.getStream(loop ? new LoopingAudioStream(input -> new RawAudioStream(format, input), sound) : new RawAudioStream(format, sound));
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

    @Unique
    private AudioStream getStream(AudioStream stream) {
        return this.sound instanceof SoundStreamModifier ? ((SoundStreamModifier) this.sound).modifyStream(stream) : new MonoWrapper(stream);
    }
}
