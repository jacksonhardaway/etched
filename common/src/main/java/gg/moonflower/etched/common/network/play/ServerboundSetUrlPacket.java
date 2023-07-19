package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.common.network.play.handler.EtchedServerPlayPacketHandler;
import gg.moonflower.pollen.api.network.v1.packet.PollinatedPacket;
import gg.moonflower.pollen.api.network.v1.packet.PollinatedPacketContext;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author Jackson
 */
public class ServerboundSetUrlPacket implements PollinatedPacket<EtchedServerPlayPacketHandler> {

    private final String url;

    public ServerboundSetUrlPacket(String url) {
        this.url = url;
    }

    public ServerboundSetUrlPacket(FriendlyByteBuf buf) {
        this.url = buf.readUtf(32767);
    }

    @Override
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeUtf(this.url);
    }

    @Override
    public void processPacket(EtchedServerPlayPacketHandler handler, PollinatedPacketContext ctx) {
        handler.handleSetUrl(this, ctx);
    }

    /**
     * @return The URL to set in the etching table
     */
    public String getUrl() {
        return url;
    }

}
