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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Manages requests made to the SoundCloud API for tracks.</p>
 *
 * @author Ocelot
 */
public class SoundCloud {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CLIENT_ID = "Gef7Kyef9qUHLjDFrmLfJTGqXRS9QT3l";

    private static Map<String, String> getDownloadHeaders() {
        Map<String, String> map = new HashMap<>();
        map.put("X-Minecraft-Version", SharedConstants.getCurrentVersion().getName());
        map.put("X-Minecraft-Version-ID", SharedConstants.getCurrentVersion().getId());
        map.put("User-Agent", "Minecraft Java/" + SharedConstants.getCurrentVersion().getName());
        return map;
    }

    private static InputStream get(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException {
        HttpURLConnection httpURLConnection = null;
        if (progressListener != null)
            progressListener.progressStartRequest(new TranslatableComponent("sound_cloud.requesting"));

        try {
            URL uRL = new URL(url);
            httpURLConnection = (HttpURLConnection) uRL.openConnection(proxy);
            httpURLConnection.setInstanceFollowRedirects(true);
            float f = 0.0F;
            Map<String, String> map = getDownloadHeaders();
            float g = (float) map.entrySet().size();

            for (Map.Entry<String, String> entry : map.entrySet()) {
                httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
                if (progressListener != null)
                    progressListener.progressStagePercentage((int) (++f / g * 100.0F));
            }

            if (httpURLConnection.getResponseCode() != 200) {
                IOUtils.closeQuietly(httpURLConnection.getInputStream());
                throw new IOException(httpURLConnection.getResponseCode() + " " + httpURLConnection.getResponseMessage());
            }

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
        } finally {
            if (httpURLConnection != null)
                IOUtils.closeQuietly(httpURLConnection.getErrorStream());
        }
    }

//    private static InputStream get(String url, @Nullable DownloadProgressListener progressListener) throws IOException {
//        if (progressListener != null)
//            progressListener.progressStartRequest(new TranslatableComponent("sound_cloud.requesting"));
//        HttpGet get = new HttpGet(url);
//        CloseableHttpClient client = HttpClients.custom().setUserAgent("Minecraft Java/" + SharedConstants.getCurrentVersion().getName()).build();
//        CloseableHttpResponse response = client.execute(get);
//        StatusLine statusLine = response.getStatusLine();
//        if (statusLine.getStatusCode() != 200) {
//            IOUtils.closeQuietly(client, response);
//            throw new IOException(statusLine.getStatusCode() + " " + statusLine.getReasonPhrase());
//        }
//        float size = (float) response.getEntity().getContentLength() / 1000.0F / 1000.0F;
//        if (progressListener != null && size > 0)
//            progressListener.progressStartDownload(size);
//
//        return new EofSensorInputStream(response.getEntity().getContent(), new EofSensorWatcher() {
//            @Override
//            public boolean eofDetected(InputStream wrapped) {
//                return true;
//            }
//
//            @Override
//            public boolean streamClosed(InputStream wrapped) throws IOException {
//                response.close();
//                return true;
//            }
//
//            @Override
//            public boolean streamAbort(InputStream wrapped) throws IOException {
//                response.close();
//                return true;
//            }
//        });
//    }

    private static <T> T resolve(String url, @Nullable DownloadProgressListener progressListener, Proxy proxy, Request<T> function) throws IOException, JsonParseException {
        try (InputStreamReader reader = new InputStreamReader(get("https://api-v2.soundcloud.com/resolve?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8.toString()) + "&client_id=" + CLIENT_ID, progressListener, proxy))) {
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
     * @param trackUrl         The URL to the track
     * @param progressListener The listener for net status
     * @param proxy            The internet proxy
     * @return The URL to the audio file
     * @throws IOException        If any error occurs with requests
     * @throws JsonParseException If any error occurs when parsing
     */
    public static String resolveUrl(String trackUrl, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException {
        return resolve(trackUrl, progressListener, proxy, json -> {
            JsonArray media = GsonHelper.getAsJsonArray(GsonHelper.getAsJsonObject(json, "media"), "transcodings");
            for (int i = 0; i < media.size(); i++) {
                JsonObject transcodingJson = GsonHelper.convertToJsonObject(media.get(i), "transcodings[" + i + "]");

                JsonObject format = transcodingJson.getAsJsonObject("format");
                if ("progressive".equals(format.get("protocol").getAsString())) {
                    try (InputStreamReader reader = new InputStreamReader(get(GsonHelper.getAsString(transcodingJson, "url") + "?client_id=" + CLIENT_ID, progressListener, proxy))) {
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
     * @param trackUrl         The URL to the track
     * @param progressListener The listener for net status
     * @param proxy            The internet proxy
     * @return The artist and title in a pair
     * @throws IOException        If any error occurs with requests
     * @throws JsonParseException If any error occurs when parsing
     */
    public static Pair<String, String> resolveTrack(String trackUrl, @Nullable DownloadProgressListener progressListener, Proxy proxy) throws IOException, JsonParseException {
        return resolve(trackUrl, progressListener, proxy, json -> {
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
        try {
            String host = new URI(url).getHost();
            return "soundcloud.com".equals(host);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    @FunctionalInterface
    private interface Request<T> {

        T process(JsonObject json) throws IOException, JsonParseException;
    }
}
