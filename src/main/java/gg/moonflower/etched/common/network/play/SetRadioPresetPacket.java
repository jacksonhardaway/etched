package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.common.radio.RadioStation;
import gg.moonflower.etched.common.radio.RadioStationSerialization;
import gg.moonflower.etched.core.Etched;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetRadioPresetPacket(int index, RadioStation station, boolean activate) implements CustomPacketPayload {

    public static final Type<SetRadioPresetPacket> TYPE = new Type<>(Etched.etchedPath("set_radio_preset"));
    public static final StreamCodec<FriendlyByteBuf, SetRadioPresetPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SetRadioPresetPacket::index,
            RadioStationSerialization.STREAM_CODEC,
            SetRadioPresetPacket::station,
            ByteBufCodecs.BOOL,
            SetRadioPresetPacket::activate,
            SetRadioPresetPacket::new);

    public SetRadioPresetPacket {
        station = station != null ? station : RadioStation.EMPTY;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
