package me.jaackson.etched.mixin.client;

import me.jaackson.etched.common.network.handler.EtchedClientPlayHandler;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Inject(method = "playStreamingMusic", at = @At("HEAD"))
    public void onMusicStop(@Nullable SoundEvent soundEvent, BlockPos pos, CallbackInfo ci) {
        if (soundEvent == null)
            EtchedClientPlayHandler.onStopRecord(pos);
    }
}
