package gg.moonflower.etched.core.mixin.client;

import gg.moonflower.etched.api.record.PlayableRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    private ClientLevel level;
    @Unique
    private BlockPos capturePos;

    @Inject(method = "playStreamingMusic", at = @At("HEAD"))
    public void capturePos(SoundEvent sound, BlockPos pos, CallbackInfo ci) {
        this.capturePos = pos;
    }

    @Redirect(method = "playStreamingMusic", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setNowPlaying(Lnet/minecraft/network/chat/Component;)V"))
    public void redirectNowPlaying(Gui gui, Component component) {
        if (this.level.getBlockState(this.capturePos.above()).isAir() && PlayableRecord.canShowMessage(this.capturePos.getX() + 0.5, this.capturePos.getY() + 0.5, this.capturePos.getZ() + 0.5))
            gui.setNowPlaying(component);
    }
}
