package gg.moonflower.etched.api.sound.download;

import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A source for audio to download from besides a direct URL.
 *
 * @author Ocelot
 * @since 2.0.0
 */
public interface SoundDownloadSource {

    /**
     * @return The vanilla Minecraft download headers
     */
    static Map<String, String> getDownloadHeaders() {
        Map<String, String> map = new HashMap<>();
        map.put("X-Minecraft-Version", SharedConstants.getCurrentVersion().getName());
        map.put("X-Minecraft-Version-ID", SharedConstants.getCurrentVersion().getId());
        map.put("User-Agent", "Minecraft Java/" + SharedConstants.getCurrentVersion().getName());
        return map;
    }

    /**
     * Resolves the streaming URL for the specified track.
     *
     * @param trackUrl         The URL to the track
     * @param progressListener The listener for net status
     * @param proxy            The internet proxy
     * @return The URL to the audio file
     * @throws IOException        If any error occurs with requests
     * @throws JsonParseException If any error occurs when parsing
     */
    List<URL> resolveUrl(String trackUrl, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException;

    /**
     * Resolves the artist and title for the specified track.
     *
     * @param trackUrl         The URL to the track
     * @param progressListener The listener for net status
     * @param proxy            The internet proxy
     * @return The artist and title in a pair
     * @throws IOException        If any error occurs with requests
     * @throws JsonParseException If any error occurs when parsing
     */
    Optional<Pair<String, String>> resolveTrack(String trackUrl, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException, JsonParseException;

    /**
     * Checks to see if the specified URL is for this source.
     *
     * @param url The URL to go to
     * @return Whether that URL is valid
     */
    boolean isValidUrl(String url);

    /**
     * Checks to see if the specified URL should be stored in the temporary cache.
     *
     * @param url The URL to check
     * @return <code>true</code> if the sound should be placed in the temporary folder or <code>false</code> to place it in the minecraft folder
     */
    boolean isTemporary(String url);

    /**
     * @return The name of this API source
     */
    String getApiName();

    /**
     * Retrieves the special "brand" text for this source.
     *
     * @param url The URL being queried
     * @return The text to display as the brand or nothing
     */
    default Optional<Component> getBrandText(String url) {
        return Optional.empty();
    }
}
