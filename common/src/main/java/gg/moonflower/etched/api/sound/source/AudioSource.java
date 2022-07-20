package gg.moonflower.etched.api.sound.source;

import gg.moonflower.etched.api.sound.download.SoundDownloadSource;
import gg.moonflower.etched.api.util.AsyncInputStream;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.api.util.ProgressTrackingInputStream;
import gg.moonflower.etched.client.sound.SoundCache;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.HttpUtil;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * Sources of raw audio data to be played.
 *
 * @author Ocelot
 */
public interface AudioSource {

    Logger LOGGER = LogManager.getLogger();

    static Map<String, String> getDownloadHeaders() {
        Map<String, String> map = SoundDownloadSource.getDownloadHeaders();
        map.put("X-Minecraft-Username", Minecraft.getInstance().getUser().getName());
        map.put("X-Minecraft-UUID", Minecraft.getInstance().getUser().getUuid());
        return map;
    }

    static AsyncInputStream.InputStreamSupplier downloadTo(Path file, URL url, @Nullable DownloadProgressListener progressListener, AudioFileType type) {
        if (progressListener != null)
            progressListener.progressStartRequest(new TranslatableComponent("resourcepack.requesting"));

        try {
            HttpGet get = new HttpGet(url.toURI());
            getDownloadHeaders().forEach(get::addHeader);
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                try (CloseableHttpResponse response = client.execute(get)) {
                    HttpEntity entity = response.getEntity();
                    InputStream inputStream = entity.getContent();
                    long contentLength = entity.getContentLength();

                    // Indicates a cache of "forever"
                    long cacheTime = Long.MAX_VALUE;
                    int cachePriority = 0;
                    boolean cache = true;

                    Header cacheControl = response.getFirstHeader("Cache-Control");
                    if (cacheControl != null) {
                        for (HeaderElement element : cacheControl.getElements()) {
                            switch (element.getName()) {
                                case "max-age": {
                                    if (cachePriority > 0)
                                        break;
                                    try {
                                        cacheTime = Integer.parseInt(element.getValue());
                                    } catch (NumberFormatException e) {
                                        LOGGER.error("Invalid number: " + element.getValue());
                                    }
                                    break;
                                }
                                case "s-maxage": {
                                    cachePriority = 1;
                                    try {
                                        cacheTime = Integer.parseInt(element.getValue());
                                    } catch (NumberFormatException e) {
                                        LOGGER.error("Invalid number: " + element.getValue());
                                    }
                                    break;
                                }
                                // Skip must-revalidate
                                // Skip no-cache because "hidden" files are already in the temp directory
                                case "no-store": {
                                    cache = false;
                                    break;
                                }
                                // Skip private
                                // Skip public
                                // Skip no-transform
                                // Skip immutable
                                // Skip stale-while-revalidate
                                // Skip stale-if-error
                            }
                        }
                    }

                    Header ageHeader = response.getFirstHeader("Age");
                    if (ageHeader != null) {
                        try {
                            cacheTime -= Integer.parseInt(ageHeader.getValue());
                        } catch (NumberFormatException e) {
                            LOGGER.error("Invalid number: " + ageHeader.getValue());
                        }
                    }

                    if (contentLength <= 0 || cacheTime <= 0 || !cache) {
                        if (!type.isStream())
                            throw new IOException("The provided URL is a stream, but that is not supported");
                        Files.deleteIfExists(file);
                        return () -> new AsyncInputStream(url::openStream, 8192, 4, HttpUtil.DOWNLOAD_EXECUTOR);
                    }

                    if (!type.isFile())
                        throw new IOException("The provided URL is a file, but that is not supported");
                    if (SoundCache.isValid(file, file.getFileName().toString()))
                        return () -> Files.newInputStream(file.toFile().toPath());
                    if (contentLength > 104857600)
                        throw new IOException("Filesize is bigger than maximum allowed (file is " + contentLength + ", limit is 104857600)");

                    SoundCache.updateCache(file, file.getFileName().toString(), cacheTime, TimeUnit.SECONDS, new ProgressTrackingInputStream(inputStream, contentLength, progressListener) {
                        @Override
                        public int read() throws IOException {
                            int value = super.read();
                            if (this.getRead() > 104857600)
                                throw new IOException("Filesize was bigger than maximum allowed (got >= " + this.getRead() + ", limit was 104857600)");
                            return value;
                        }

                        @Override
                        public int read(byte[] b, int off, int len) throws IOException {
                            int value = super.read(b, off, len);
                            if (this.getRead() > 104857600)
                                throw new IOException("Filesize was bigger than maximum allowed (got >= " + this.getRead() + ", limit was 104857600)");
                            return value;
                        }
                    });
                }
            }
        } catch (Throwable e) {
            throw new CompletionException(e);
        }
        return () -> Files.newInputStream(file.toFile().toPath());
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
            return file;
        }

        public boolean isStream() {
            return stream;
        }
    }
}
