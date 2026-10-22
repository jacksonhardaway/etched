package gg.moonflower.etched.api.sound.source;

import gg.moonflower.etched.api.util.AccumulatingDownloadProgressListener;
import gg.moonflower.etched.api.util.AsyncInputStream;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.api.util.StreamingInputStream;
import net.minecraft.Util;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @author Ocelot
 */
public class StreamingAudioSource implements AudioSource {

    /**
     * The number of segments downloaded ahead of playback. Downloading every segment at once starts a thread per segment, which floods the network on long tracks
     */
    public static final int PREFETCH_SEGMENTS = StreamingInputStream.BUFFER_SIZE * 2;

    private final URL[] urls;
    private final DownloadProgressListener progressListener;
    private final boolean temporary;
    private final AudioFileType type;

    public StreamingAudioSource(URL[] urls, @Nullable DownloadProgressListener progressListener, boolean temporary, AudioFileType type) {
        this.urls = urls;
        // Only the segments needed to start playing report progress
        this.progressListener = progressListener != null ? new AccumulatingDownloadProgressListener(progressListener, Math.min(urls.length, StreamingInputStream.BUFFER_SIZE)) : null;
        this.temporary = temporary;
        this.type = type;
    }

    private List<AsyncInputStream.InputStreamSupplier> createSegments(List<CompletableFuture<AsyncInputStream.InputStreamSupplier>> downloads) {
        List<AsyncInputStream.InputStreamSupplier> segments = new ArrayList<>(this.urls.length);
        for (int i = 0; i < this.urls.length; i++) {
            int index = i;
            segments.add(() -> {
                CompletableFuture<AsyncInputStream.InputStreamSupplier> download;
                synchronized (downloads) {
                    for (int j = index; j < Math.min(index + PREFETCH_SEGMENTS, this.urls.length); j++) {
                        if (downloads.get(j) == null) {
                            URL url = this.urls[j];
                            DownloadProgressListener listener = j < StreamingInputStream.BUFFER_SIZE ? this.progressListener : null;
                            downloads.set(j, CompletableFuture.supplyAsync(() -> AudioSource.downloadTo(url, this.temporary, listener, this.type), Util.nonCriticalIoPool()));
                        }
                    }
                    download = downloads.set(index, null); // Played segments are never opened again, so don't keep them around
                }

                try {
                    return download.join().open();
                } catch (CompletionException e) {
                    throw new IOException("Failed to download segment " + index, e.getCause());
                }
            });
        }
        return segments;
    }

    @Override
    public CompletableFuture<InputStream> openStream() {
        return CompletableFuture.supplyAsync(() -> {
            List<CompletableFuture<AsyncInputStream.InputStreamSupplier>> downloads = new ArrayList<>(this.urls.length);
            for (int i = 0; i < this.urls.length; i++) {
                downloads.add(null);
            }

            List<AsyncInputStream.InputStreamSupplier> segments = this.createSegments(downloads);
            InputStream first;
            try {
                first = segments.getFirst().open(); // Fail early instead of silently skipping every segment when the source is unreachable
            } catch (IOException e) {
                throw new CompletionException(e);
            }
            segments.set(0, () -> first);
            return new StreamingInputStream(segments);
        }, Util.nonCriticalIoPool()); // Opening downloads the first segments
    }
}
