package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandler;
import gg.moonflower.pollen.api.network.packet.PollinatedPacket;
import gg.moonflower.pollen.api.network.packet.PollinatedPacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.io.IOException;

/**
 * @author Ocelot
 */
public class ClientboundPlayMusicPacket implements PollinatedPacket<EtchedClientPlayPacketHandler> {

    private final TrackData[] tracks;
    private final BlockPos pos;

    public ClientboundPlayMusicPacket(TrackData[] tracks, BlockPos pos) {
        this.tracks = tracks;
        this.pos = pos;
    }

    public ClientboundPlayMusicPacket(FriendlyByteBuf buf) throws IOException {
        this.tracks = new TrackData[buf.readVarInt()];
        for (int i = 0; i < this.tracks.length; i++)
            this.tracks[i] = buf.readWithCodec(TrackData.CODEC);
        this.pos = buf.readBlockPos();
    }

    @Override
    public void writePacketData(FriendlyByteBuf buf) throws IOException {
        buf.writeVarInt(this.tracks.length);
        for (TrackData track : this.tracks)
            buf.writeWithCodec(TrackData.CODEC, track);
        buf.writeBlockPos(this.pos);
    }

    @Override
    public void processPacket(EtchedClientPlayPacketHandler handler, PollinatedPacketContext ctx) {
        handler.handlePlayMusicPacket(this, ctx);
    }

    /**
     * @return The tracks to play in sequence
     */
    public TrackData[] getTracks() {
        return tracks;
    }

    /**
     * @return The position the music disk is playing at
     */
    public BlockPos getPos() {
        return pos;
    }
}
