package gg.moonflower.etched.common.network.play.handler;

import gg.moonflower.etched.common.network.play.ServerboundSetEtchingUrlPacket;
import gg.moonflower.pollen.api.network.packet.PollinatedPacketContext;

public interface EtchedServerPlayPacketHandler {

    void handleSetEtcherUrl(ServerboundSetEtchingUrlPacket pkt, PollinatedPacketContext ctx);
}
