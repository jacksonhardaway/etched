package gg.moonflower.etched.core.mixin.fabric.client;

import gg.moonflower.etched.core.hook.SoundEngineHook;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.concurrent.CompletableFuture;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Unique
    private Sound sound;

    @Inject(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundBufferLibrary;getStream(Lnet/minecraft/resources/ResourceLocation;Z)Ljava/util/concurrent/CompletableFuture;"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void captureSound(SoundInstance soundInstance, CallbackInfo ci, WeighedSoundEvents weighedSoundEvents, ResourceLocation resourceLocation, Sound sound) {
        this.sound = sound;
    }

    @Redirect(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundBufferLibrary;getStream(Lnet/minecraft/resources/ResourceLocation;Z)Ljava/util/concurrent/CompletableFuture;"))
    public CompletableFuture<AudioStream> redirectSoundStream(SoundBufferLibrary soundBufferLibrary, ResourceLocation resourceLocation, boolean loop) {
        return SoundEngineHook.getSoundStream(soundBufferLibrary, this.sound, loop);
    }
}