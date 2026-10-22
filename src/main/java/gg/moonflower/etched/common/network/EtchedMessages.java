package gg.moonflower.etched.common.network;

import gg.moonflower.etched.common.network.play.*;
import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandler;
import gg.moonflower.etched.common.network.play.handler.EtchedServerPlayPacketHandler;
import gg.moonflower.etched.core.Etched;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Supplier;

@EventBusSubscriber(modid = Etched.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class EtchedMessages {

    @SubscribeEvent
    public static void init(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("4");

        // Client
        registrar.playToClient(ClientboundInvalidEtchUrlPacket.TYPE, ClientboundInvalidEtchUrlPacket.CODEC, clientHandler(() -> EtchedClientPlayPacketHandler::handleSetInvalidEtch));
        registrar.playToClient(ClientboundPlayBlockMusicPacket.TYPE, ClientboundPlayBlockMusicPacket.CODEC, clientHandler(() -> EtchedClientPlayPacketHandler::handlePlayBlockMusicPacket));
        registrar.playToClient(ClientboundPlayEntityMusicPacket.TYPE, ClientboundPlayEntityMusicPacket.CODEC, clientHandler(() -> EtchedClientPlayPacketHandler::handlePlayEntityMusicPacket));
        registrar.playToClient(ClientboundContraptionBlockDataPacket.TYPE, ClientboundContraptionBlockDataPacket.CODEC, clientHandler(() -> EtchedClientPlayPacketHandler::handleContraptionBlockData));

        // Server
        registrar.playToServer(ServerboundEditMusicLabelPacket.TYPE, ServerboundEditMusicLabelPacket.CODEC, EtchedServerPlayPacketHandler::handleEditMusicLabel);
        registrar.playToServer(ServerboundSoundSettingsPacket.TYPE, ServerboundSoundSettingsPacket.CODEC, EtchedServerPlayPacketHandler::handleSoundSettings);
        // Optional so older clients can still join. The server resolves tracks itself for them
        registrar.optional().playToServer(ServerboundEtchTrackDataPacket.TYPE, ServerboundEtchTrackDataPacket.CODEC, EtchedServerPlayPacketHandler::handleEtchTrackData);

        // Bidirectional
        registerBidirectional(registrar, SetAlbumJukeboxTrackPacket.TYPE, SetAlbumJukeboxTrackPacket.CODEC, clientHandler(() -> EtchedClientPlayPacketHandler::handleSetAlbumJukeboxTrack), EtchedServerPlayPacketHandler::handleSetAlbumJukeboxTrack);
        registerBidirectional(registrar, SetUrlPacket.TYPE, SetUrlPacket.CODEC, clientHandler(() -> EtchedClientPlayPacketHandler::handleSetUrl), EtchedServerPlayPacketHandler::handleSetUrl);
    }

    private static <T extends CustomPacketPayload> IPayloadHandler<T> clientHandler(Supplier<IPayloadHandler<T>> handler) {
        return FMLLoader.getDist() == Dist.CLIENT ? handler.get() : (payload, context) -> {
        };
    }

    private static <T extends CustomPacketPayload> void registerBidirectional(PayloadRegistrar registrar, CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> reader, IPayloadHandler<T> clientHandler, IPayloadHandler<T> serverHandler) {
        registrar.playBidirectional(type, reader, (pkt, ctx) -> {
            if (ctx.flow().isClientbound()) {
                clientHandler.handle(pkt, ctx);
            } else {
                serverHandler.handle(pkt, ctx);
            }
        });
    }
}
