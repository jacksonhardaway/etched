package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandler;
import gg.moonflower.pollen.api.network.v1.packet.PollinatedPacket;
import gg.moonflower.pollen.api.network.v1.packet.PollinatedPacketContext;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ocelot
 */
public class ClientboundSetUrlPacket implements PollinatedPacket<EtchedClientPlayPacketHandler> {

    private final String url;

    public ClientboundSetUrlPacket(@Nullable String url) {
        this.url = url != null ? url : "";
    }

    public ClientboundSetUrlPacket(FriendlyByteBuf buf) {
        this.url = buf.readUtf(32767);
    }

    @Override
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeUtf(this.url);
    }

    @Override
    public void processPacket(EtchedClientPlayPacketHandler handler, PollinatedPacketContext ctx) {
        handler.handleSetUrl(this, ctx);
    }

    /**
     * @return The URL to set in the etching table
     */
    public String getUrl() {
        return url;
    }

}
