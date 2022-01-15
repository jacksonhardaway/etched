package gg.moonflower.etched.client.sound.download;

import gg.moonflower.etched.api.sound.download.SoundDownloadSource;
import net.minecraft.Util;
import net.minecraft.util.HttpUtil;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ocelot
 */
public final class SoundCloudIdTracker {

    private static final Object LOCK = new Object();

    private static final Pattern PAGE_APP_SCRIPT_PATTERN = Pattern.compile("https://[A-Za-z0-9-.]+/assets/[a-f0-9-]+\\.js");
    private static final Pattern APP_SCRIPT_CLIENT_ID_PATTERN = Pattern.compile(",client_id:\"([a-zA-Z0-9-_]+)\"");

    private static volatile String currentId;
    private static volatile CompletableFuture<?> currentRequest;

    private SoundCloudIdTracker() {
    }

    private static String getLastMatchWithinLimit(Matcher m) {
        String lastMatch = null;
        for (int i = 0; m.find() && i < 9; i++)
            lastMatch = m.group();
        return lastMatch;
    }

    private static String findScriptUrl(Proxy proxy) throws IOException {
        HttpURLConnection httpURLConnection = null;

        try {
            URL uRL = new URL("https://soundcloud.com");
            httpURLConnection = (HttpURLConnection) uRL.openConnection(proxy);
            httpURLConnection.setInstanceFollowRedirects(true);
            Map<String, String> map = SoundDownloadSource.getDownloadHeaders();

            for (Map.Entry<String, String> entry : map.entrySet()) {
                httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            if (httpURLConnection.getResponseCode() != 200)
                throw new IOException(httpURLConnection.getResponseCode() + " " + httpURLConnection.getResponseMessage());

            String result = getLastMatchWithinLimit(PAGE_APP_SCRIPT_PATTERN.matcher(IOUtils.toString(httpURLConnection.getInputStream(), StandardCharsets.UTF_8)));
            if (result == null)
                throw new IllegalStateException("Could not find application script from main page.");

            return result;
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    @Nullable
    private static String findIdFromScript(String url, Proxy proxy) {
        try {
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection(proxy);
            httpURLConnection.setInstanceFollowRedirects(true);
            Map<String, String> map = SoundDownloadSource.getDownloadHeaders();

            for (Map.Entry<String, String> entry : map.entrySet()) {
                httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            if (httpURLConnection.getResponseCode() != 200)
                throw new IOException(httpURLConnection.getResponseCode() + " " + httpURLConnection.getResponseMessage());

            Matcher clientIdMatcher = APP_SCRIPT_CLIENT_ID_PATTERN.matcher(IOUtils.toString(httpURLConnection.getInputStream(), StandardCharsets.UTF_8));
            return clientIdMatcher.find() ? clientIdMatcher.group(1) : null;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void findIdFromSite(Proxy proxy) {
        SoundCloudSource.LOGGER.info("Retrieving sound cloud id");
        if (currentRequest == null || currentRequest.isDone()) {
            currentRequest = CompletableFuture.supplyAsync(() -> {

                try {
                    String clientId = findIdFromScript(findScriptUrl(proxy), proxy);
                    if (clientId == null)
                        throw new IOException("Failed to find client id from soundcloud script");

                    return clientId;
                } catch (Throwable e) {
                    throw new CompletionException(e);
                }
            }, HttpUtil.DOWNLOAD_EXECUTOR).thenApplyAsync(clientId -> {
                synchronized (LOCK) {
                    return currentId = clientId;
                }
            }, Util.backgroundExecutor()).exceptionally(e -> null);
        }
        currentRequest.join();
    }

    /**
     * Invalidates the previously found client id.
     */
    public static void invalidate() {
        synchronized (LOCK) {
            currentId = null;
        }
    }

    /**
     * Retrieves the client id from sound cloud using the provided proxy.
     *
     * @param proxy The proxy to use
     * @return The client id from sound cloud
     */
    public static String fetch(Proxy proxy) {
        if (currentId == null) {
            findIdFromSite(proxy);
            return currentId;
        }
        return currentId;
    }
}
