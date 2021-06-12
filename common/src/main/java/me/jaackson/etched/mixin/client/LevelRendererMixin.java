package me.jaackson.etched.mixin.client;

import me.jaackson.etched.client.sound.StopListeningSound;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Unique
    private BlockPos pos;

    @Shadow
    private ClientLevel level;

    @Shadow
    protected abstract void notifyNearbyEntities(Level level, BlockPos blockPos, boolean bl);

    @Inject(method = "playStreamingMusic", at = @At("HEAD"))
    public void playStreamingMusic(SoundEvent soundEvent, BlockPos pos, CallbackInfo ci) {
        this.pos = pos;
    }

    @ModifyArg(method = "playStreamingMusic", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V"), index = 0)
    public SoundInstance modifySoundInstance(SoundInstance soundInstance) {
        return new StopListeningSound(soundInstance, () -> this.notifyNearbyEntities(this.level, this.pos, false));
    }
}
