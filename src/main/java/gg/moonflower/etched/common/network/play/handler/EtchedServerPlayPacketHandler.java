package gg.moonflower.etched.common.network.play.handler;

import gg.moonflower.etched.common.component.MusicLabelComponent;
import gg.moonflower.etched.common.menu.AlbumJukeboxMenu;
import gg.moonflower.etched.common.menu.EtchingMenu;
import gg.moonflower.etched.common.menu.SoundSettingsMenu;
import gg.moonflower.etched.common.menu.UrlMenu;
import gg.moonflower.etched.common.network.play.ServerboundEditMusicLabelPacket;
import gg.moonflower.etched.common.network.play.ServerboundEtchTrackDataPacket;
import gg.moonflower.etched.common.network.play.ServerboundSoundSettingsPacket;
import gg.moonflower.etched.common.network.play.SetAlbumJukeboxTrackPacket;
import gg.moonflower.etched.common.network.play.SetUrlPacket;
import gg.moonflower.etched.core.registry.EtchedComponents;
import gg.moonflower.etched.core.registry.EtchedItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class EtchedServerPlayPacketHandler {

    public static void handleSetUrl(SetUrlPacket pkt, IPayloadContext ctx) {
        Player player = ctx.player();
        if (player.containerMenu instanceof UrlMenu menu) {
            menu.setUrl(pkt.url());
        }
    }

    public static void handleSoundSettings(ServerboundSoundSettingsPacket pkt, IPayloadContext ctx) {
        if (ctx.player().containerMenu instanceof SoundSettingsMenu menu && Float.isFinite(pkt.volume())) {
            menu.setVolume(pkt.volume());
            menu.setRadius(pkt.radius());
        }
    }

    public static void handleEtchTrackData(ServerboundEtchTrackDataPacket pkt, IPayloadContext ctx) {
        if (ctx.player().containerMenu instanceof EtchingMenu menu) {
            menu.setClientTrackData(pkt.url(), pkt.tracks(), pkt.error());
        }
    }

    public static void handleEditMusicLabel(ServerboundEditMusicLabelPacket pkt, IPayloadContext ctx) {
        int slot = pkt.slot();
        if (!Inventory.isHotbarSlot(slot) && slot != 40) {
            return;
        }

        Player player = ctx.player();
        ItemStack labelStack = player.getInventory().getItem(slot);
        if (!labelStack.is(EtchedItems.MUSIC_LABEL.get())) {
            return;
        }

        labelStack.update(EtchedComponents.MUSIC_LABEL,
                MusicLabelComponent.DEFAULT,
                label -> label.withInfo(StringUtils.normalizeSpace(pkt.artist()), StringUtils.normalizeSpace(pkt.title())));
    }

    public static void handleSetAlbumJukeboxTrack(SetAlbumJukeboxTrackPacket pkt, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }

        if (player.containerMenu instanceof AlbumJukeboxMenu menu) {
            ServerLevel level = player.serverLevel();
            if (menu.setPlayingTrack(level, pkt)) {
                PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(menu.getPos()), new SetAlbumJukeboxTrackPacket(pkt.playingIndex(), pkt.track()));
            }
        }
    }
}
