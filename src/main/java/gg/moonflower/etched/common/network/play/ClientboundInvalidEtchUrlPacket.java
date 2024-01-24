package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Jackson
 */
@ApiStatus.Internal
public class ClientboundInvalidEtchUrlPacket implements EtchedPacket {

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
    public void processPacket(NetworkEvent.Context ctx) {
        EtchedClientPlayPacketHandler.handleSetInvalidEtch(this, ctx);
    }

    /**
     * @return The exception to set in the etching table
     */
    public String getException() {
        return this.exception;
    }
}
