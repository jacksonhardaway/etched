package gg.moonflower.etched.core.mixin;

import gg.moonflower.etched.core.registry.EtchedItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(JukeboxBlock.class)
public class JukeboxBlockMixin {

    @Inject(method = "getAnalogOutputSignal", at = @At("TAIL"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    public void getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir, BlockEntity blockEntity) {
        if (blockEntity instanceof JukeboxBlockEntity && ((JukeboxBlockEntity) blockEntity).getRecord().getItem() == EtchedItems.ETCHED_MUSIC_DISC.get())
            cir.setReturnValue(15);
    }
}
