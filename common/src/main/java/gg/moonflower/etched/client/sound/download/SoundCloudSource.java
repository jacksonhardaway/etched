package gg.moonflower.etched.client.sound.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import gg.moonflower.etched.api.sound.download.SoundDownloadSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.api.util.M3uParser;
import gg.moonflower.etched.api.util.ProgressTrackingInputStream;
import gg.moonflower.etched.core.Etched;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.apache.commons.io.IOUtils;
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
    private static final Component BRAND = new TranslatableComponent("sound_source." + Etched.MOD_ID + ".sound_cloud").withStyle(style -> style.withColor(TextColor.fromRgb(0xFF5500)));

    private final Map<String, Boolean> validCache = new WeakHashMap<>();

    private InputStream get(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy, int attempt, boolean requiresId) throws IOException {
        HttpURLConnection httpURLConnection = null;
        if (progressListener != null)
            progressListener.progressStartRequest(new TranslatableComponent("sound_source." + Etched.MOD_ID + ".requesting", this.getApiName()));

        try {
            URL uRL = requiresId ? new URL(url + SoundCloudIdTracker.fetch(proxy)) : new URL(url);
            httpURLConnection = (HttpURLConnection) uRL.openConnection(proxy);
            httpURLConnection.setInstanceFollowRedirects(true);
            Map<String, String> map = SoundDownloadSource.getDownloadHeaders();

            for (Map.Entry<String, String> entry : map.entrySet())
                httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());

            int response = httpURLConnection.getResponseCode();
            if (attempt == 0 && (response == 401 || response == 403)) {
                LOGGER.info("Attempting to authenticate");
                SoundCloudIdTracker.invalidate();
                return get(url, progressListener, proxy, 1, requiresId);
            }

            long size = httpURLConnection.getContentLengthLong();
            if (response != 200)
                throw new IOException(response + " " + httpURLConnection.getResponseMessage());

            return progressListener != null && size != -1 ? new ProgressTrackingInputStream(httpURLConnection.getInputStream(), size, progressListener) : httpURLConnection.getInputStream();
        } catch (Throwable e) {
            if (httpURLConnection != null) {
                try {
                    LOGGER.error(IOUtils.toString(httpURLConnection.getErrorStream(), StandardCharsets.UTF_8));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            throw new IOException(e);
        }
    }

    private <T> T resolve(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy, SourceRequest<T> function) throws IOException, JsonParseException {
        try (InputStreamReader reader = new InputStreamReader(get("https://api-v2.soundcloud.com/resolve?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8.toString()) + "&client_id=", progressListener, proxy, 0, true))) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();

            String kind = GsonHelper.getAsString(json, "kind");
            if (!"track".equals(kind) && !"playlist".equals(kind))
                throw new IOException("URL is not a track or album");
            if ("track".equals(kind) && !GsonHelper.getAsBoolean(json, "streamable"))
                throw new IOException("URL is not streamable");
            if ("playlist".equals(kind) && !GsonHelper.getAsBoolean(json, "is_album"))
                throw new IOException("URL is not a track or album");

            return function.process(json);
        }
    }

    private List<URL> resolveTrackUrl(JsonObject json, Proxy proxy) throws IOException {
        JsonArray media = GsonHelper.getAsJsonArray(GsonHelper.getAsJsonObject(json, "media"), "transcodings");

        int progressiveIndex = -1;
        for (int i = 0; i < media.size(); i++) {
            JsonObject transcodingJson = GsonHelper.convertToJsonObject(media.get(i), "transcodings[" + i + "]");

            JsonObject format = transcodingJson.getAsJsonObject("format");
            String protocol = format.get("protocol").getAsString();
            if ("progressive".equals(protocol))
                progressiveIndex = i;
            if ("hls".equals(protocol)) {
                try (InputStreamReader r = new InputStreamReader(get(GsonHelper.getAsString(transcodingJson, "url") + "?client_id=", null, proxy, 0, true))) {
                    try (InputStreamReader reader = new InputStreamReader(get(GsonHelper.getAsString(new JsonParser().parse(r).getAsJsonObject(), "url"), null, proxy, 0, false))) {
                        return M3uParser.parse(reader);
                    }
                }
            }
        }
        if (progressiveIndex == -1)
            throw new IOException("Could not find an audio source");
        try (InputStreamReader reader = new InputStreamReader(get(GsonHelper.getAsString(GsonHelper.convertToJsonObject(media.get(progressiveIndex), "transcodings[" + progressiveIndex + "]"), "url") + "?client_id=", null, proxy, 0, true))) {
            return Collections.singletonList(new URL(GsonHelper.getAsString(new JsonParser().parse(reader).getAsJsonObject(), "url")));
        }
    }

    @Override
    public List<URL> resolveUrl(String trackUrl, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException {
        return resolve(trackUrl, progressListener, proxy, json -> {
            if (progressListener != null)
                progressListener.progressStartRequest(RESOLVING_TRACKS);
            String kind = GsonHelper.getAsString(json, "kind");
            if ("playlist".equals(kind)) {
                JsonArray tracksJson = GsonHelper.getAsJsonArray(json, "tracks");
                List<URL> urls = new ArrayList<>();

                for (int i = 0; i < tracksJson.size(); i++) {
                    try {
                        urls.addAll(resolveTrackUrl(GsonHelper.convertToJsonObject(tracksJson.get(i), "tracks[" + i + "]"), proxy));
                    } catch (Exception ignored) {
                    }
                }
                if (urls.isEmpty())
                    throw new IOException("Failed to retrieve tracks in album");
                return urls;
            }
            return resolveTrackUrl(json, proxy);
        });
    }

    @Override
    public Optional<TrackData> resolveTrack(String trackUrl, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException, JsonParseException {
        return Optional.of(resolve(trackUrl, progressListener, proxy, json -> {
            JsonObject user = GsonHelper.getAsJsonObject(json, "user");
            String artist = GsonHelper.getAsString(user, "username");
            String title = GsonHelper.getAsString(json, "title");
            String kind = GsonHelper.getAsString(json, "kind");
            return new TrackData(artist, title, "playlist".equals(kind));
        }));
    }

    @Override
    public Optional<InputStream> resolveAlbumCover(String trackUrl, @Nullable DownloadProgressListener progressListener, Proxy proxy, ResourceManager resourceManager) throws IOException {
        return resolve(trackUrl, progressListener, proxy, json -> {
            if (!"playlist".equals(GsonHelper.getAsString(json, "kind")))
                return Optional.empty();
            if (!json.has("artwork_url") || json.get("artwork__url").isJsonNull())
                return Optional.of(resourceManager.getResource(DEFAULT_ART).getInputStream());
            return Optional.of(get(GsonHelper.getAsString(json, "artwork_url"), progressListener, proxy, 0, false));
        });
    }

    @Override
    public boolean isValidUrl(String url) {
        return this.validCache.computeIfAbsent(url, key -> {
            try {
                String host = new URI(key).getHost();
                return "soundcloud.com".equals(host);
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
