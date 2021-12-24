package gg.moonflower.etched.core.mixin.client;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.sounds.AudioStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(Channel.class)
public class ChannelMixin {

    @Unique
    private final AtomicBoolean loaded = new AtomicBoolean(false);
    @Unique
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    @Inject(method = "stop", at = @At("HEAD"))
    public void stop(CallbackInfo ci) {
        if (!this.loaded.get())
            this.stopped.set(true);
    }

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    public void play(CallbackInfo ci) {
        if (this.stopped.get()) {
            this.stopped.set(false);
            ci.cancel();
        }
    }

    @Inject(method = "stopped", at = @At("TAIL"), cancellable = true)
    public void stopped(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && this.stopped.get())
            cir.setReturnValue(true);
    }

    @Inject(method = "attachStaticBuffer", at = @At("HEAD"))
    public void attachStaticBuffer(SoundBuffer soundBuffer, CallbackInfo ci) {
        this.loaded.set(true);
    }

    @Inject(method = "attachBufferStream", at = @At("HEAD"))
    public void attachBufferStream(AudioStream audioStream, CallbackInfo ci) {
        this.loaded.set(true);
    }
}
