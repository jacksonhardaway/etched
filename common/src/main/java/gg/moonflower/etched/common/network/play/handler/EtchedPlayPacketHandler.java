package gg.moonflower.etched.common.network.play.handler;

import gg.moonflower.etched.common.network.play.SetAlbumJukeboxTrackPacket;
import gg.moonflower.pollen.api.network.packet.PollinatedPacketContext;

public interface EtchedPlayPacketHandler {

    void handleSetAlbumJukeboxTrack(SetAlbumJukeboxTrackPacket pkt, PollinatedPacketContext ctx);
}
