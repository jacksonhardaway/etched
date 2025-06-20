package gg.moonflower.etched.core.mixin.jukebox;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.common.network.play.ClientboundPlayBlockMusicPacket;
import gg.moonflower.etched.core.registry.EtchedComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin extends BlockEntity {

    @Shadow
    private ItemStack item;

    @Shadow
    @Final
    private JukeboxSongPlayer jukeboxSongPlayer;

    @Shadow
    public abstract void onSongChanged();

    @Unique
    private boolean etched$playing;

    public JukeboxBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Inject(method = "setTheItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/JukeboxSongPlayer;stop(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    public void stop(ItemStack stack, CallbackInfo ci) {
        if (this.level instanceof ServerLevel serverLevel) {
            BlockPos pos = this.getBlockPos();
            if (PlayableRecord.isPlayableRecord(stack)) {
                PacketDistributor.sendToPlayersNear(serverLevel, null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 64, new ClientboundPlayBlockMusicPacket(stack.copy(), pos));
                this.etched$playing = true;
            } else if (this.etched$playing) {
                this.etched$playing = false;
                ((JukeboxSongPlayerAccessor) this.jukeboxSongPlayer).setTicksSinceSongStarted(0L);
                this.level.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, pos, GameEvent.Context.of(this.getBlockState()));
                this.level.levelEvent(1011, pos, 0);
                this.onSongChanged();
            }
        }
    }

    @Inject(method = "getComparatorOutput", at = @At("TAIL"), cancellable = true)
    public void getComparatorOutput(CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() == 0 && PlayableRecord.isPlayableRecord(this.item)) {
            cir.setReturnValue(15);
        }
    }

    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    public void canPlaceItem(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        JukeboxBlockEntity self = (JukeboxBlockEntity)(Object)this;
        if (stack.has(EtchedComponents.DISC_APPEARANCE)
                && self.getItem(slot).isEmpty()) cir.setReturnValue(true);
    }
}
