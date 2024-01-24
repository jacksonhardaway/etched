package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandler;
import gg.moonflower.etched.common.network.play.handler.EtchedServerPlayPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public class SetAlbumJukeboxTrackPacket implements EtchedPacket {

    private final int playingIndex;
    private final int track;

    public SetAlbumJukeboxTrackPacket(int playingIndex, int track) {
        this.playingIndex = playingIndex;
        this.track = track;
    }

    public SetAlbumJukeboxTrackPacket(FriendlyByteBuf buf) {
        this.playingIndex = buf.readVarInt();
        this.track = buf.readVarInt();
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

    /**
     * @return The playing index to set the jukebox to
     */
    public int getPlayingIndex() {
        return this.playingIndex;
    }

    /**
     * @return The track to set the jukebox to
     */
    public int getTrack() {
        return this.track;
    }
}
