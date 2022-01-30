package gg.moonflower.etched.core.mixin.fabric.client;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.sound.StopListeningSound;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Unique
    private BlockPos pos;

    @Shadow
    private ClientLevel level;

    @Shadow
    protected abstract void notifyNearbyEntities(Level level, BlockPos blockPos, boolean bl);

    @Redirect(method = "playStreamingMusic", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setNowPlaying(Lnet/minecraft/network/chat/Component;)V"))
    public void redirectNowPlaying(Gui gui, Component component) {
        if (this.level.getBlockState(this.pos.above()).isAir() && PlayableRecord.canShowMessage(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5))
            gui.setNowPlaying(component);
    }

    @Inject(method = "playStreamingMusic", at = @At("HEAD"))
    public void playStreamingMusic(SoundEvent soundEvent, BlockPos pos, CallbackInfo ci) {
        this.pos = pos;
    }

    @ModifyVariable(method = "playStreamingMusic", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.BEFORE), index = 3)
    public SoundInstance modifySoundInstance(SoundInstance soundInstance) {
        return StopListeningSound.create(soundInstance, () -> this.notifyNearbyEntities(this.level, this.pos, false));
    }
}
