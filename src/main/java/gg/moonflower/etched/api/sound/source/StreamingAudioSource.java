package gg.moonflower.etched.api.sound.source;

import gg.moonflower.etched.api.util.AccumulatingDownloadProgressListener;
import gg.moonflower.etched.api.util.AsyncInputStream;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.api.util.StreamingInputStream;
import net.minecraft.Util;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Ocelot
 */
public class StreamingAudioSource implements AudioSource {

    private final CompletableFuture<List<AsyncInputStream.InputStreamSupplier>> downloadFuture;

    public StreamingAudioSource(URL[] urls, @Nullable DownloadProgressListener progressListener, boolean temporary, AudioFileType type) {
        DownloadProgressListener accumulatingListener = progressListener != null ? new AccumulatingDownloadProgressListener(progressListener, urls.length) : null;

        List<CompletableFuture<AsyncInputStream.InputStreamSupplier>> list = new ArrayList<>(urls.length);
        for (URL value : urls) {
            CompletableFuture<AsyncInputStream.InputStreamSupplier> future = CompletableFuture.supplyAsync(() -> AudioSource.downloadTo(value, temporary, accumulatingListener, type), Util.nonCriticalIoPool());
            list.add(future);
        }

        this.downloadFuture = Util.sequenceFailFast(list);
    }

    @Override
    public CompletableFuture<InputStream> openStream() {
        return this.downloadFuture.thenApply(StreamingInputStream::new);
    }
}
