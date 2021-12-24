package gg.moonflower.etched.core.mixin.fabric.client;

import gg.moonflower.etched.api.sound.StopListeningSound;
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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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

    @ModifyVariable(method = "playStreamingMusic", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.BEFORE), index = 3)
    public SoundInstance modifySoundInstance(SoundInstance soundInstance) {
        return new StopListeningSound(soundInstance, () -> this.notifyNearbyEntities(this.level, this.pos, false));
    }
}
