package gg.moonflower.etched.api.record;

import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.pollen.api.util.NbtConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Information about track metadata for discs
 *
 * @author Ocelot
 * @since 2.0.0
 */
public class TrackData {

    public static final TrackData EMPTY = new TrackData(null, "Unknown", new TextComponent("Custom Music"));
    public static final Codec<TrackData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("Url").forGetter(TrackData::getUrl),
            Codec.STRING.optionalFieldOf("Author", EMPTY.getArtist()).forGetter(TrackData::getArtist),
            Codec.STRING.optionalFieldOf("Title", Component.Serializer.toJson(EMPTY.getTitle())).<Component>xmap(json -> {
                if (!json.startsWith("{"))
                    return new TextComponent(json);
                try {
                    return Component.Serializer.fromJson(json);
                } catch (JsonParseException e) {
                    return new TextComponent(json);
                }
            }, Component.Serializer::toJson).forGetter(TrackData::getTitle)
    ).apply(instance, TrackData::new));

    private static final Pattern RESOURCE_LOCATION_PATTERN = Pattern.compile("[a-z0-9_.-]+");

    private final String url;
    private final String artist;
    private final Component title;

    public TrackData(String url, String artist, Component title) {
        this.url = url;
        this.artist = artist;
        this.title = title;
    }

    public static boolean isValid(CompoundTag nbt) {
        return nbt.contains("Url", NbtConstants.STRING) && isValidURL(nbt.getString("Url"));
    }

    /**
     * Checks to see if the specified string is a valid music URL.
     *
     * @param url The text to check
     * @return Whether the data is valid
     */
    public static boolean isValidURL(String url) {
        if (isLocalSound(url))
            return true;
        try {
            String scheme = new URI(url).getScheme();
            return "http".equals(scheme) || "https".equals(scheme);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Checks to see if the specified URL is a resource location sound.
     *
     * @param url The url to check
     * @return Whether that sound can be played as a local sound event
     */
    public static boolean isLocalSound(String url) {
        String[] parts = url.split(":");
        if (parts.length > 2)
            return false;
        for (String part : parts)
            if (!RESOURCE_LOCATION_PATTERN.matcher(part).matches())
                return false;
        return true;
    }

    public CompoundTag save(CompoundTag nbt) {
        if (this.url != null)
            nbt.putString("Url", this.url);
        if (this.title != null)
            nbt.putString("Title", Component.Serializer.toJson(this.title));
        if (this.artist != null)
            nbt.putString("Author", this.artist);
        return nbt;
    }

    /**
     * @return The URL for the track
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return The name of the artist
     */
    public String getArtist() {
        return artist;
    }

    /**
     * @return The title of the track
     */
    public Component getTitle() {
        return title;
    }

    public TrackData withUrl(String url) {
        return new TrackData(url, this.artist, this.title);
    }

    public TrackData withArtist(String artist) {
        return new TrackData(this.url, artist, this.title);
    }

    public TrackData withTitle(String title) {
        return new TrackData(this.url, this.artist, new TextComponent(title));
    }

    public TrackData withTitle(Component title) {
        return new TrackData(this.url, this.artist, title);
    }

    /**
     * @return The name to show as the record title
     */
    public Component getDisplayName() {
        return new TranslatableComponent("sound_source." + Etched.MOD_ID + ".info", this.artist, this.title);
    }
}
