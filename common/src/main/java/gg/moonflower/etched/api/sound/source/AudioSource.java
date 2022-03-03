package gg.moonflower.etched.api.sound.source;

import gg.moonflower.etched.api.sound.download.SoundDownloadSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.api.util.FileChannelInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TranslatableComponent;
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

import java.io.*;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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

    static InputStreamSupplier downloadTo(Path file, URL url, @Nullable DownloadProgressListener progressListener, AudioFileType type) {
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
                            System.out.println(element);
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
                                // Skip no-cache
                                // Skip must-revalidate
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
                        return url::openStream;
                    }

                    if (!type.isLimit())
                        throw new IOException("The provided URL is a file, but that is not supported");
                    if (progressListener != null)
                        progressListener.progressStartDownload(contentLength / 1024.0F / 1024.0F);

                    if (contentLength > 104857600)
                        throw new IOException("Filesize is bigger than maximum allowed (file is " + contentLength + ", limit is 104857600)");

                    try (OutputStream outputStream = new DataOutputStream(new FileOutputStream(file.toFile()))) {
                        long readBytes = 0;
                        int k;
                        byte[] bs = new byte[4096];
                        while ((k = inputStream.read(bs)) >= 0) {
                            readBytes += (float) k;
                            if (progressListener != null)
                                progressListener.progressStage((float) readBytes / contentLength);

                            if (readBytes > 104857600)
                                throw new IOException("Filesize was bigger than maximum allowed (got >= " + readBytes + ", limit was 104857600)");

                            if (Thread.interrupted()) {
                                LOGGER.error("INTERRUPTED");
                                return () -> new FileChannelInputStream(FileChannel.open(file, StandardOpenOption.READ));
                            }

                            outputStream.write(bs, 0, k);
                        }
                    }
                }
            }
        } catch (Throwable var22) {
            throw new CompletionException(var22);
        }
        return () -> new FileChannelInputStream(FileChannel.open(file, StandardOpenOption.READ));
    }

    /**
     * @return A future to a resource that will exist at some point in the future
     */
    CompletableFuture<InputStream> openStream();

    enum AudioFileType {
        FILE(true, false),
        STREAM(false, true),
        BOTH(true, true);

        private final boolean limit;
        private final boolean stream;


        AudioFileType(boolean limit, boolean stream) {
            this.limit = limit;
            this.stream = stream;
        }

        public boolean isLimit() {
            return limit;
        }

        public boolean isStream() {
            return stream;
        }
    }

    @FunctionalInterface
    interface InputStreamSupplier {
        InputStream get() throws IOException;
    }
}
