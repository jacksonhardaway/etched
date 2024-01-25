package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * @param record The record to play
 * @param pos    The position the music disk is playing at
 * @author Ocelot
 */
@ApiStatus.Internal
public record ClientboundPlayMusicPacket(ItemStack record, BlockPos pos) implements EtchedPacket {

    public ClientboundPlayMusicPacket(FriendlyByteBuf buf) {
        this(buf.readItem(), buf.readBlockPos());
    }

    @Override
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeItem(this.record);
        buf.writeBlockPos(this.pos);
    }

    @Override
    public void processPacket(NetworkEvent.Context ctx) {
        EtchedClientPlayPacketHandler.handlePlayMusicPacket(this, ctx);
    }

    /**
     * @return The tracks to play in sequence
     */
    public TrackData[] tracks() {
        return PlayableRecord.getStackMusic(this.record).orElseGet(() -> new TrackData[0]);
    }
}
