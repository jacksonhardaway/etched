package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.core.Etched;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.ApiStatus;

/**
 * @param entityId The id of the contraption entity carrying the block
 * @param localPos The position of the block inside the contraption
 * @param data     The block entity data that changed, merged into what the client has
 */
@ApiStatus.Internal
public record ClientboundContraptionBlockDataPacket(int entityId, BlockPos localPos, CompoundTag data) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientboundContraptionBlockDataPacket> TYPE = new CustomPacketPayload.Type<>(Etched.etchedPath("contraption_block_data"));
    public static final StreamCodec<ByteBuf, ClientboundContraptionBlockDataPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ClientboundContraptionBlockDataPacket::entityId,
            BlockPos.STREAM_CODEC,
            ClientboundContraptionBlockDataPacket::localPos,
            ByteBufCodecs.COMPOUND_TAG,
            ClientboundContraptionBlockDataPacket::data,
            ClientboundContraptionBlockDataPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
