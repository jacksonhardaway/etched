package gg.moonflower.etched.core.mixin.forge.client;

import gg.moonflower.etched.core.hook.SoundEngineHook;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Redirect(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getStream(Lnet/minecraft/client/sounds/SoundBufferLibrary;Lnet/minecraft/client/resources/sounds/Sound;Z)Ljava/util/concurrent/CompletableFuture;"))
    public CompletableFuture<AudioStream> redirectSoundStream(SoundInstance instance, SoundBufferLibrary soundBufferLibrary, Sound sound, boolean loop) {
        return SoundEngineHook.getSoundStream(soundBufferLibrary, sound, loop);
    }
}
