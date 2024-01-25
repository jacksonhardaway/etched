package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * @param exception The exception to set in the etching table
 * @author Jackson
 */
@ApiStatus.Internal
public record ClientboundInvalidEtchUrlPacket(String exception) implements EtchedPacket {

    public ClientboundInvalidEtchUrlPacket(FriendlyByteBuf buf) {
        this(buf.readUtf());
    }

    @Override
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeUtf(this.exception);
    }

    @Override
    public void processPacket(NetworkEvent.Context ctx) {
        EtchedClientPlayPacketHandler.handleSetInvalidEtch(this, ctx);
    }
}
