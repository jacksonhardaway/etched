package gg.moonflower.etched.common.network.play.handler;

import gg.moonflower.etched.common.network.play.ServerboundEditMusicLabelPacket;
import gg.moonflower.etched.common.network.play.ServerboundSetEtchingTableUrlPacket;
import gg.moonflower.pollen.api.network.packet.PollinatedPacketContext;

public interface EtchedServerPlayPacketHandler extends EtchedPlayPacketHandler {

    void handleSetEtchingTableUrl(ServerboundSetEtchingTableUrlPacket pkt, PollinatedPacketContext ctx);

    void handleEditMusicLabel(ServerboundEditMusicLabelPacket pkt, PollinatedPacketContext ctx);
}
