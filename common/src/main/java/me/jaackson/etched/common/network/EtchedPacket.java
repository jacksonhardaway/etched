package me.jaackson.etched.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Jackson
 */
public interface EtchedPacket {

    /**
     * Serializes the data in this packet into the specified buffer
     *
     * @param buf The buffer to write data into
     */
    void write(FriendlyByteBuf buf);

    /**
     * @return The channel to send this packet over
     */
    ResourceLocation getChannel();
}
