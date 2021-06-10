package me.jaackson.etched.mixin.client;

import com.mojang.blaze3d.audio.OggAudioStream;
import com.sun.media.sound.WaveFileReader;
import me.jaackson.etched.client.sound.*;
import net.minecraft.Util;
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
import java.io.FileInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    @Shadow
    @Final
    private static Logger LOGGER;

    @Unique
    private Sound sound;

    @Inject(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundBufferLibrary;getStream(Lnet/minecraft/resources/ResourceLocation;Z)Ljava/util/concurrent/CompletableFuture;", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD)
    public void captureSound(SoundInstance soundInstance, CallbackInfo ci, WeighedSoundEvents weighedSoundEvents, ResourceLocation resourceLocation, Sound sound) {
        this.sound = sound;
    }

    @Redirect(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundBufferLibrary;getStream(Lnet/minecraft/resources/ResourceLocation;Z)Ljava/util/concurrent/CompletableFuture;"))
    public CompletableFuture<AudioStream> redirectSoundStream(SoundBufferLibrary soundBufferLibrary, ResourceLocation resourceLocation, boolean loop) {
        if (!(this.sound instanceof AbstractOnlineSoundInstance.OnlineSound))
            return soundBufferLibrary.getStream(resourceLocation, loop);

        AbstractOnlineSoundInstance.OnlineSound onlineSound = (AbstractOnlineSoundInstance.OnlineSound) this.sound;
        return SoundCache.getAudioStream(onlineSound.getURL(), onlineSound.getProgressListener()).<AudioStream>thenApplyAsync(path -> {
            FileInputStream is = null;

            // Try loading as OGG
            try {
                is = new FileInputStream(path.toFile());
                return new MonoWrapper(loop ? new LoopingAudioStream(OggAudioStream::new, is) : new OggAudioStream(is));
            } catch (Exception e) {
                IOUtils.closeQuietly(is);
                LOGGER.debug("Failed to load as OGG", e);
                // Try loading as MP3

                try {
                    is = new FileInputStream(path.toFile());
                    fr.delthas.javamp3.Sound sound = new fr.delthas.javamp3.Sound(is);
                    AudioFormat format = sound.getAudioFormat();
                    return new MonoWrapper(loop ? new LoopingAudioStream(input -> new RawAudioStream(format, input), sound) : new RawAudioStream(format, sound));
                } catch (Exception e1) {
                    IOUtils.closeQuietly(is);
                    LOGGER.debug("Failed to load as MP3", e1);

                    // Try loading as WAV
                    try {
                        is = new FileInputStream(path.toFile());
                        AudioInputStream ais = new WaveFileReader().getAudioInputStream(is);
                        AudioFormat format = ais.getFormat();
                        return new MonoWrapper(loop ? new LoopingAudioStream(input -> new RawAudioStream(format, input), ais) : new RawAudioStream(format, ais));
                    } catch (Exception e2) {
                        IOUtils.closeQuietly(is);
                        throw new CompletionException(new UnsupportedAudioFileException("Failed to load audio"));
                    }
                }
            }
        }, Util.backgroundExecutor()).exceptionally((e) -> {
            e.printStackTrace();
            return EmptyAudioStream.INSTANCE;
        });
    }
}
