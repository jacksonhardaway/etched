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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the MP3 files of an Internet Archive item. A link to a single file in the item only plays that file.
 */
public class ArchiveOrgSource implements SoundDownloadSource {

    private static final Pattern DETAILS_PATTERN = Pattern.compile("^/details/([^/]+)(?:/(.+))?$");
    // The best quality MP3 derivative is used when an item has more than one
    private static final List<String> FORMATS = List.of("VBR MP3", "320Kbps MP3", "256Kbps MP3", "192Kbps MP3", "160Kbps MP3", "128Kbps MP3", "MP3", "64Kbps MP3");
    private static final Component BRAND = Component.translatable("sound_source." + Etched.MOD_ID + ".archive_org").withStyle(style -> style.withColor(TextColor.fromRgb(0x428BCA)));

    private final Map<String, Boolean> validCache = new WeakHashMap<>();

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
        Matcher matcher = DETAILS_PATTERN.matcher(new URI(url).getPath());
        if (!matcher.find()) {
            throw new IOException("URL is not an Internet Archive item");
        }
        String identifier = matcher.group(1);
        String fileName = matcher.group(2);

        JsonObject json;
        try (InputStream stream = this.get("https://archive.org/metadata/" + URLEncoder.encode(identifier, StandardCharsets.UTF_8), progressListener, proxy)) {
            json = JsonParser.parseString(IOUtils.toString(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        if (!json.has("metadata")) {
            throw new IOException("Internet Archive item not found");
        }

        List<JsonObject> files = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "files", new JsonArray())) {
            files.add(element.getAsJsonObject());
        }

        List<JsonObject> tracks = new ArrayList<>();
        if (fileName != null) {
            for (JsonObject file : files) {
                String name = GsonHelper.getAsString(file, "name", "");
                if (name.equals(fileName) || name.equals(fileName.replace('+', ' '))) {
                    tracks.add(file);
                    break;
                }
            }
        } else {
            for (String format : FORMATS) {
                for (JsonObject file : files) {
                    if (format.equals(GsonHelper.getAsString(file, "format", ""))) {
                        tracks.add(file);
                    }
                }
                if (!tracks.isEmpty()) {
                    break;
                }
            }
            tracks.sort(Comparator.comparing(file -> GsonHelper.getAsString(file, "name")));
        }
        if (tracks.isEmpty()) {
            throw new IOException("No MP3 files found in Internet Archive item");
        }

        return function.process(identifier, GsonHelper.getAsJsonObject(json, "metadata"), tracks);
    }

    private static String getString(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (element.isJsonArray()) {
            StringJoiner joiner = new StringJoiner(", ");
            element.getAsJsonArray().forEach(value -> joiner.add(value.getAsString()));
            return joiner.toString();
        }
        return element.getAsString();
    }

    private static String getFileUrl(String identifier, JsonObject file) {
        String name = URLEncoder.encode(GsonHelper.getAsString(file, "name"), StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
        return "https://archive.org/download/" + identifier + "/" + name;
    }

    private static String getTitle(JsonObject file) {
        String title = getString(file, "title", "");
        if (!title.isBlank()) {
            return title;
        }
        String name = GsonHelper.getAsString(file, "name");
        name = name.substring(name.lastIndexOf('/') + 1);
        int extension = name.lastIndexOf('.');
        return extension > 0 ? name.substring(0, extension) : name;
    }

    @Override
    public Collection<URL> resolveUrl(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException, URISyntaxException, JsonParseException {
        return this.resolve(url, progressListener, proxy, (identifier, metadata, tracks) -> {
            if (progressListener != null) {
                progressListener.progressStartRequest(RESOLVING_TRACKS);
            }
            List<URL> trackUrls = new ArrayList<>(tracks.size());
            for (JsonObject track : tracks) {
                trackUrls.add(new URI(getFileUrl(identifier, track)).toURL());
            }
            return trackUrls;
        });
    }

    @Override
    public Collection<TrackData> resolveTracks(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException, URISyntaxException, JsonParseException {
        return this.resolve(url, progressListener, proxy, (identifier, metadata, tracks) -> {
            String artist = getString(metadata, "creator", TrackData.EMPTY.artist());
            if (tracks.size() == 1) {
                JsonObject track = tracks.getFirst();
                return Collections.singleton(new TrackData(url, getString(track, "creator", artist), Component.literal(getTitle(track))));
            }

            List<TrackData> result = new ArrayList<>(tracks.size() + 1);
            result.add(new TrackData(url, artist, Component.literal(getString(metadata, "title", identifier))));
            for (JsonObject track : tracks) {
                String trackUrl = "https://archive.org/details/" + identifier + "/" + URLEncoder.encode(GsonHelper.getAsString(track, "name"), StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
                result.add(new TrackData(trackUrl, getString(track, "creator", artist), Component.literal(getTitle(track))));
            }
            return result;
        });
    }

    @Override
    public Optional<String> resolveAlbumCover(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy, ResourceManager resourceManager) throws IOException, URISyntaxException, JsonParseException {
        Matcher matcher = DETAILS_PATTERN.matcher(new URI(url).getPath());
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of("https://archive.org/services/img/" + matcher.group(1));
    }

    @Override
    public boolean isValidUrl(String url) {
        return this.validCache.computeIfAbsent(url, key -> {
            try {
                URI uri = new URI(key);
                String host = uri.getHost();
                return host != null && (host.equals("archive.org") || host.equals("www.archive.org")) && uri.getPath() != null && DETAILS_PATTERN.matcher(uri.getPath()).find();
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
        return "Internet Archive";
    }

    @Override
    public Optional<Component> getBrandText(String url) {
        return Optional.of(BRAND);
    }

    @FunctionalInterface
    private interface Request<T> {

        T process(String identifier, JsonObject metadata, List<JsonObject> tracks) throws IOException, JsonParseException, URISyntaxException;
    }
}
