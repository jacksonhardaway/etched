package gg.moonflower.etched.core.mixin;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.mixin.client.LevelRendererAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;
import java.util.Random;

@Mixin(JukeboxBlock.class)
public abstract class JukeboxBlockMixin extends BaseEntityBlock {

    protected JukeboxBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "getAnalogOutputSignal", at = @At("TAIL"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    public void getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir, BlockEntity blockEntity) {
        if (blockEntity instanceof JukeboxBlockEntity) {
            ItemStack record = ((JukeboxBlockEntity) blockEntity).getRecord();
            if (!(record.getItem() instanceof RecordItem) && record.getItem() instanceof PlayableRecord)
                cir.setReturnValue(15);
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void animateTick(BlockState state, Level level, BlockPos pos, Random random) {
        if (state.getValue(JukeboxBlock.HAS_RECORD) && Etched.CLIENT_CONFIG.showNotes.get() && level.getBlockState(pos.above()).isAir()) {
            Minecraft minecraft = Minecraft.getInstance();
            Map<BlockPos, SoundInstance> sounds = ((LevelRendererAccessor) minecraft.levelRenderer).getPlayingRecords();
            if (sounds.containsKey(pos) && minecraft.getSoundManager().isActive(sounds.get(pos)))
                level.addParticle(ParticleTypes.NOTE, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, random.nextInt(25) / 24D, 0, 0);
        }
    }
}
