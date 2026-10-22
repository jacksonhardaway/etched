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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves tracks, albums and playlists from Yandex Music. Without an OAuth token only 30-second previews are available.
 */
public class YandexMusicSource implements SoundDownloadSource {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String API = "https://api.music.yandex.net";
    private static final String SIGN_SALT = "XGRlBW9FXlekgbPrRHuSiA";
    private static final Pattern HOST_PATTERN = Pattern.compile("(?:[a-z]+\\.)?music\\.yandex\\.[a-z]+");
    private static final Pattern TRACK_PATTERN = Pattern.compile("^/(?:album/\\d+/)?track/(\\d+)");
    private static final Pattern ALBUM_PATTERN = Pattern.compile("^/album/(\\d+)/?$");
    private static final Pattern USER_PLAYLIST_PATTERN = Pattern.compile("^/users/([^/]+)/playlists/(\\d+)");
    private static final Pattern PLAYLIST_PATTERN = Pattern.compile("^/playlists?/([\\w.-]+)");
    private static final Component BRAND = Component.translatable("sound_source." + Etched.MOD_ID + ".yandex_music").withStyle(style -> style.withColor(TextColor.fromRgb(0xFFCC00)));

    private final Map<String, Boolean> validCache = new WeakHashMap<>();

    private static String getToken() {
        try {
            return Etched.CLIENT_CONFIG.yandexMusicToken.get().trim();
        } catch (IllegalStateException e) {
            // The client config is not loaded on dedicated servers
            return "";
        }
    }

    private InputStream get(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy, boolean api) throws IOException {
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

            String token = getToken();
            if (api && !token.isEmpty()) {
                httpURLConnection.setRequestProperty("Authorization", "OAuth " + token);
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

    private JsonElement getApi(String path, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException, JsonParseException {
        try (InputStream stream = this.get(API + path, progressListener, proxy, true)) {
            JsonObject json = JsonParser.parseString(IOUtils.toString(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.has("error")) {
                throw new IOException("Yandex Music API error: " + json.get("error"));
            }
            return json.get("result");
        }
    }

    /**
     * Fetches the track, album or playlist the URL points to and returns the tracks in it.
     * The first element is <code>null</code> for single tracks or the album/playlist info otherwise.
     */
    private <T> T resolve(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy, Request<T> function) throws IOException, URISyntaxException, JsonParseException {
        URI uri = new URI(url);
        String path = uri.getPath() == null ? "" : uri.getPath();
        String base = uri.getScheme() + "://" + uri.getHost();

        Matcher matcher = TRACK_PATTERN.matcher(path);
        if (matcher.find()) {
            JsonArray result = GsonHelper.convertToJsonArray(this.getApi("/tracks/" + matcher.group(1), progressListener, proxy), "result");
            if (result.isEmpty()) {
                throw new IOException("Track not found");
            }
            return function.process(base, null, Collections.singletonList(result.get(0).getAsJsonObject()));
        }

        matcher = ALBUM_PATTERN.matcher(path);
        if (matcher.find()) {
            JsonObject album = GsonHelper.convertToJsonObject(this.getApi("/albums/" + matcher.group(1) + "/with-tracks", progressListener, proxy), "result");
            List<JsonObject> tracks = new ArrayList<>();
            for (JsonElement volume : GsonHelper.getAsJsonArray(album, "volumes", new JsonArray())) {
                for (JsonElement track : volume.getAsJsonArray()) {
                    tracks.add(track.getAsJsonObject());
                }
            }
            return function.process(base, album, tracks);
        }

        JsonElement playlistResult = null;
        matcher = USER_PLAYLIST_PATTERN.matcher(path);
        if (matcher.find()) {
            playlistResult = this.getApi("/users/" + matcher.group(1) + "/playlists/" + matcher.group(2), progressListener, proxy);
        } else {
            matcher = PLAYLIST_PATTERN.matcher(path);
            if (matcher.find()) {
                playlistResult = this.getApi("/playlist/" + matcher.group(1), progressListener, proxy);
            }
        }
        if (playlistResult != null) {
            JsonObject playlist = GsonHelper.convertToJsonObject(playlistResult, "result");
            List<JsonObject> tracks = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(playlist, "tracks", new JsonArray())) {
                JsonObject entry = element.getAsJsonObject();
                if (entry.has("track")) {
                    tracks.add(GsonHelper.getAsJsonObject(entry, "track"));
                }
            }
            return function.process(base, playlist, tracks);
        }

        throw new IOException("URL is not a track, album or playlist");
    }

    private static String getArtist(JsonObject json) {
        StringJoiner joiner = new StringJoiner(", ");
        for (JsonElement artist : GsonHelper.getAsJsonArray(json, "artists", new JsonArray())) {
            JsonObject artistJson = artist.getAsJsonObject();
            if (artistJson.has("name")) {
                joiner.add(GsonHelper.getAsString(artistJson, "name"));
            }
        }
        return joiner.length() > 0 ? joiner.toString() : TrackData.EMPTY.artist();
    }

    private static String getTitle(JsonObject json) {
        String title = GsonHelper.getAsString(json, "title");
        String version = GsonHelper.getAsString(json, "version", "");
        return version.isBlank() ? title : title + " (" + version + ")";
    }

    private static String getTrackUrl(String base, JsonObject track) {
        String id = GsonHelper.getAsString(track, "id");
        JsonArray albums = GsonHelper.getAsJsonArray(track, "albums", new JsonArray());
        if (!albums.isEmpty()) {
            return base + "/album/" + albums.get(0).getAsJsonObject().get("id").getAsString() + "/track/" + id;
        }
        return base + "/track/" + id;
    }

    @Nullable
    private static String getCoverUrl(@Nullable String coverUri) {
        if (coverUri == null || coverUri.isBlank()) {
            return null;
        }
        return "https://" + coverUri.replace("%%", "400x400");
    }

    private URL resolveDownloadUrl(String trackId, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException, URISyntaxException {
        JsonArray infos = GsonHelper.convertToJsonArray(this.getApi("/tracks/" + trackId + "/download-info", progressListener, proxy), "result");

        JsonObject best = null;
        for (JsonElement element : infos) {
            JsonObject info = element.getAsJsonObject();
            if (!"mp3".equals(GsonHelper.getAsString(info, "codec", ""))) {
                continue;
            }
            if (best == null || GsonHelper.getAsInt(info, "bitrateInKbps", 0) > GsonHelper.getAsInt(best, "bitrateInKbps", 0)) {
                best = info;
            }
        }
        if (best == null) {
            throw new IOException("No MP3 stream available for track " + trackId);
        }

        String xml;
        try (InputStream stream = this.get(GsonHelper.getAsString(best, "downloadInfoUrl"), progressListener, proxy, false)) {
            xml = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        String host = getXmlValue(xml, "host");
        String path = getXmlValue(xml, "path");
        String ts = getXmlValue(xml, "ts");
        String s = getXmlValue(xml, "s");
        String sign = DigestUtils.md5Hex(SIGN_SALT + path.substring(1) + s);
        return new URI("https://" + host + "/get-mp3/" + sign + "/" + ts + path).toURL();
    }

    private static String getXmlValue(String xml, String tag) throws IOException {
        Matcher matcher = Pattern.compile("<" + tag + ">([^<]*)</" + tag + ">").matcher(xml);
        if (!matcher.find()) {
            throw new IOException("Missing '" + tag + "' in download info");
        }
        return matcher.group(1);
    }

    @Override
    public Collection<URL> resolveUrl(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException, URISyntaxException, JsonParseException {
        return this.resolve(url, progressListener, proxy, (base, container, tracks) -> {
            if (progressListener != null) {
                progressListener.progressStartRequest(RESOLVING_TRACKS);
            }
            List<URL> trackUrls = new ArrayList<>(tracks.size());
            for (JsonObject track : tracks) {
                if (!GsonHelper.getAsBoolean(track, "available", true)) {
                    continue;
                }
                try {
                    trackUrls.add(this.resolveDownloadUrl(GsonHelper.getAsString(track, "id"), progressListener, proxy));
                } catch (IOException e) {
                    if (container == null) {
                        throw e;
                    }
                    LOGGER.warn("Skipping unavailable Yandex Music track {}", track.get("id"), e);
                }
            }
            return trackUrls;
        });
    }

    @Override
    public Collection<TrackData> resolveTracks(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException, URISyntaxException, JsonParseException {
        return this.resolve(url, progressListener, proxy, (base, container, tracks) -> {
            if (container == null) {
                JsonObject track = tracks.getFirst();
                return Collections.singleton(new TrackData(url, getArtist(track), Component.literal(getTitle(track))));
            }

            List<TrackData> result = new ArrayList<>(tracks.size() + 1);
            if (container.has("owner")) {
                JsonObject owner = GsonHelper.getAsJsonObject(container, "owner");
                String ownerName = GsonHelper.getAsString(owner, "name", GsonHelper.getAsString(owner, "login", TrackData.EMPTY.artist()));
                result.add(new TrackData(url, ownerName, Component.literal(GsonHelper.getAsString(container, "title"))));
            } else {
                result.add(new TrackData(url, getArtist(container), Component.literal(getTitle(container))));
            }
            for (JsonObject track : tracks) {
                if (GsonHelper.getAsBoolean(track, "available", true)) {
                    result.add(new TrackData(getTrackUrl(base, track), getArtist(track), Component.literal(getTitle(track))));
                }
            }
            return result;
        });
    }

    @Override
    public Optional<String> resolveAlbumCover(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy, ResourceManager resourceManager) throws IOException, URISyntaxException, JsonParseException {
        return this.resolve(url, progressListener, proxy, (base, container, tracks) -> {
            if (container != null) {
                if (container.has("cover") && container.get("cover").isJsonObject()) {
                    String cover = getCoverUrl(GsonHelper.getAsString(GsonHelper.getAsJsonObject(container, "cover"), "uri", null));
                    if (cover != null) {
                        return Optional.of(cover);
                    }
                }
                String cover = getCoverUrl(GsonHelper.getAsString(container, "coverUri", GsonHelper.getAsString(container, "ogImage", null)));
                if (cover != null) {
                    return Optional.of(cover);
                }
            }
            if (tracks.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(getCoverUrl(GsonHelper.getAsString(tracks.getFirst(), "coverUri", null)));
        });
    }

    @Override
    public boolean isValidUrl(String url) {
        return this.validCache.computeIfAbsent(url, key -> {
            try {
                String host = new URI(key).getHost();
                return host != null && HOST_PATTERN.matcher(host).matches();
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
        return "Yandex Music";
    }

    @Override
    public Optional<Component> getBrandText(String url) {
        return Optional.of(BRAND);
    }

    @FunctionalInterface
    private interface Request<T> {

        T process(String base, @Nullable JsonObject container, List<JsonObject> tracks) throws IOException, JsonParseException, URISyntaxException;
    }
}
