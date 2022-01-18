package gg.moonflower.etched.common.network.play.handler;

import gg.moonflower.etched.common.item.SimpleMusicLabelItem;
import gg.moonflower.etched.common.menu.AlbumJukeboxMenu;
import gg.moonflower.etched.common.menu.EtchingMenu;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.ServerboundEditMusicLabelPacket;
import gg.moonflower.etched.common.network.play.ServerboundSetEtchingTableUrlPacket;
import gg.moonflower.etched.common.network.play.SetAlbumJukeboxTrackPacket;
import gg.moonflower.pollen.api.network.packet.PollinatedPacketContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class EtchedServerPlayPacketHandlerImpl implements EtchedServerPlayPacketHandler {

    @Override
    public void handleSetEtchingTableUrl(ServerboundSetEtchingTableUrlPacket pkt, PollinatedPacketContext ctx) {
        Player player = ctx.getSender();
        if (player == null)
            return;

        if (player.containerMenu instanceof EtchingMenu) {
            EtchingMenu menu = (EtchingMenu) player.containerMenu;
            menu.setUrl(pkt.getUrl());
        }
    }

    @Override
    public void handleEditMusicLabel(ServerboundEditMusicLabelPacket pkt, PollinatedPacketContext ctx) {
        int slot = pkt.getSlot();
        if (!Inventory.isHotbarSlot(slot) && slot != 40)
            return;

        ServerPlayer player = ctx.getSender();
        if (player == null)
            return;

        ItemStack labelStack = player.getInventory().getItem(slot);
        SimpleMusicLabelItem.setTitle(labelStack, StringUtils.normalizeSpace(pkt.getTitle()));
        SimpleMusicLabelItem.setAuthor(labelStack, StringUtils.normalizeSpace(pkt.getAuthor()));
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
