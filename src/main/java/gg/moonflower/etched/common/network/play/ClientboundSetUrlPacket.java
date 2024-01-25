package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * @param url The URL to set in the etching table
 * @author Ocelot
 */
@ApiStatus.Internal
public record ClientboundSetUrlPacket(String url) implements EtchedPacket {

    public ClientboundSetUrlPacket(@Nullable String url) {
        this.url = url != null ? url : "";
    }

    public ClientboundSetUrlPacket(FriendlyByteBuf buf) {
        this(buf.readUtf());
    }

    @Override
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeUtf(this.url);
    }

    @Override
    public void processPacket(NetworkEvent.Context ctx) {
        EtchedClientPlayPacketHandler.handleSetUrl(this, ctx);
    }
}
