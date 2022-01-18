package gg.moonflower.etched.core.mixin.fabric.client;

import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    @Shadow
    protected abstract boolean isInLevel();

    @Inject(method = "addAndRegisterBlockEntity", at = @At("TAIL"))
    public void addAndRegisterBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {
        if (this.isInLevel()) {
            if (blockEntity instanceof AlbumJukeboxBlockEntity)
                ((AlbumJukeboxBlockEntity) blockEntity).onLoad();
        }
    }
}
