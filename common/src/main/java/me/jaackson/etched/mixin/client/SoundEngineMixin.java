package me.jaackson.etched.mixin.client;

import com.mojang.blaze3d.audio.OggAudioStream;
import me.jaackson.etched.client.sound.AbstractOnlineSoundInstance;
import me.jaackson.etched.client.sound.SoundCache;
import net.minecraft.Util;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {

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
        return SoundCache.getAudioStream(((AbstractOnlineSoundInstance.OnlineSound) this.sound).getURL(), ((AbstractOnlineSoundInstance.OnlineSound) this.sound).getProgressListener()).thenApplyAsync(inputStream -> {
            try {
                return loop ? new LoopingAudioStream(OggAudioStream::new, inputStream) : new OggAudioStream(inputStream);
            } catch (IOException var5) {
                throw new CompletionException(var5);
            }
        }, Util.backgroundExecutor());
    }
}
