package gg.moonflower.etched.client.sound.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import gg.moonflower.etched.api.sound.download.SoundDownloadSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.api.util.ProgressTrackingInputStream;
import gg.moonflower.etched.core.Etched;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ocelot
 */
public class BandCampSource implements SoundDownloadSource {

    private static final Pattern DATA_PATTERN = Pattern.compile("data-tralbum=\"([^\"]+)\"");
    private static final Component BRAND = new TranslatableComponent("sound_source." + Etched.MOD_ID + ".band_camp").withStyle(style -> style.withColor(TextColor.fromRgb(0x477987)));

    private final Map<String, Boolean> validCache = new WeakHashMap<>();

    private InputStream get(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException {
        HttpURLConnection httpURLConnection;
        if (progressListener != null)
            progressListener.progressStartRequest(new TranslatableComponent("sound_source." + Etched.MOD_ID + ".requesting", this.getApiName()));

        try {
            URL uRL = new URL(url);
            httpURLConnection = (HttpURLConnection) uRL.openConnection(proxy);
            httpURLConnection.setInstanceFollowRedirects(true);
            Map<String, String> map = SoundDownloadSource.getDownloadHeaders();

            for (Map.Entry<String, String> entry : map.entrySet())
                httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());

            long size = httpURLConnection.getContentLengthLong();
            int response = httpURLConnection.getResponseCode();
            if (response != 200)
                throw new IOException(response + " " + httpURLConnection.getResponseMessage());

            return progressListener != null && size != -1 ? new ProgressTrackingInputStream(httpURLConnection.getInputStream(), size, progressListener) : httpURLConnection.getInputStream();
        } catch (IOException e) {
            throw e;
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    private <T> T resolve(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy, SourceRequest<T> function) throws IOException, JsonParseException {
        try (InputStream stream = get(url, progressListener, proxy)) {
            Matcher dataMatcher = DATA_PATTERN.matcher(IOUtils.toString(stream, StandardCharsets.UTF_8));
            String raw = dataMatcher.find() ? dataMatcher.group(1) : null;
            if (raw == null)
                throw new IOException("Failed to find properties");

            JsonObject json = new JsonParser().parse(raw.replaceAll("&quot;", "\"")).getAsJsonObject();
            String type = GsonHelper.getAsString(GsonHelper.getAsJsonObject(json, "current"), "type");
            if (!"track".equals(type) && !"album".equals(type))
                throw new IOException("URL is not a track or album");

            return function.process(json);
        }
    }

    @Override
    public List<URL> resolveUrl(String trackUrl, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException {
        return resolve(trackUrl, progressListener, proxy, json -> {
            if (progressListener != null)
                progressListener.progressStartRequest(RESOLVING_TRACKS);
            JsonArray trackInfoArray = GsonHelper.getAsJsonArray(json, "trackinfo");
            List<URL> trackUrls = new ArrayList<>(trackInfoArray.size());
            for (int i = 0; i < trackInfoArray.size(); i++) {
                JsonObject trackInfoJson = GsonHelper.convertToJsonObject(trackInfoArray.get(i), "trackinfo[" + i + "]");
                JsonObject fileJson = GsonHelper.getAsJsonObject(trackInfoJson, "file");
                if (fileJson.has("mp3-128"))
                    trackUrls.add(new URL(GsonHelper.getAsString(fileJson, "mp3-128")));
            }
            return trackUrls;
        });
    }

    @Override
    public Optional<TrackData> resolveTrack(String trackUrl, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException, JsonParseException {
        return Optional.of(resolve(trackUrl, progressListener, proxy, json -> {
            JsonObject current = GsonHelper.getAsJsonObject(json, "current");
            String artist = GsonHelper.getAsString(json, "artist");
            String title = GsonHelper.getAsString(current, "title");
            String type = GsonHelper.getAsString(GsonHelper.getAsJsonObject(json, "current"), "type");
            return new TrackData(artist, title, "album".equals(type));
        }));
    }

    @Override
    public Optional<InputStream> resolveAlbumCover(String trackUrl, @Nullable DownloadProgressListener progressListener, Proxy proxy, ResourceManager resourceManager) throws IOException {
        return Optional.empty();
    }

    @Override
    public boolean isValidUrl(String url) {
        this.validCache.clear();
        return this.validCache.computeIfAbsent(url, key -> {
            try {
                String host = new URI(key).getHost();
                return host != null && host.endsWith("bandcamp.com");
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
        return "BandCamp";
    }

    @Override
    public Optional<Component> getBrandText(String url) {
        return Optional.of(BRAND);
    }
}
