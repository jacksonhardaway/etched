package me.jaackson.etched.common.network.handler;

import me.jaackson.etched.common.menu.EtchingMenu;
import me.jaackson.etched.common.network.ServerboundSetEtcherUrlPacket;
import net.minecraft.world.entity.player.Player;

public class EtchedServerPlayHandler {

    public static void handleSetEtcherUrl(ServerboundSetEtcherUrlPacket pkt, Player player) {
        if (player.containerMenu instanceof EtchingMenu) {
            EtchingMenu menu = (EtchingMenu) player.containerMenu;
            menu.setUrl(pkt.getUrl());
        }
    }
}
