package gg.moonflower.etched.core.mixin.fabric.client;

import gg.moonflower.etched.api.sound.AbstractOnlineSoundInstance;
import gg.moonflower.etched.api.sound.StopListeningSound;
import gg.moonflower.etched.api.sound.TickableStopListeningSound;
import gg.moonflower.etched.api.sound.WrappedSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Unique
    private SoundInstance soundInstance;

    @Inject(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundBufferLibrary;getStream(Lnet/minecraft/resources/ResourceLocation;Z)Ljava/util/concurrent/CompletableFuture;"))
    public void captureSound(SoundInstance soundInstance, CallbackInfo ci) {
        this.soundInstance = soundInstance instanceof WrappedSoundInstance ? ((WrappedSoundInstance) soundInstance).getParent() : soundInstance;
    }

    @Redirect(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundBufferLibrary;getStream(Lnet/minecraft/resources/ResourceLocation;Z)Ljava/util/concurrent/CompletableFuture;"))
    public CompletableFuture<AudioStream> redirectSoundStream(SoundBufferLibrary soundBufferLibrary, ResourceLocation resourceLocation, boolean loop) {
        if (this.soundInstance instanceof AbstractOnlineSoundInstance)
            return ((AbstractOnlineSoundInstance) this.soundInstance).getStream(soundBufferLibrary, this.soundInstance.getSound(), loop);
        return soundBufferLibrary.getStream(resourceLocation, loop);
    }
}