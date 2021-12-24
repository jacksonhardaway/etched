package gg.moonflower.etched.client.sound.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import gg.moonflower.etched.api.sound.download.SoundDownloadSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.api.util.M3uParser;
import gg.moonflower.etched.core.Etched;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TranslatableComponent;
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
            float f = 0.0F;
            Map<String, String> map = SoundDownloadSource.getDownloadHeaders();
            float g = (float) map.entrySet().size();

            for (Map.Entry<String, String> entry : map.entrySet()) {
                httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
                if (progressListener != null)
                    progressListener.progressStagePercentage((int) (++f / g * 100.0F));
            }

            int response = httpURLConnection.getResponseCode();
            if (attempt == 0 && (response == 401 || response == 403)) {
                LOGGER.info("Attempting to authenticate");
                SoundCloudIdTracker.invalidate();
                return get(url, progressListener, proxy, 1, requiresId);
            }

            if (response != 200)
                throw new IOException(response + " " + httpURLConnection.getResponseMessage());

            return httpURLConnection.getInputStream();
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

    private <T> T resolve(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy, Request<T> function) throws IOException, JsonParseException {
        try (InputStreamReader reader = new InputStreamReader(get("https://api-v2.soundcloud.com/resolve?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8.toString()) + "&client_id=", progressListener, proxy, 0, true))) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            if (!"track".equals(GsonHelper.getAsString(json, "kind")))
                throw new IOException("URL is not a track");
            if (!GsonHelper.getAsBoolean(json, "streamable"))
                throw new IOException("URL is not streamable");

            return function.process(json);
        }
    }

    @Override
    public List<URL> resolveUrl(String trackUrl, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException {
        return resolve(trackUrl, progressListener, proxy, json -> {
            JsonArray media = GsonHelper.getAsJsonArray(GsonHelper.getAsJsonObject(json, "media"), "transcodings");

            int progressiveIndex = -1;
            for (int i = 0; i < media.size(); i++) {
                JsonObject transcodingJson = GsonHelper.convertToJsonObject(media.get(i), "transcodings[" + i + "]");

                JsonObject format = transcodingJson.getAsJsonObject("format");
                String protocol = format.get("protocol").getAsString();
                if ("progressive".equals(protocol))
                    progressiveIndex = i;
                if ("hls".equals(protocol)) {
                    try (InputStreamReader r = new InputStreamReader(get(GsonHelper.getAsString(transcodingJson, "url") + "?client_id=", progressListener, proxy, 0, true))) {
                        try (InputStreamReader reader = new InputStreamReader(get(GsonHelper.getAsString(JsonParser.parseReader(r).getAsJsonObject(), "url"), progressListener, proxy, 0, false))) {
                            return M3uParser.parse(reader);
                        }
                    }
                }
            }
            if (progressiveIndex == -1)
                throw new IOException("Could not find an audio source");
            try (InputStreamReader reader = new InputStreamReader(get(GsonHelper.getAsString(GsonHelper.convertToJsonObject(media.get(progressiveIndex), "transcodings[" + progressiveIndex + "]"), "url") + "?client_id=", progressListener, proxy, 0, true))) {
                return Collections.singletonList(new URL(GsonHelper.getAsString(JsonParser.parseReader(reader).getAsJsonObject(), "url")));
            }
        });
    }

    @Override
    public Optional<Pair<String, String>> resolveTrack(String trackUrl, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException, JsonParseException {
        return Optional.of(resolve(trackUrl, progressListener, proxy, json -> {
            JsonObject user = GsonHelper.getAsJsonObject(json, "user");
            String artist = GsonHelper.getAsString(user, "username");
            String title = GsonHelper.getAsString(json, "title");
            return Pair.of(artist, title);
        }));
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

    @FunctionalInterface
    private interface Request<T> {

        T process(JsonObject json) throws IOException, JsonParseException;
    }
}
