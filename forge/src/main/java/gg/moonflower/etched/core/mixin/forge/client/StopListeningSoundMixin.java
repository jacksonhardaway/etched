package gg.moonflower.etched.core.mixin.forge.client;

import gg.moonflower.etched.api.sound.StopListeningSound;
import gg.moonflower.etched.api.sound.WrappedSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import org.spongepowered.asm.mixin.Mixin;

import java.util.concurrent.CompletableFuture;

@Mixin(StopListeningSound.class)
public abstract class StopListeningSoundMixin implements SoundInstance, WrappedSoundInstance {

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
        return this.getParent().getStream(soundBuffers, sound, looping);
    }
}
