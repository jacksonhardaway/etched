package gg.moonflower.etched.common.radio;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class RadioStationSerialization {

    public static final StreamCodec<FriendlyByteBuf, RadioStation> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            RadioStation::name,
            ByteBufCodecs.STRING_UTF8,
            RadioStation::url,
            RadioStation::new);

    private RadioStationSerialization() {
    }

    public static CompoundTag save(RadioStation station) {
        CompoundTag tag = new CompoundTag();
        if (!station.name().isEmpty()) {
            tag.putString("Name", station.name());
        }
        if (!station.url().isEmpty()) {
            tag.putString("Url", station.url());
        }
        return tag;
    }

    public static RadioStation load(CompoundTag tag) {
        String name = tag.contains("Name", Tag.TAG_STRING) ? tag.getString("Name") : "";
        String url = tag.contains("Url", Tag.TAG_STRING) ? tag.getString("Url") : "";
        return new RadioStation(name, url);
    }
}
