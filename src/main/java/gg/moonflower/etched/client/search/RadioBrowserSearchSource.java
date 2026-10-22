package gg.moonflower.etched.client.search;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Proxy;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Searches internet radio stations using the public <a href="https://www.radio-browser.info">Radio Browser</a> catalog.
 */
public class RadioBrowserSearchSource implements MusicSearchSource {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Component NAME = Component.literal("Radio Browser");
    private static final String[] SERVERS = {"all.api.radio-browser.info", "de1.api.radio-browser.info", "de2.api.radio-browser.info"};
    private static final Set<String> SUPPORTED_CODECS = Set.of("MP3", "AAC", "AAC+"); // OGG is skipped since many stations use Opus, which can't be decoded
    private static final int LIMIT = 50;

    @Override
    public Component getName() {
        return NAME;
    }

    @Override
    public List<MusicSearchResult> search(String query, Proxy proxy) throws IOException {
        IOException error = null;
        for (String server : SERVERS) {
            try {
                return this.search(server, query, proxy);
            } catch (IOException e) {
                LOGGER.debug("Failed to search {}", server, e);
                error = e;
            }
        }
        throw error;
    }

    private List<MusicSearchResult> search(String server, String query, Proxy proxy) throws IOException {
        String url = "https://" + server + "/json/stations/search?name=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&limit=" + LIMIT + "&hidebroken=true&order=clickcount&reverse=true";
        try (InputStreamReader reader = new InputStreamReader(MusicSearchSource.read(MusicSearchSource.open(URI.create(url).toURL(), proxy)), StandardCharsets.UTF_8)) {
            JsonArray stations = JsonParser.parseReader(reader).getAsJsonArray();
            List<MusicSearchResult> results = new ArrayList<>(stations.size());
            Set<String> urls = new HashSet<>(); // The catalog often lists the same stream several times
            for (JsonElement element : stations) {
                JsonObject station = element.getAsJsonObject();
                String codec = GsonHelper.getAsString(station, "codec", "").toUpperCase(Locale.ROOT);
                if (!SUPPORTED_CODECS.contains(codec) || GsonHelper.getAsInt(station, "hls", 0) != 0) {
                    continue;
                }

                String streamUrl = GsonHelper.getAsString(station, "url_resolved", "");
                if (StringUtil.isNullOrEmpty(streamUrl)) {
                    streamUrl = GsonHelper.getAsString(station, "url", "");
                }
                if (StringUtil.isNullOrEmpty(streamUrl) || !urls.add(streamUrl)) {
                    continue;
                }

                StringJoiner details = new StringJoiner(" · ");
                String country = GsonHelper.getAsString(station, "country", "");
                if (!country.isBlank()) {
                    details.add(country);
                }
                int bitrate = GsonHelper.getAsInt(station, "bitrate", 0);
                details.add(bitrate > 0 ? codec + " " + bitrate + " kbps" : codec);

                results.add(new MusicSearchResult(GsonHelper.getAsString(station, "name", "").strip(), details.toString(), streamUrl));
            }
            return results;
        } catch (RuntimeException e) {
            throw new IOException("Invalid response from " + server, e);
        }
    }
}
