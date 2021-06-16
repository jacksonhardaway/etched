package me.jaackson.etched.client.sound.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.GsonHelper;
import org.apache.commons.io.IOUtils;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.EofSensorInputStream;
import org.apache.http.conn.EofSensorWatcher;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * <p>Manages requests made to the SoundCloud API for tracks.</p>
 *
 * @author Ocelot
 */
public class SoundCloud {

    private static final String CLIENT_ID = "Gef7Kyef9qUHLjDFrmLfJTGqXRS9QT3l";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";

    private static InputStream get(String url, @Nullable DownloadProgressListener progressListener) throws IOException {
        if (progressListener != null)
            progressListener.progressStartRequest(new TranslatableComponent("sound_cloud.requesting"));
        HttpGet get = new HttpGet(url);
        CloseableHttpClient client = HttpClients.custom().setUserAgent("Minecraft Java/" + SharedConstants.getCurrentVersion().getName()).build();
        CloseableHttpResponse response = client.execute(get);
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() != 200) {
            IOUtils.closeQuietly(client, response);
            throw new IOException(statusLine.getStatusCode() + " " + statusLine.getReasonPhrase());
        }
        float size = (float) response.getEntity().getContentLength() / 1000.0F / 1000.0F;
        if (progressListener != null && size > 0)
            progressListener.progressStartDownload(size);

        return new EofSensorInputStream(response.getEntity().getContent(), new EofSensorWatcher() {
            @Override
            public boolean eofDetected(InputStream wrapped) {
                return true;
            }

            @Override
            public boolean streamClosed(InputStream wrapped) throws IOException {
                response.close();
                return true;
            }

            @Override
            public boolean streamAbort(InputStream wrapped) throws IOException {
                response.close();
                return true;
            }
        });
    }

    private static <T> T resolve(String url, @Nullable DownloadProgressListener progressListener, Request<T> function) throws IOException, JsonParseException {
        try (InputStreamReader reader = new InputStreamReader(get("https://api-v2.soundcloud.com/resolve?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8.toString()) + "&client_id=" + CLIENT_ID, progressListener))) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();

            if (!"track".equals(GsonHelper.getAsString(json, "kind")))
                throw new IOException("URL is not a track");
            if (!GsonHelper.getAsBoolean(json, "streamable"))
                throw new IOException("URL is not streamable");

            return function.process(json);
        }
    }

    /**
     * Resolves the streaming URL for the specified track.
     *
     * @param trackUrl The URL to the track
     * @return The URL to the audio file
     * @throws IOException        If any error occurs with requests
     * @throws JsonParseException If any error occurs when parsing
     */
    public static String resolveUrl(String trackUrl, @Nullable DownloadProgressListener progressListener) throws IOException {
        return resolve(trackUrl, progressListener, json -> {
            JsonArray media = GsonHelper.getAsJsonArray(GsonHelper.getAsJsonObject(json, "media"), "transcodings");
            for (int i = 0; i < media.size(); i++) {
                JsonObject transcodingJson = GsonHelper.convertToJsonObject(media.get(i), "transcodings[" + i + "]");

                JsonObject format = transcodingJson.getAsJsonObject("format");
                if ("progressive".equals(format.get("protocol").getAsString())) {
                    try (InputStreamReader reader = new InputStreamReader(get(GsonHelper.getAsString(transcodingJson, "url") + "?client_id=" + CLIENT_ID, progressListener))) {
                        return GsonHelper.getAsString(new JsonParser().parse(reader).getAsJsonObject(), "url");
                    }
                }
            }
            throw new IOException("Could not find an audio source");
        });
    }

    /**
     * Resolves the artist and title for the specified track.
     *
     * @param trackUrl The URL to the track
     * @return The artist and title in a pair
     * @throws IOException        If any error occurs with requests
     * @throws JsonParseException If any error occurs when parsing
     */
    public static Pair<String, String> resolveTrack(String trackUrl, @Nullable DownloadProgressListener progressListener) throws IOException, JsonParseException {
        return resolve(trackUrl, progressListener, json -> {
            JsonObject user = GsonHelper.getAsJsonObject(json, "user");
            String artist = GsonHelper.getAsString(user, "username");
            String title = GsonHelper.getAsString(json, "title");
            return Pair.of(artist, title);
        });
    }

    /**
     * Checks to see if the specified URL is for sound cloud.
     *
     * @param url The URL to go to
     * @return Whether or not that URL is valid
     */
    public static boolean isValidUrl(String url) {
        return url.contains("soundcloud.com");
    }

    @FunctionalInterface
    private interface Request<T> {

        T process(JsonObject json) throws IOException, JsonParseException;
    }
}
