package gg.moonflower.etched.client.search;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.moonflower.etched.common.sound.download.SoundCloudIdTracker;
import gg.moonflower.etched.common.sound.download.SoundCloudSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Searches tracks on SoundCloud. Results are track page URLs, which {@link SoundCloudSource} already knows how to play.
 */
public class SoundCloudSearchSource implements MusicSearchSource {

    private static final Component NAME = Component.literal("SoundCloud").withStyle(style -> style.withColor(TextColor.fromRgb(0xFF5500)));
    private static final Set<String> UNPLAYABLE_POLICIES = Set.of("BLOCK", "SNIP"); // SNIP tracks only play a 30 second preview
    private static final int LIMIT = 50;

    private static String formatDuration(long millis) {
        long seconds = millis / 1000;
        return seconds >= 3600 ? String.format("%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60) : String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private static boolean isPlayable(JsonObject track) {
        if (!GsonHelper.getAsBoolean(track, "streamable", false) || UNPLAYABLE_POLICIES.contains(GsonHelper.getAsString(track, "policy", ""))) {
            return false;
        }

        JsonObject media = GsonHelper.getAsJsonObject(track, "media", null);
        if (media == null) {
            return false;
        }
        for (JsonElement transcoding : GsonHelper.getAsJsonArray(media, "transcodings", new JsonArray())) {
            JsonObject format = GsonHelper.getAsJsonObject(transcoding.getAsJsonObject(), "format", null);
            if (format != null && SoundCloudSource.Format.parse(format) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Component getName() {
        return NAME;
    }

    @Override
    public List<MusicSearchResult> search(String query, Proxy proxy) throws IOException {
        return this.search(query, proxy, 0);
    }

    private List<MusicSearchResult> search(String query, Proxy proxy, int attempt) throws IOException {
        String clientId = SoundCloudIdTracker.fetch(proxy);
        if (clientId == null) {
            throw new IOException("Failed to authenticate with SoundCloud");
        }

        String url = "https://api-v2.soundcloud.com/search/tracks?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&limit=" + LIMIT + "&client_id=" + clientId;
        HttpURLConnection connection = MusicSearchSource.open(URI.create(url).toURL(), proxy);
        int response = connection.getResponseCode();
        if (attempt == 0 && (response == 401 || response == 403)) { // The client id expired, so fetch a new one
            SoundCloudIdTracker.invalidate();
            return this.search(query, proxy, 1);
        }

        try (InputStreamReader reader = new InputStreamReader(MusicSearchSource.read(connection), StandardCharsets.UTF_8)) {
            JsonArray tracks = GsonHelper.getAsJsonArray(JsonParser.parseReader(reader).getAsJsonObject(), "collection");
            List<MusicSearchResult> results = new ArrayList<>(tracks.size());
            for (JsonElement element : tracks) {
                JsonObject track = element.getAsJsonObject();
                if (!isPlayable(track)) {
                    continue;
                }

                String artist = GsonHelper.getAsString(GsonHelper.getAsJsonObject(track, "user", new JsonObject()), "username", "");
                String duration = formatDuration(GsonHelper.getAsLong(track, "full_duration", GsonHelper.getAsLong(track, "duration", 0)));
                results.add(new MusicSearchResult(GsonHelper.getAsString(track, "title", ""), artist.isBlank() ? duration : artist + " · " + duration, GsonHelper.getAsString(track, "permalink_url")));
            }
            return results;
        } catch (RuntimeException e) {
            throw new IOException("Invalid response from SoundCloud", e);
        }
    }
}
