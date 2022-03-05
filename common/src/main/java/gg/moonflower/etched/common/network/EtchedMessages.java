package gg.moonflower.etched.common.network;

import gg.moonflower.etched.common.network.play.*;
import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandlerImpl;
import gg.moonflower.etched.common.network.play.handler.EtchedServerPlayPacketHandlerImpl;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.pollen.api.network.PollinatedPlayNetworkChannel;
import gg.moonflower.pollen.api.network.packet.PollinatedPacketDirection;
import gg.moonflower.pollen.api.registry.NetworkRegistry;
import net.minecraft.resources.ResourceLocation;

public class EtchedMessages {

    public static final PollinatedPlayNetworkChannel PLAY = NetworkRegistry.createPlay(new ResourceLocation(Etched.MOD_ID, "play"), "1", () -> EtchedClientPlayPacketHandlerImpl::new, () -> EtchedServerPlayPacketHandlerImpl::new);

    public static void init() {
        PLAY.register(ClientboundAddMinecartJukeboxPacket.class, ClientboundAddMinecartJukeboxPacket::new, PollinatedPacketDirection.PLAY_CLIENTBOUND);
        PLAY.register(ClientboundInvalidEtchUrlPacket.class, ClientboundInvalidEtchUrlPacket::new, PollinatedPacketDirection.PLAY_CLIENTBOUND);
        PLAY.register(ClientboundPlayEntityMusicPacket.class, ClientboundPlayEntityMusicPacket::new, PollinatedPacketDirection.PLAY_CLIENTBOUND);
        PLAY.register(ClientboundPlayMusicPacket.class, ClientboundPlayMusicPacket::new, PollinatedPacketDirection.PLAY_CLIENTBOUND);
        PLAY.register(ClientboundSetUrlPacket.class, ClientboundSetUrlPacket::new, PollinatedPacketDirection.PLAY_CLIENTBOUND);
        PLAY.register(ServerboundSetUrlPacket.class, ServerboundSetUrlPacket::new, PollinatedPacketDirection.PLAY_SERVERBOUND);
        PLAY.register(ServerboundEditMusicLabelPacket.class, ServerboundEditMusicLabelPacket::new, PollinatedPacketDirection.PLAY_SERVERBOUND);
        PLAY.register(SetAlbumJukeboxTrackPacket.class, SetAlbumJukeboxTrackPacket::new, null); // Bidirectional
    }
}
