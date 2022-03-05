package gg.moonflower.etched.common.network.play.handler;

import gg.moonflower.etched.common.network.play.*;
import gg.moonflower.pollen.api.network.packet.PollinatedPacketContext;

public interface EtchedClientPlayPacketHandler extends EtchedPlayPacketHandler {

    void handlePlayMusicPacket(ClientboundPlayMusicPacket pkt, PollinatedPacketContext ctx);

    void handleAddMinecartJukeboxPacket(ClientboundAddMinecartJukeboxPacket pkt, PollinatedPacketContext ctx);

    void handlePlayEntityMusicPacket(ClientboundPlayEntityMusicPacket pkt, PollinatedPacketContext ctx);

    void handleSetInvalidEtch(ClientboundInvalidEtchUrlPacket pkt, PollinatedPacketContext ctx);

    void handleSetUrl(ClientboundSetUrlPacket pkt, PollinatedPacketContext ctx);
}
