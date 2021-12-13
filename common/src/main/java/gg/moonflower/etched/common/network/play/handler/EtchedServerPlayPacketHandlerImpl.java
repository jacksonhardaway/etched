package gg.moonflower.etched.common.network.play.handler;

import gg.moonflower.etched.common.menu.EtchingMenu;
import gg.moonflower.etched.common.network.play.ServerboundSetEtchingUrlPacket;
import gg.moonflower.pollen.api.network.packet.PollinatedPacketContext;
import net.minecraft.world.entity.player.Player;

public class EtchedServerPlayPacketHandlerImpl implements EtchedServerPlayPacketHandler {

    @Override
    public void handleSetEtcherUrl(ServerboundSetEtchingUrlPacket pkt, PollinatedPacketContext ctx) {
        Player player = ctx.getSender();
        if (player == null)
            return;

        if (player.containerMenu instanceof EtchingMenu) {
            EtchingMenu menu = (EtchingMenu) player.containerMenu;
            menu.setUrl(pkt.getUrl());
        }
    }
}
