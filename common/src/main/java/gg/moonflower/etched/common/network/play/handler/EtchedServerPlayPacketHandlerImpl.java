package gg.moonflower.etched.common.network.play.handler;

import gg.moonflower.etched.common.menu.AlbumJukeboxMenu;
import gg.moonflower.etched.common.menu.EtchingMenu;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.ServerboundSetEtchingUrlPacket;
import gg.moonflower.etched.common.network.play.SetAlbumJukeboxTrackPacket;
import gg.moonflower.pollen.api.network.packet.PollinatedPacketContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
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

    @Override
    public void handleSetAlbumJukeboxTrack(SetAlbumJukeboxTrackPacket pkt, PollinatedPacketContext ctx) {
        Player player = ctx.getSender();
        if (player == null)
            return;

        if (player.containerMenu instanceof AlbumJukeboxMenu) {
            AlbumJukeboxMenu menu = (AlbumJukeboxMenu) player.containerMenu;
            if (menu.setPlayingTrack(player.level, pkt))
                EtchedMessages.PLAY.sendToTracking((ServerLevel) player.level, menu.getPos(), new SetAlbumJukeboxTrackPacket(pkt.getPlayingIndex(), pkt.getTrack()));
        }
    }
}
