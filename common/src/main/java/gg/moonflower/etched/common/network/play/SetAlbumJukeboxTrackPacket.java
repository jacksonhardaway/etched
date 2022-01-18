package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.common.network.play.handler.EtchedServerPlayPacketHandler;
import gg.moonflower.pollen.api.network.packet.PollinatedPacket;
import gg.moonflower.pollen.api.network.packet.PollinatedPacketContext;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author Ocelot
 */
public class SetAlbumJukeboxTrackPacket implements PollinatedPacket<EtchedServerPlayPacketHandler> {

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
    public void processPacket(EtchedServerPlayPacketHandler handler, PollinatedPacketContext ctx) {
        handler.handleSetAlbumJukeboxTrack(this, ctx);
    }

    /**
     * @return The playing index to set the jukebox to
     */
    public int getPlayingIndex() {
        return playingIndex;
    }

    /**
     * @return The track to set the jukebox to
     */
    public int getTrack() {
        return track;
    }
}
