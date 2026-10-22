package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.core.Etched;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.ApiStatus;

/**
 * @param volume The volume from 0 to 1 to set in the open music player menu
 * @param radius The radius in blocks to set in the open music player menu
 */
@ApiStatus.Internal
public record ServerboundSoundSettingsPacket(float volume, int radius) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerboundSoundSettingsPacket> TYPE = new CustomPacketPayload.Type<>(Etched.etchedPath("sound_settings"));
    public static final StreamCodec<ByteBuf, ServerboundSoundSettingsPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            ServerboundSoundSettingsPacket::volume,
            ByteBufCodecs.VAR_INT,
            ServerboundSoundSettingsPacket::radius,
            ServerboundSoundSettingsPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
