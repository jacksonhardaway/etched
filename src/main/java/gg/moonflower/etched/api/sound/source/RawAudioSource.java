package gg.moonflower.etched.api.sound.source;

import gg.moonflower.etched.api.util.AsyncInputStream;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import net.minecraft.Util;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @author Ocelot
 */
public class RawAudioSource implements AudioSource {

    private final CompletableFuture<AsyncInputStream.InputStreamSupplier> locationFuture;

    public RawAudioSource(URL url, @Nullable DownloadProgressListener listener, boolean temporary, AudioFileType type) {
        this.locationFuture = CompletableFuture.supplyAsync(() -> AudioSource.downloadTo(url, temporary, listener, type), Util.nonCriticalIoPool());
    }

    @Override
    public CompletableFuture<InputStream> openStream() {
        return this.locationFuture.thenApply(stream -> {
            try {
                return stream.open();
            } catch (Exception e) {
                throw new CompletionException("Failed to open stream", e);
            }
        });
    }
}
