package gg.moonflower.etched.core.mixin;

import gg.moonflower.etched.core.registry.EtchedTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Parrot.class)
public abstract class ParrotMixin extends Entity {

    @Shadow private BlockPos jukebox;

    public ParrotMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z")) // TODO: use mixin extras
    public boolean addAudioProviders(BlockState instance, Block block) {
        boolean isAudioProvider = this.level.getBlockState(this.jukebox).is(EtchedTags.AUDIO_PROVIDER);
        boolean isJukebox = instance.is(block);
        return isAudioProvider || isJukebox;
    }
}
