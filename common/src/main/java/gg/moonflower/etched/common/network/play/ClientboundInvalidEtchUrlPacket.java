package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandler;
import gg.moonflower.pollen.api.network.packet.PollinatedPacket;
import gg.moonflower.pollen.api.network.packet.PollinatedPacketContext;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author Jackson
 */
public class ClientboundInvalidEtchUrlPacket implements PollinatedPacket<EtchedClientPlayPacketHandler> {

    private final String exception;

    public ClientboundInvalidEtchUrlPacket(String exception) {
        this.exception = exception;
    }

    public ClientboundInvalidEtchUrlPacket(FriendlyByteBuf buf) {
        this.exception = buf.readUtf(32767);
    }

    @Override
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeUtf(this.exception);
    }

    @Override
    public void processPacket(EtchedClientPlayPacketHandler handler, PollinatedPacketContext ctx) {
        handler.handleSetInvalidEtch(this, ctx);
    }

    /**
     * @return The exception to set in the etching table
     */
    public String getException() {
        return exception;
    }
}
