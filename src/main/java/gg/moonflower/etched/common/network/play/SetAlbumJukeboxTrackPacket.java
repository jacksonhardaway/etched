package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandler;
import gg.moonflower.etched.common.network.play.handler.EtchedServerPlayPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * @param playingIndex The playing index to set the jukebox to
 * @param track        The track to set the jukebox to
 * @author Ocelot
 */
@ApiStatus.Internal
public record SetAlbumJukeboxTrackPacket(int playingIndex, int track) implements EtchedPacket {

    public SetAlbumJukeboxTrackPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeVarInt(this.playingIndex);
        buf.writeVarInt(this.track);
    }

    @Override
    public void processPacket(NetworkEvent.Context ctx) {
        switch (ctx.getDirection().getReceptionSide()) {
            case CLIENT -> EtchedClientPlayPacketHandler.handleSetAlbumJukeboxTrack(this, ctx);
            case SERVER -> EtchedServerPlayPacketHandler.handleSetAlbumJukeboxTrack(this, ctx);
        }
    }
}
