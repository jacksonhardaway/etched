package gg.moonflower.etched.api.sound.source;

import gg.moonflower.etched.api.util.AccumulatingDownloadProgressListener;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.api.util.StreamingInputStream;
import net.minecraft.Util;
import net.minecraft.util.HttpUtil;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;

/**
 * @author Ocelot
 */
public class StreamingAudioSource implements AudioSource {

    private final URL[] urls;
    private final boolean temporary;
    private final AudioFileType type;
    private final CompletableFuture<?> downloadFuture;
    private CompletableFuture<InputStream> stream;

    public StreamingAudioSource(URL[] urls, @Nullable DownloadProgressListener progressListener, boolean temporary, AudioFileType type) {
        this.urls = urls;
        this.temporary = temporary;
        this.type = type;
        int files = Math.min(urls.length, 3);
        DownloadProgressListener accumulatingListener = progressListener != null ? new AccumulatingDownloadProgressListener(progressListener, files) : null;
        this.downloadFuture = CompletableFuture.allOf(IntStream.range(0, files).mapToObj(i -> CompletableFuture.runAsync(() -> AudioSource.downloadTo(urls[i], temporary, accumulatingListener, type), HttpUtil.DOWNLOAD_EXECUTOR)).toArray(CompletableFuture[]::new));
    }

    @Override
    public CompletableFuture<InputStream> openStream() {
        if (this.stream == null) {
            this.stream = this.downloadFuture.thenApplyAsync(__ -> {
                try {
                    return new StreamingInputStream(this.urls, i -> CompletableFuture.supplyAsync(() -> AudioSource.downloadTo(this.urls[i], this.temporary, null, this.type), HttpUtil.DOWNLOAD_EXECUTOR).thenApplyAsync(stream -> {
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
        return this.stream;
    }
}
