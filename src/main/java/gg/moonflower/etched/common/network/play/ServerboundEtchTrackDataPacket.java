package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.core.Etched;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * Sends the track info the client resolved for an etching table URL, so the server doesn't need to reach the music service itself.
 *
 * @param url    The URL the tracks were resolved from
 * @param tracks The resolved tracks. For albums, the first entry is the album itself
 * @param error  Why the tracks could not be resolved or an empty string if they were
 */
@ApiStatus.Internal
public record ServerboundEtchTrackDataPacket(String url, List<TrackData> tracks, String error) implements CustomPacketPayload {

    public static final int MAX_TRACKS = 256;
    public static final int MAX_ERROR_LENGTH = 256;

    public static final CustomPacketPayload.Type<ServerboundEtchTrackDataPacket> TYPE = new CustomPacketPayload.Type<>(Etched.etchedPath("etch_track_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundEtchTrackDataPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ServerboundEtchTrackDataPacket::url,
            TrackData.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_TRACKS)),
            ServerboundEtchTrackDataPacket::tracks,
            ByteBufCodecs.stringUtf8(MAX_ERROR_LENGTH),
            ServerboundEtchTrackDataPacket::error,
            ServerboundEtchTrackDataPacket::new);

    public static ServerboundEtchTrackDataPacket success(String url, List<TrackData> tracks) {
        return new ServerboundEtchTrackDataPacket(url, tracks.size() > MAX_TRACKS ? tracks.subList(0, MAX_TRACKS) : tracks, "");
    }

    public static ServerboundEtchTrackDataPacket failure(String url, String error) {
        return new ServerboundEtchTrackDataPacket(url, List.of(), error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
