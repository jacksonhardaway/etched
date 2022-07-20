package gg.moonflower.etched.core.mixin.client;

import gg.moonflower.etched.api.sound.SoundStopListener;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.Map;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Inject(method = "tickNonPaused", at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    public void onSoundRemoved(CallbackInfo ci, Iterator<?> iterator, Map.Entry<?, ?> entry, ChannelAccess.ChannelHandle channelHandle2, SoundInstance soundInstance) {
        if (soundInstance instanceof SoundStopListener)
            ((SoundStopListener) soundInstance).onStop();
    }
}
