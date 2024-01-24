package gg.moonflower.etched.api.sound.source;

import gg.moonflower.etched.api.util.AccumulatingDownloadProgressListener;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.api.util.StreamingInputStream;
import gg.moonflower.etched.client.sound.SoundCache;
import net.minecraft.Util;
import net.minecraft.util.HttpUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;

/**
 * @author Ocelot
 */
public class StreamingAudioSource implements AudioSource {

    private final AudioFileType type;
    private final Path[] locations;
    private final URL[] urls;
    private final CompletableFuture<?> downloadFuture;
    private CompletableFuture<InputStream> stream;

    public StreamingAudioSource(String hash, URL[] urls, @Nullable DownloadProgressListener progressListener, boolean temporary, AudioFileType type) throws IOException {
        this.type = type;
        this.locations = new Path[urls.length];
        for (int i = 0; i < urls.length; i++) {
            this.locations[i] = SoundCache.resolveFilePath(DigestUtils.sha1Hex(hash + i), temporary);
        }
        this.urls = urls;
        int files = Math.min(urls.length, 3);
        DownloadProgressListener accumulatingListener = progressListener != null ? new AccumulatingDownloadProgressListener(progressListener, files) : null;
        this.downloadFuture = CompletableFuture.allOf(IntStream.range(0, files).mapToObj(i -> CompletableFuture.runAsync(() -> AudioSource.downloadTo(this.locations[i], urls[i], accumulatingListener, type), HttpUtil.DOWNLOAD_EXECUTOR)).toArray(CompletableFuture[]::new));
    }

    @Override
    public CompletableFuture<InputStream> openStream() {
        if (this.stream != null) {
            return this.stream;
        }
        return this.stream = this.downloadFuture.thenApplyAsync(__ -> {
            try {
                return new StreamingInputStream(this.urls, i -> CompletableFuture.supplyAsync(() -> AudioSource.downloadTo(this.locations[i], this.urls[i], null, this.type), HttpUtil.DOWNLOAD_EXECUTOR).thenApplyAsync(stream -> {
                    try {
                        return stream.get();
                    } catch (Exception e) {
                        throw new CompletionException("Failed to open channel", e);
                    }
                }, Util.ioPool()));
            } catch (Exception e) {
                throw new CompletionException("Failed to open stream", e);
            }
        }, Util.ioPool());
    }
}
