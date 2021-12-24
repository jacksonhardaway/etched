package gg.moonflower.etched.core.mixin.fabric.client;

import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "handleBlockEntityData", at = @At("TAIL"))
    public void handleBlockEntityData(ClientboundBlockEntityDataPacket pkt, CallbackInfo ci) {
        BlockPos blockpos = pkt.getPos();
        BlockEntity blockEntity = Objects.requireNonNull(this.minecraft.level).getBlockEntity(blockpos);

        if (blockEntity instanceof AlbumJukeboxBlockEntity)
            blockEntity.load(pkt.getTag());
    }
}
