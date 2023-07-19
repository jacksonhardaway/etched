package gg.moonflower.etched.common.network;

import gg.moonflower.etched.common.network.play.*;
import gg.moonflower.etched.common.network.play.handler.EtchedServerPlayPacketHandlerImpl;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.pollen.api.network.v1.PollinatedPlayNetworkChannel;
import gg.moonflower.pollen.api.network.v1.packet.PollinatedPacketDirection;
import gg.moonflower.pollen.api.registry.network.v1.PollinatedNetworkRegistry;
import net.minecraft.resources.ResourceLocation;

public class EtchedMessages {

    public static final PollinatedPlayNetworkChannel PLAY = PollinatedNetworkRegistry.createPlay(new ResourceLocation(Etched.MOD_ID, "play"), "2");

    public static void init() {
        PLAY.register(ClientboundInvalidEtchUrlPacket.class, ClientboundInvalidEtchUrlPacket::new, PollinatedPacketDirection.PLAY_CLIENTBOUND);
        PLAY.register(ClientboundPlayEntityMusicPacket.class, ClientboundPlayEntityMusicPacket::new, PollinatedPacketDirection.PLAY_CLIENTBOUND);
        PLAY.register(ClientboundPlayMusicPacket.class, ClientboundPlayMusicPacket::new, PollinatedPacketDirection.PLAY_CLIENTBOUND);
        PLAY.register(ClientboundSetUrlPacket.class, ClientboundSetUrlPacket::new, PollinatedPacketDirection.PLAY_CLIENTBOUND);
        PLAY.register(ServerboundSetUrlPacket.class, ServerboundSetUrlPacket::new, PollinatedPacketDirection.PLAY_SERVERBOUND);
        PLAY.register(ServerboundEditMusicLabelPacket.class, ServerboundEditMusicLabelPacket::new, PollinatedPacketDirection.PLAY_SERVERBOUND);
        PLAY.register(SetAlbumJukeboxTrackPacket.class, SetAlbumJukeboxTrackPacket::new, null); // Bidirectional
        PLAY.setServerHandler(new EtchedServerPlayPacketHandlerImpl());
    }
}
