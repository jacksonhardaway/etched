package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.core.Etched;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetRadioSettingsPacket(int volumePercent, int range) implements CustomPacketPayload {

    public static final Type<SetRadioSettingsPacket> TYPE = new Type<>(Etched.etchedPath("set_radio_settings"));
    public static final StreamCodec<FriendlyByteBuf, SetRadioSettingsPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SetRadioSettingsPacket::volumePercent,
            ByteBufCodecs.VAR_INT,
            SetRadioSettingsPacket::range,
            SetRadioSettingsPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
