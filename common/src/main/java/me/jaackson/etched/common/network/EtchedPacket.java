package me.jaackson.etched.common.network;

import net.minecraft.network.FriendlyByteBuf;

public interface EtchedPacket {
    FriendlyByteBuf write(FriendlyByteBuf buf);
}
