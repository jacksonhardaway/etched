package gg.moonflower.etched.common.network;

import gg.moonflower.etched.common.network.play.*;
import gg.moonflower.etched.core.Etched;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public class EtchedMessages {

    public static final SimpleChannel PLAY = NetworkRegistry.newSimpleChannel(new ResourceLocation(Etched.MOD_ID, "play"), () -> "3", "3"::equals, "3"::equals);

    private static int index = 0;

    public static synchronized void init() {
        register(ClientboundInvalidEtchUrlPacket.class, ClientboundInvalidEtchUrlPacket::new, NetworkDirection.PLAY_TO_CLIENT);
        register(ClientboundPlayEntityMusicPacket.class, ClientboundPlayEntityMusicPacket::new, NetworkDirection.PLAY_TO_CLIENT);
        register(ClientboundPlayMusicPacket.class, ClientboundPlayMusicPacket::new, NetworkDirection.PLAY_TO_CLIENT);
        register(ClientboundSetUrlPacket.class, ClientboundSetUrlPacket::new, NetworkDirection.PLAY_TO_CLIENT);
        register(ServerboundSetUrlPacket.class, ServerboundSetUrlPacket::new, NetworkDirection.PLAY_TO_SERVER);
        register(ServerboundEditMusicLabelPacket.class, ServerboundEditMusicLabelPacket::new, NetworkDirection.PLAY_TO_SERVER);
        register(SetAlbumJukeboxTrackPacket.class, SetAlbumJukeboxTrackPacket::new, null); // Bidirectional
    }

    private static <MSG extends EtchedPacket> void register(Class<MSG> clazz, Function<FriendlyByteBuf, MSG> decoder, @Nullable NetworkDirection direction) {
        PLAY.registerMessage(index++, clazz, (msg, friendlyByteBuf) -> {
            try {
                msg.writePacketData(friendlyByteBuf);
            } catch (Exception e) {
                throw new EncoderException(e);
            }
        }, decoder, (msg, ctx) -> {
            NetworkEvent.Context context = ctx.get();
            msg.processPacket(context);
            context.setPacketHandled(true);
        }, Optional.ofNullable(direction));
    }
}
