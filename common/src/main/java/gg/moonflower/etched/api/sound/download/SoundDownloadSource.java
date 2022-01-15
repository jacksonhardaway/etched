package gg.moonflower.etched.api.sound.download;

import com.google.gson.JsonParseException;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.core.Etched;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
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

    ResourceLocation DEFAULT_ART = new ResourceLocation(Etched.MOD_ID, "textures/default_album_cover.png");
    Component RESOLVING_TRACKS = new TranslatableComponent("record." + Etched.MOD_ID + ".resolvingTracks");

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
    Optional<TrackData> resolveTrack(String trackUrl, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException, JsonParseException;

    /**
     * Resolves the input stream to the cover for the specified album.
     *
     * @param trackUrl         The URL to the album
     * @param progressListener The listener for net status
     * @param proxy            The internet proxy
     * @return A stream to the track or <code>{@link Optional#empty()}</code> if there is no cover
     * @throws IOException If any error occurs with requests
     */
    Optional<InputStream> resolveAlbumCover(String trackUrl, @Nullable DownloadProgressListener progressListener, Proxy proxy, ResourceManager resourceManager) throws IOException;

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

    /**
     * Information about track metadata for discs
     *
     * @author Ocelot
     * @since 2.0.0
     */
    class TrackData {

        private final String artist;
        private final String title;
        private final boolean album;

        public TrackData(String artist, String title, boolean album) {
            this.artist = artist;
            this.title = title;
            this.album = album;
        }

        /**
         * @return The name of the artist
         */
        public String getArtist() {
            return artist;
        }

        /**
         * @return The title of the piece
         */
        public String getTitle() {
            return title;
        }

        /**
         * @return Whether the music is an album or a single track
         */
        public boolean isAlbum() {
            return album;
        }
    }
}
