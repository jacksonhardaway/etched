package gg.moonflower.etched.core.mixin;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.ClientboundPlayMusicPacket;
import gg.moonflower.etched.core.registry.EtchedItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.ticks.ContainerSingleItem;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin extends BlockEntity implements ContainerSingleItem {

    @Shadow
    @Final
    private NonNullList<ItemStack> items;

    @Shadow
    protected abstract void setHasRecordBlockState(@Nullable Entity entity, boolean hasRecord);

    @Shadow
    public abstract void startPlaying();

    @Shadow
    public abstract boolean isRecordPlaying();

    @Shadow
    protected abstract boolean shouldSendJukeboxPlayingEvent();

    @Shadow
    private int ticksSinceLastEvent;

    @Shadow
    protected abstract void spawnMusicParticles(Level level, BlockPos pos);

    public JukeboxBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Inject(method = "startPlaying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(Lnet/minecraft/world/entity/player/Player;ILnet/minecraft/core/BlockPos;I)V", shift = At.Shift.AFTER))
    public void startPlaying(CallbackInfo ci) {
        if (!(this.getFirstItem().getItem() instanceof RecordItem)) {
            BlockPos pos = this.getBlockPos();
            EtchedMessages.PLAY.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 64, this.level.dimension())), new ClientboundPlayMusicPacket(this.getFirstItem().copy(), pos));
        }
    }

    @Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
    public void setItem(int slot, ItemStack stack, CallbackInfo ci) {
        if (stack.is(EtchedItems.ALBUM_COVER.get()) && this.level != null) {
            this.items.set(slot, stack);
            this.setHasRecordBlockState(null, true);
            this.startPlaying();
            ci.cancel();
        }
    }

    @Inject(method = "canPlaceItem", at = @At("RETURN"), cancellable = true)
    public void canPlaceItem(int index, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            cir.setReturnValue(stack.is(EtchedItems.ALBUM_COVER.get()) && this.getItem(index).isEmpty());
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (this.isRecordPlaying()) {
            Item item = this.getFirstItem().getItem();
            if (!(item instanceof RecordItem) && item instanceof PlayableRecord) {
                ++this.ticksSinceLastEvent;

                // Allow music particles and events to play for custom records
                if (this.shouldSendJukeboxPlayingEvent()) {
                    this.ticksSinceLastEvent = 0;
                    level.gameEvent(GameEvent.JUKEBOX_PLAY, pos, GameEvent.Context.of(state));
                    this.spawnMusicParticles(level, pos);
                } else {
                    this.ticksSinceLastEvent--;
                }
            }
        }
    }
}
