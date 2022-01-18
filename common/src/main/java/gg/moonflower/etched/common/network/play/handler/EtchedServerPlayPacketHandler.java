package gg.moonflower.etched.common.network.play.handler;

import gg.moonflower.etched.common.network.play.SetAlbumJukeboxTrackPacket;
import gg.moonflower.etched.common.network.play.ServerboundSetEtchingUrlPacket;
import gg.moonflower.pollen.api.network.packet.PollinatedPacketContext;

public interface EtchedServerPlayPacketHandler extends EtchedPlayPacketHandler {

    void handleSetEtcherUrl(ServerboundSetEtchingUrlPacket pkt, PollinatedPacketContext ctx);
}
