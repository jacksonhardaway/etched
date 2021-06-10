package me.jaackson.etched.common.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * @author Jackson
 */
public interface EtchedPacket {

    /**
     *
     * @param buf
     */
    void write(FriendlyByteBuf buf);
}
