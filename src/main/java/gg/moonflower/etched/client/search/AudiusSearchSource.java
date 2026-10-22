package gg.moonflower.etched.client.search;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.moonflower.etched.common.sound.download.AudiusSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Proxy;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Searches tracks on Audius. Results are track page URLs, which {@link AudiusSource} already knows how to play.
 */
public class AudiusSearchSource implements MusicSearchSource {

    private static final Component NAME = Component.literal("Audius").withStyle(style -> style.withColor(TextColor.fromRgb(0xCC0FE0)));
    private static final int LIMIT = 50;

    private static String formatDuration(long seconds) {
        return seconds >= 3600 ? String.format("%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60) : String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    public Component getName() {
        return NAME;
    }

    @Override
    public List<MusicSearchResult> search(String query, Proxy proxy) throws IOException {
        String url = AudiusSource.API + "/tracks/search?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&limit=" + LIMIT + "&" + AudiusSource.APP_NAME;
        try (InputStreamReader reader = new InputStreamReader(MusicSearchSource.read(MusicSearchSource.open(URI.create(url).toURL(), proxy)), StandardCharsets.UTF_8)) {
            JsonArray tracks = GsonHelper.getAsJsonArray(JsonParser.parseReader(reader).getAsJsonObject(), "data");
            List<MusicSearchResult> results = new ArrayList<>(tracks.size());
            for (JsonElement element : tracks) {
                JsonObject track = element.getAsJsonObject();
                if (!AudiusSource.isPlayable(track) || !track.has("permalink")) {
                    continue;
                }

                String artist = GsonHelper.getAsString(GsonHelper.getAsJsonObject(track, "user", new JsonObject()), "name", "");
                String duration = formatDuration(GsonHelper.getAsLong(track, "duration", 0));
                results.add(new MusicSearchResult(GsonHelper.getAsString(track, "title", ""), artist.isBlank() ? duration : artist + " · " + duration, "https://audius.co" + GsonHelper.getAsString(track, "permalink")));
            }
            return results;
        } catch (RuntimeException e) {
            throw new IOException("Invalid response from Audius", e);
        }
    }
}
