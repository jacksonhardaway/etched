package me.jaackson.etched.common.network;

import me.jaackson.etched.Etched;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Jackson
 */
public class ClientboundInvalidEtchUrlPacket implements EtchedPacket {

    public static final ResourceLocation CHANNEL = new ResourceLocation(Etched.MOD_ID, "invalid_etch");

    private final String exception;

    public ClientboundInvalidEtchUrlPacket(String exception) {
        this.exception = exception;
    }

    public ClientboundInvalidEtchUrlPacket(FriendlyByteBuf buf) {
        this.exception = buf.readUtf(32767);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.exception);
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }

    /**
     * @return The exception to set in the etching table
     */
    public String getException() {
        return exception;
    }
}
