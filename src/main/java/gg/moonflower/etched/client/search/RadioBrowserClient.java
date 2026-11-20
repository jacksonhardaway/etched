package gg.moonflower.etched.client.search;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Searches the public Radio Browser catalog for streams Etched can play directly.
 */
public final class RadioBrowserClient {

    private static final String DISCOVERY_HOST = "all.api.radio-browser.info";
    private static final List<String> FALLBACK_HOSTS = List.of(DISCOVERY_HOST, "de1.api.radio-browser.info");
    private static final Set<String> SUPPORTED_CODECS = Set.of("MP3", "MPEG", "AAC", "AAC+");
    private static final int RESULT_LIMIT = 30;
    private static final int MAX_RESPONSE_SIZE = 512 * 1024;
    private static final int CONNECT_TIMEOUT = 5_000;
    private static final int READ_TIMEOUT = 10_000;
    private static final String USER_AGENT = "Etched-Regalade/5.1.0";

    private RadioBrowserClient() {
    }

    public static List<Station> search(String query, Proxy proxy) throws IOException {
        IOException lastError = null;
        for (String host : findHosts()) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException("Radio Browser search was cancelled");
            }

            try {
                String apiBase = "https://" + host;
                String url = apiBase + "/json/stations/search?name="
                        + URLEncoder.encode(query, StandardCharsets.UTF_8)
                        + "&hidebroken=true&order=clickcount&reverse=true&limit=" + RESULT_LIMIT;
                return parseStations(request(URI.create(url), proxy, MAX_RESPONSE_SIZE), apiBase);
            } catch (IOException exception) {
                lastError = exception;
            }
        }
        throw lastError != null ? lastError : new IOException("No Radio Browser server is available");
    }

    public static void registerClick(Station station, Proxy proxy) {
        if (station.stationUuid().isBlank()) {
            return;
        }

        try {
            String uuid = URLEncoder.encode(station.stationUuid(), StandardCharsets.UTF_8);
            request(URI.create(station.apiBase() + "/json/url/" + uuid), proxy, 64 * 1024);
        } catch (Exception ignored) {
            // Popularity tracking is best-effort and must never prevent playback.
        }
    }

    static List<Station> parseStations(byte[] response, String apiBase) throws IOException {
        try {
            JsonArray stations = JsonParser.parseString(new String(response, StandardCharsets.UTF_8)).getAsJsonArray();
            List<Station> results = new ArrayList<>(Math.min(stations.size(), RESULT_LIMIT));
            Set<String> urls = new HashSet<>();

            for (JsonElement element : stations) {
                JsonObject station = element.getAsJsonObject();
                String codec = string(station, "codec").toUpperCase(Locale.ROOT);
                if (!SUPPORTED_CODECS.contains(codec) || isHls(station)) {
                    continue;
                }

                String streamUrl = string(station, "url_resolved").strip();
                if (streamUrl.isEmpty()) {
                    streamUrl = string(station, "url").strip();
                }
                if (streamUrl.isEmpty() || streamUrl.length() > 32768 || !urls.add(streamUrl)) {
                    continue;
                }

                URI streamUri;
                try {
                    streamUri = URI.create(streamUrl);
                } catch (IllegalArgumentException exception) {
                    continue;
                }
                if (!"http".equalsIgnoreCase(streamUri.getScheme()) && !"https".equalsIgnoreCase(streamUri.getScheme())) {
                    continue;
                }

                String name = string(station, "name").strip();
                if (name.isEmpty()) {
                    name = streamUri.getHost();
                    if (name == null) {
                        name = "Unnamed station";
                    }
                }

                StringJoiner details = new StringJoiner(" | ");
                String countryCode = string(station, "countrycode").strip();
                if (!countryCode.isEmpty()) {
                    details.add(countryCode.toUpperCase(Locale.ROOT));
                }
                int bitrate = integer(station, "bitrate");
                details.add(bitrate > 0 ? codec + " " + bitrate + " kbps" : codec);

                results.add(new Station(
                        name,
                        details.toString(),
                        streamUrl,
                        string(station, "stationuuid"),
                        apiBase
                ));
            }
            return results;
        } catch (RuntimeException exception) {
            throw new IOException("Radio Browser returned an invalid response", exception);
        }
    }

    private static boolean isHls(JsonObject station) {
        JsonElement value = station.get("hls");
        if (value == null || value.isJsonNull()) {
            return false;
        }
        if (!value.isJsonPrimitive()) {
            return true;
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isNumber()) {
            return primitive.getAsInt() != 0;
        }
        return Boolean.parseBoolean(primitive.getAsString());
    }

    private static String string(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }

    private static int integer(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive()) {
            return 0;
        }
        try {
            return value.getAsInt();
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private static List<String> findHosts() {
        Set<String> hosts = new LinkedHashSet<>();
        try {
            for (InetAddress address : InetAddress.getAllByName(DISCOVERY_HOST)) {
                String host = address.getCanonicalHostName().toLowerCase(Locale.ROOT);
                if (host.endsWith(".api.radio-browser.info")) {
                    hosts.add(host);
                }
            }
        } catch (IOException ignored) {
            // Static fallbacks below keep search usable when DNS discovery fails.
        }
        hosts.addAll(FALLBACK_HOSTS);
        return List.copyOf(hosts);
    }

    private static byte[] request(URI uri, Proxy proxy, int maximumSize) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection(proxy != null ? proxy : Proxy.NO_PROXY);
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", USER_AGENT);

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Radio Browser returned HTTP " + responseCode);
            }

            try (InputStream stream = connection.getInputStream()) {
                byte[] data = stream.readNBytes(maximumSize + 1);
                if (data.length > maximumSize) {
                    throw new IOException("Radio Browser response is too large");
                }
                return data;
            }
        } finally {
            connection.disconnect();
        }
    }

    public record Station(String name, String details, String url, String stationUuid, String apiBase) {
    }
}
