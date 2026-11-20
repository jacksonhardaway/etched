package gg.moonflower.etched.core.mixin.client.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.api.sound.StopListeningSound;
import gg.moonflower.etched.client.GuiHook;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Unique
    private BlockPos etched$pos;

    @Shadow
    private ClientLevel level;

    @Shadow
    protected abstract void notifyNearbyEntities(Level level, BlockPos blockPos, boolean bl);

    @Inject(method = "stopJukeboxSong", at = @At("HEAD"), cancellable = true)
    private void retireRadioWithoutBlocking(BlockPos pos, CallbackInfo ci) {
        if (SoundTracker.retireRadioStream(this.level, pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "playJukeboxSong", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setNowPlaying(Lnet/minecraft/network/chat/Component;)V", shift = At.Shift.BEFORE))
    public void preNowPlaying(Holder<JukeboxSong> song, BlockPos pos, CallbackInfo ci) {
        if (!this.level.getBlockState(this.etched$pos.above()).isAir() || !PlayableRecord.canShowMessage(this.etched$pos.getX() + 0.5, this.etched$pos.getY() + 0.5, this.etched$pos.getZ() + 0.5)) {
            GuiHook.setHidePlayingText(true);
        }
    }

    @Inject(method = "playJukeboxSong", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setNowPlaying(Lnet/minecraft/network/chat/Component;)V", shift = At.Shift.AFTER))
    public void postNowPlaying(Holder<JukeboxSong> song, BlockPos pos, CallbackInfo ci) {
        GuiHook.setHidePlayingText(false);
    }

    @Inject(method = "playJukeboxSong", at = @At("HEAD"), remap = false)
    public void playRecord(Holder<JukeboxSong> song, BlockPos pos, CallbackInfo ci) {
        this.etched$pos = pos;
    }

    @Inject(method = "playJukeboxSong", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.BEFORE), remap = false)
    public void modifySoundInstance(Holder<JukeboxSong> song, BlockPos pos, CallbackInfo ci, @Local LocalRef<SoundInstance> sound) {
        sound.set(StopListeningSound.create(sound.get(), () -> this.notifyNearbyEntities(this.level, this.etched$pos, false)));
    }
}
