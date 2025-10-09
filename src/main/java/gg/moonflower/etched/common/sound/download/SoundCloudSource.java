package gg.moonflower.etched.common.sound.download;

import com.google.gson.*;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.download.SoundDownloadSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.api.util.M3uParser;
import gg.moonflower.etched.api.util.ProgressTrackingInputStream;
import gg.moonflower.etched.core.Etched;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author Ocelot
 */
public class SoundCloudSource implements SoundDownloadSource {

    static final Logger LOGGER = LogManager.getLogger();
    private static final Component BRAND = Component.translatable("sound_source." + Etched.MOD_ID + ".sound_cloud").withStyle(style -> style.withColor(TextColor.fromRgb(0xFF5500)));

    private final Map<String, Boolean> validCache = new WeakHashMap<>();

    private static URL appendUri(String uri, String appendQuery) throws Exception {
        URI oldUri = new URI(uri);
        return new URI(oldUri.getScheme(), oldUri.getAuthority(), oldUri.getPath(), oldUri.getQuery() == null ? appendQuery : oldUri.getQuery() + "&" + appendQuery, oldUri.getFragment()).toURL();
    }

    private InputStream get(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy, int attempt, boolean requiresId) throws IOException {
        HttpURLConnection httpURLConnection;
        if (progressListener != null) {
            progressListener.progressStartRequest(Component.translatable("sound_source." + Etched.MOD_ID + ".requesting", this.getApiName()));
        }

        try {
            URL uRL = requiresId ? appendUri(url, "client_id=" + SoundCloudIdTracker.fetch(proxy)) : new URI(url).toURL();
            httpURLConnection = (HttpURLConnection) uRL.openConnection(proxy);
            httpURLConnection.setInstanceFollowRedirects(true);
            Map<String, String> map = SoundDownloadSource.getDownloadHeaders();

            for (Map.Entry<String, String> entry : map.entrySet()) {
                httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            int response = httpURLConnection.getResponseCode();
            if (requiresId && attempt == 0 && (response == 401 || response == 403)) { // Authenticate if required and bad auth response
                LOGGER.info("Attempting to authenticate");
                SoundCloudIdTracker.invalidate();
                return this.get(url, progressListener, proxy, 1, true);
            }

            long size = httpURLConnection.getContentLengthLong();
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

    private <T> T resolve(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy, SourceRequest<T> function) throws IOException, URISyntaxException, JsonParseException {
        try (InputStreamReader reader = new InputStreamReader(this.get("https://api-v2.soundcloud.com/resolve?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8), progressListener, proxy, 0, true))) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            String kind = GsonHelper.getAsString(json, "kind");
            if (!"track".equals(kind) && !"playlist".equals(kind)) {
                throw new IOException("URL is not a track or album");
            }
            if ("track".equals(kind) && !GsonHelper.getAsBoolean(json, "streamable")) {
                throw new IOException("URL is not streamable");
            }
            if ("playlist".equals(kind) && !GsonHelper.getAsBoolean(json, "is_album")) {
                throw new IOException("URL is not a track or album");
            }

            return function.process(json);
        }
    }

    @Override
    public List<URL> resolveUrl(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException, URISyntaxException, JsonParseException {
        return this.resolve(url, progressListener, proxy, json -> {
            if (progressListener != null) {
                progressListener.progressStartRequest(RESOLVING_TRACKS);
            }
            JsonArray media = GsonHelper.getAsJsonArray(GsonHelper.getAsJsonObject(json, "media"), "transcodings");

            Map<Format, String> urls = new EnumMap<>(Format.class);
            for (int i = 0; i < media.size(); i++) {
                JsonObject transcodingJson = GsonHelper.convertToJsonObject(media.get(i), "transcodings[" + i + "]");

                Format format = Format.parse(transcodingJson.getAsJsonObject("format"));
                if (format == null) {
                    continue;
                }

                urls.put(format, GsonHelper.getAsString(transcodingJson, "url"));
            }

            for (Format format : Format.FORMATS) {
                String dataUrl = urls.get(format);
                if (dataUrl == null) {
                    continue;
                }

                try (InputStreamReader reader = new InputStreamReader(this.get(dataUrl, null, proxy, 0, true))) {
                    JsonObject urlJson = JsonParser.parseReader(reader).getAsJsonObject();
                    if (format.isHls()) {
                        try (InputStream stream = this.get(GsonHelper.getAsString(urlJson, "url"), null, proxy, 0, false)) {
                            return M3uParser.parse(stream);
                        }
                    } else {
                        return Collections.singletonList(new URI(GsonHelper.getAsString(urlJson, "url")).toURL());
                    }
                }
            }

            throw new IOException("Could not find an audio source");
        });
    }

    public enum Format {
        OGG_PROGRESSIVE, MP3_PROGRESSIVE, OGG_HLS, MP3_HLS;

        public boolean isHls() {
            return this == OGG_HLS || this == MP3_HLS;
        }

        private static final Format[] FORMATS = values();

        public static @Nullable Format parse(JsonObject format) {
            JsonElement type = format.get("mime_type");
            if (type == null || !type.isJsonPrimitive()) {
                return null;
            }

            boolean mp3;
            String typeString = type.getAsString().toLowerCase(Locale.ROOT);
            if (typeString.startsWith("audio/ogg")) {
//                mp3 = false;
                return null;
            } else if (typeString.startsWith("audio/mpeg")) {
                mp3 = true;
            } else {
                return null;
            }

            JsonElement protocol = format.get("protocol");
            if (protocol == null || !protocol.isJsonPrimitive()) {
                return null;
            }

            String protocolString = protocol.getAsString().toLowerCase(Locale.ROOT);
            return switch (protocolString) {
                case "hls" -> mp3 ? MP3_HLS : OGG_HLS;
                case "progressive" -> mp3 ? MP3_PROGRESSIVE : OGG_PROGRESSIVE;
                default -> null;
            };
        }
    }

    @Override
    public List<TrackData> resolveTracks(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException, URISyntaxException, JsonParseException {
        return this.resolve(url, progressListener, proxy, json -> {
            JsonObject user = GsonHelper.getAsJsonObject(json, "user");
            String artist = GsonHelper.getAsString(user, "username");
            String title = GsonHelper.getAsString(json, "title");
            String kind = GsonHelper.getAsString(json, "kind");
            if ("playlist".equals(kind)) {
                JsonArray tracksJson = GsonHelper.getAsJsonArray(json, "tracks");
                List<TrackData> tracks = new ArrayList<>();
                tracks.add(new TrackData(url, artist, Component.literal(title)));

                for (int i = 0; i < tracksJson.size(); i++) {
                    try {
                        JsonObject trackJson = GsonHelper.convertToJsonObject(tracksJson.get(i), "tracks[" + i + "]");
                        if (!trackJson.has("permalink_url")) { // Paid song
                            continue;
                        }
                        JsonObject trackUser = GsonHelper.getAsJsonObject(trackJson, "user", user);
                        String trackUrl = GsonHelper.getAsString(trackJson, "permalink_url");
                        String trackArtist = GsonHelper.getAsString(trackUser, "username");
                        String trackTitle = GsonHelper.getAsString(trackJson, "title");
                        tracks.add(new TrackData(trackUrl, trackArtist, Component.literal(trackTitle)));
                    } catch (JsonParseException e) {
                        LOGGER.error("Failed to parse track: {}[{}]", url, i, e);
                    }
                }

                return tracks;
            }

            return Collections.singletonList(new TrackData(url, artist, Component.literal(title)));
        });
    }

    @Override
    public Optional<String> resolveAlbumCover(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy, ResourceManager resourceManager) throws IOException, URISyntaxException, JsonParseException {
        return this.resolve(url, progressListener, proxy, json -> {
            if (!json.has("artwork_url") || json.get("artwork_url").isJsonNull()) {
                return Optional.empty();
            }
            return Optional.of(GsonHelper.getAsString(json, "artwork_url"));
        });
    }

    @Override
    public boolean isValidUrl(String url) {
        return this.validCache.computeIfAbsent(url, key -> {
            try {
                String host = new URI(key).getHost();
                return host != null && host.endsWith("soundcloud.com");
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
        return "SoundCloud";
    }

    @Override
    public Optional<Component> getBrandText(String url) {
        return Optional.of(BRAND);
    }
}
