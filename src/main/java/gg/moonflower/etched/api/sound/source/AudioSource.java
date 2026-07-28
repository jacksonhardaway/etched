package gg.moonflower.etched.api.sound.source;

import gg.moonflower.etched.api.sound.download.SoundDownloadSource;
import gg.moonflower.etched.api.util.AsyncInputStream;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.api.util.LiveAudioInputStream;
import gg.moonflower.etched.api.util.ReconnectingInputStream;
import gg.moonflower.etched.api.util.ProgressTrackingInputStream;
import gg.moonflower.etched.client.sound.SoundCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.network.chat.Component;
import net.minecraft.util.HttpUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Sources of raw audio data to be played.
 *
 * @author Ocelot
 */
public interface AudioSource {

    Logger LOGGER = LogManager.getLogger();
    long MAX_SIZE = 100 * 1024 * 1024; // 100MB

    /**
     * @return The vanilla Minecraft client download headers
     */
    static Map<String, String> getDownloadHeaders() {
        Map<String, String> map = SoundDownloadSource.getDownloadHeaders();
        User user = Minecraft.getInstance().getUser();
        map.put("X-Minecraft-Username", user.getName());
        map.put("X-Minecraft-UUID", user.getUuid());
        return map;
    }


    /**
     * Opens a dedicated HTTP connection for live audio streaming.
     *
     * A fresh connection is used for each stream (never reused).  The
     * {@code Accept-Encoding: identity} header prevents SHOUTcast/Icecast
     * servers from applying compression that would corrupt the raw MP3
     * byte stream.  Read timeouts are set to 30s — stalled
     * connections are handled by the {@code ReconnectingInputStream}
     * wrapper, not by waiting for the timeout.
     *
     * The returned stream's {@code close()} method also disconnects
     * the underlying {@link HttpURLConnection}.
     */
    static InputStream openLiveConnection(URL url, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        headers.forEach(connection::setRequestProperty);
        connection.setRequestProperty("Accept-Encoding", "identity");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000); // A stalled station is reopened by ReconnectingInputStream.

        try {
            int response = connection.getResponseCode();
            if (response != HttpURLConnection.HTTP_OK) {
                throw new IOException("Failed to connect to " + url + ": " + response + ". " + connection.getResponseMessage());
            }

            InputStream input = connection.getInputStream();
            return new FilterInputStream(input) {
                private boolean disconnected;

                @Override
                public void close() throws IOException {
                    if (this.disconnected) {
                        return;
                    }
                    this.disconnected = true;
                    try {
                        super.close();
                    } finally {
                        connection.disconnect();
                    }
                }
            };
        } catch (Throwable t) {
            connection.disconnect();
            if (t instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Failed to open live audio connection", t);
        }
    }

    /**
     * Downloads audio data from a URL.
     *
     * For files (content length known): downloads to disk cache with
     * standard HTTP headers and cache-control handling.
     *
     * For live streams (content length unknown or no-cache): the first
     * connection is used only to classify the URL (status code, headers)
     * and is disconnected in a finally block.  The actual streaming
     * connection is opened separately by {@link #openLiveConnection},
     * wrapped in {@code ReconnectingInputStream} for automatic reconnect
     * on server disconnect, then buffered through {@code AsyncInputStream}.
     */
    static AsyncInputStream.InputStreamSupplier downloadTo(URL url, boolean temporary, @Nullable DownloadProgressListener progressListener, AudioFileType type) {
        Path path;
        try {
            path = SoundCache.resolveFilePath(url.toString(), temporary);
        } catch (Throwable t) {
            throw new CompletionException(t);
        }

        String key = url.toString();
        SoundCache.CacheMetadata metadata = SoundCache.getMetadata(key);
        if (Files.exists(path) && metadata != null && metadata.isFresh() && !metadata.noCache()) {
            return () -> Files.newInputStream(path);
        }

        if (progressListener != null) {
            progressListener.progressStartRequest(Component.translatable("resourcepack.requesting"));
        }
        HttpURLConnection connection = null;
        try {
            // Snapshot headers now — the lambda captures this reference and runs
            // on a background thread where getDownloadHeaders() may be stale.
            Map<String, String> requestHeaders = Map.copyOf(getDownloadHeaders());
            connection = (HttpURLConnection) url.openConnection();
            requestHeaders.forEach(connection::setRequestProperty);

            int response = connection.getResponseCode();
            if (response != 200) {
                // There was a server error, but there is a valid local cache so use the cached value
                if (Files.exists(path) && (metadata != null && (metadata.isFresh() || metadata.staleIfError()))) {
                    return () -> Files.newInputStream(path);
                }
                throw new IOException("Failed to connect to " + url + ": " + response + ". " + connection.getResponseMessage());
            }

            long contentLength = connection.getContentLengthLong();

            // Indicates a cache of "forever"
            long cacheTime = Long.MAX_VALUE;
            int cachePriority = 0;
            boolean noCache = false;
            boolean staleIfError = false;
            boolean noStore = false;

            String cacheControl = connection.getHeaderField("Cache-Control");
            if (cacheControl != null) {
                String[] parts = cacheControl.split(",");
                for (String part : parts) {
                    try {
                        String[] entry = part.split("=");
                        String name = entry[0].trim();
                        String value = entry.length > 1 ? entry[1].trim() : null;
                        switch (name) {
                            case "max-age" -> {
                                if (cachePriority > 0) {
                                    break;
                                }
                                try {
                                    cacheTime = Integer.parseInt(Objects.requireNonNull(value));
                                } catch (NumberFormatException e) {
                                    LOGGER.error("Invalid max-age: " + value);
                                }
                            }
                            case "s-maxage" -> {
                                cachePriority = 1;
                                try {
                                    cacheTime = Integer.parseInt(Objects.requireNonNull(value));
                                } catch (NumberFormatException e) {
                                    LOGGER.error("Invalid s-maxage: " + value);
                                }
                            }

                            // Skip must-revalidate
                            case "no-cache" -> noCache = true;
                            case "no-store" -> noStore = true;

                            // Skip private
                            // Skip public
                            // Skip no-transform
                            // Skip immutable
                            // Skip stale-while-revalidate
                            case "stale-if-error" -> staleIfError = true;
                        }
                    } catch (Exception e) {
                        LOGGER.error("Invalid response header: {}", part, e);
                    }
                }
            }

            String ageHeader = connection.getHeaderField("Age");
            if (ageHeader != null) {
                try {
                    cacheTime -= Integer.parseInt(ageHeader);
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid Age: " + ageHeader);
                }
            }

            // Handle streams
            if (contentLength < 0 || cacheTime <= 0 || noStore) {
                if (contentLength > 0 && type.isFile()) {
                    // Simply treat as a regular file
                    LOGGER.debug("No-cache file found!");
                    // Enable caching
                    cacheTime = Long.MAX_VALUE;
                } else {
                    Files.deleteIfExists(path);
                    SoundCache.updateCacheMetadata(key, null);
                    if (!type.isStream()) {
                        throw new IOException("The provided URL is a stream, but that is not supported");
                    }
                    Map<String, String> liveHeaders = requestHeaders;
                    return () -> new LiveAudioInputStream(new AsyncInputStream(
                            () -> new ReconnectingInputStream(() -> openLiveConnection(url, liveHeaders)),
                            8192,
                            8,
                            HttpUtil.DOWNLOAD_EXECUTOR));
                }
            }

            // The cached file is still fresh, so only the metadata needs to be updated
            long expiration = cacheTime == Long.MAX_VALUE ? cacheTime : System.currentTimeMillis() / 1000L + cacheTime;
            if (Files.exists(path) && metadata != null && metadata.isFresh()) {
                SoundCache.updateCacheMetadata(key, new SoundCache.CacheMetadata(expiration, noCache, staleIfError));
                return () -> Files.newInputStream(path);
            }

            if (!type.isFile()) {
                throw new IOException("The provided URL is a file, but that is not supported");
            }

            if (contentLength > MAX_SIZE) {
                throw new IOException("File size is bigger than maximum allowed (file is " + contentLength + ", limit is " + MAX_SIZE + ")");
            }

            try (InputStream stream = new ProgressTrackingInputStream(connection.getInputStream(), contentLength, progressListener) {
                @Override
                public int read() throws IOException {
                    int value = super.read();
                    if (this.getRead() > MAX_SIZE) {
                        throw new IOException("File size was bigger than maximum allowed (got >= " + this.getRead() + ", limit was " + MAX_SIZE + ")");
                    }
                    return value;
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    int value = super.read(b, off, len);
                    if (this.getRead() > MAX_SIZE) {
                        throw new IOException("File size was bigger than maximum allowed (got >= " + this.getRead() + ", limit was " + MAX_SIZE + ")");
                    }
                    return value;
                }
            }) {
                SoundCache.updateCache(path, key, stream, new SoundCache.CacheMetadata(expiration, noCache, staleIfError));
            }
        } catch (Throwable e) {
            throw new CompletionException(e);
        } finally {
            if (connection != null) {
                // The first request only classifies the URL. Never leave its body open
                // while a second connection is used for actual live playback.
                connection.disconnect();
            }
        }
        return () -> Files.newInputStream(path);
    }

    /**
     * @return A future to a resource that will exist at some point in the future
     */
    CompletableFuture<InputStream> openStream();

    enum AudioFileType {
        FILE(true, false),
        STREAM(false, true),
        BOTH(true, true);

        private final boolean file;
        private final boolean stream;


        AudioFileType(boolean file, boolean stream) {
            this.file = file;
            this.stream = stream;
        }

        public boolean isFile() {
            return this.file;
        }

        public boolean isStream() {
            return this.stream;
        }
    }
}
