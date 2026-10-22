package gg.moonflower.etched.common.sound.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.download.SoundDownloadSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.api.util.ProgressTrackingInputStream;
import gg.moonflower.etched.core.Etched;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Resolves tracks, albums and playlists from the public Audius API, which needs no authentication.
 */
public class AudiusSource implements SoundDownloadSource {

    public static final String API = "https://api.audius.co/v1";
    public static final String APP_NAME = "app_name=" + Etched.MOD_ID;
    private static final Component BRAND = Component.translatable("sound_source." + Etched.MOD_ID + ".audius").withStyle(style -> style.withColor(TextColor.fromRgb(0xCC0FE0)));

    private final Map<String, Boolean> validCache = new WeakHashMap<>();

    /**
     * Checks to see if the track can be streamed without signing in or unlocking it.
     *
     * @param track The track json
     * @return Whether the track will play
     */
    public static boolean isPlayable(JsonObject track) {
        return GsonHelper.getAsBoolean(track, "is_streamable", true) && !GsonHelper.getAsBoolean(track, "is_stream_gated", false) && !GsonHelper.getAsBoolean(track, "is_delete", false);
    }

    private InputStream get(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException {
        HttpURLConnection httpURLConnection;
        if (progressListener != null) {
            progressListener.progressStartRequest(Component.translatable("sound_source." + Etched.MOD_ID + ".requesting", this.getApiName()));
        }

        try {
            URL uRL = new URI(url).toURL();
            httpURLConnection = (HttpURLConnection) uRL.openConnection(proxy);
            httpURLConnection.setInstanceFollowRedirects(true);
            Map<String, String> map = SoundDownloadSource.getDownloadHeaders();

            for (Map.Entry<String, String> entry : map.entrySet()) {
                httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            long size = httpURLConnection.getContentLengthLong();
            int response = httpURLConnection.getResponseCode();
            if (response != 200) {
                throw new IOException(response + " " + httpURLConnection.getResponseMessage());
            }

            return size != -1 && progressListener != null ? new ProgressTrackingInputStream(httpURLConnection.getInputStream(), size, progressListener) : httpURLConnection.getInputStream();
        } catch (IOException e) {
            throw e;
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    private <T> T resolve(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy, Request<T> function) throws IOException, URISyntaxException, JsonParseException {
        JsonElement data;
        try (InputStream stream = this.get(API + "/resolve?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8) + "&" + APP_NAME, progressListener, proxy)) {
            data = JsonParser.parseString(IOUtils.toString(stream, StandardCharsets.UTF_8)).getAsJsonObject().get("data");
        }
        if (data != null && data.isJsonArray()) {
            JsonArray array = data.getAsJsonArray();
            data = array.isEmpty() ? null : array.get(0);
        }
        if (data == null || !data.isJsonObject()) {
            throw new IOException("URL is not a track, album or playlist");
        }

        JsonObject json = data.getAsJsonObject();
        if (json.has("playlist_name")) {
            List<JsonObject> tracks = new ArrayList<>();
            for (JsonElement track : GsonHelper.getAsJsonArray(json, "tracks", new JsonArray())) {
                tracks.add(track.getAsJsonObject());
            }
            return function.process(json, tracks);
        }
        if (json.has("title")) {
            return function.process(null, Collections.singletonList(json));
        }
        throw new IOException("URL is not a track, album or playlist");
    }

    private static String getArtist(JsonObject json) {
        JsonObject user = GsonHelper.getAsJsonObject(json, "user", new JsonObject());
        return GsonHelper.getAsString(user, "name", GsonHelper.getAsString(user, "handle", TrackData.EMPTY.artist()));
    }

    @Nullable
    private static String getArtwork(JsonObject json) {
        if (!json.has("artwork") || !json.get("artwork").isJsonObject()) {
            return null;
        }
        return GsonHelper.getAsString(GsonHelper.getAsJsonObject(json, "artwork"), "480x480", null);
    }

    @Override
    public Collection<URL> resolveUrl(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException, URISyntaxException, JsonParseException {
        return this.resolve(url, progressListener, proxy, (container, tracks) -> {
            if (progressListener != null) {
                progressListener.progressStartRequest(RESOLVING_TRACKS);
            }
            List<URL> trackUrls = new ArrayList<>(tracks.size());
            for (JsonObject track : tracks) {
                if (isPlayable(track)) {
                    trackUrls.add(new URI(API + "/tracks/" + GsonHelper.getAsString(track, "id") + "/stream?" + APP_NAME).toURL());
                }
            }
            return trackUrls;
        });
    }

    @Override
    public Collection<TrackData> resolveTracks(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException, URISyntaxException, JsonParseException {
        return this.resolve(url, progressListener, proxy, (container, tracks) -> {
            if (container == null) {
                JsonObject track = tracks.getFirst();
                return Collections.singleton(new TrackData(url, getArtist(track), Component.literal(GsonHelper.getAsString(track, "title"))));
            }

            List<TrackData> result = new ArrayList<>(tracks.size() + 1);
            result.add(new TrackData(url, getArtist(container), Component.literal(GsonHelper.getAsString(container, "playlist_name"))));
            for (JsonObject track : tracks) {
                if (isPlayable(track)) {
                    result.add(new TrackData("https://audius.co" + GsonHelper.getAsString(track, "permalink"), getArtist(track), Component.literal(GsonHelper.getAsString(track, "title"))));
                }
            }
            return result;
        });
    }

    @Override
    public Optional<String> resolveAlbumCover(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy, ResourceManager resourceManager) throws IOException, URISyntaxException, JsonParseException {
        return this.resolve(url, progressListener, proxy, (container, tracks) -> {
            String artwork = container != null ? getArtwork(container) : null;
            if (artwork == null && !tracks.isEmpty()) {
                artwork = getArtwork(tracks.getFirst());
            }
            return Optional.ofNullable(artwork);
        });
    }

    @Override
    public boolean isValidUrl(String url) {
        return this.validCache.computeIfAbsent(url, key -> {
            try {
                String host = new URI(key).getHost();
                return host != null && (host.equals("audius.co") || host.endsWith(".audius.co")) && !host.startsWith("api.");
            } catch (URISyntaxException e) {
                return false;
            }
        });
    }

    @Override
    public boolean isTemporary(String url) {
        return true;
    }

    @Override
    public String getApiName() {
        return "Audius";
    }

    @Override
    public Optional<Component> getBrandText(String url) {
        return Optional.of(BRAND);
    }

    @FunctionalInterface
    private interface Request<T> {

        T process(@Nullable JsonObject container, List<JsonObject> tracks) throws IOException, JsonParseException, URISyntaxException;
    }
}
