package me.jaackson.etched.client.sound.source;

import me.jaackson.etched.client.sound.download.AccumulatingDownloadProgressListener;
import me.jaackson.etched.client.sound.download.DownloadProgressListener;
import me.jaackson.etched.client.sound.download.SoundCache;
import me.jaackson.etched.client.sound.format.FileChannelInputStream;
import me.jaackson.etched.client.sound.format.StreamingInputStream;
import net.minecraft.Util;
import net.minecraft.util.HttpUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;

/**
 * @author Ocelot
 */
public class StreamingAudioSource implements AudioSource {

    private final Proxy proxy;
    private final boolean temporary;
    private final Path[] locations;
    private final URL[] urls;
    private final CompletableFuture<?> downloadFuture;
    private CompletableFuture<InputStream> stream;

    public StreamingAudioSource(Proxy proxy, String hash, URL[] urls, @Nullable DownloadProgressListener progressListener, boolean temporary) throws IOException {
        this.proxy = proxy;
        this.temporary = temporary;
        this.locations = new Path[urls.length];
        for (int i = 0; i < urls.length; i++)
            this.locations[i] = SoundCache.resolveFilePath(DigestUtils.sha1Hex(hash + i), temporary);
        this.urls = urls;
        int files = Math.min(urls.length, 3);
        DownloadProgressListener accumulatingListener = progressListener != null ? new AccumulatingDownloadProgressListener(progressListener, files) : null;
        this.downloadFuture = CompletableFuture.allOf(IntStream.range(0, files).mapToObj(i -> CompletableFuture.runAsync(() -> AudioSource.downloadTo(this.locations[i].toFile(), urls[i], accumulatingListener, proxy, temporary), HttpUtil.DOWNLOAD_EXECUTOR)).toArray(CompletableFuture[]::new));
    }

    @Override
    public CompletableFuture<InputStream> openStream() {
        if (this.stream != null)
            return this.stream;
        return this.stream = this.downloadFuture.thenApplyAsync(__ -> {
            try {
                return new StreamingInputStream(this.urls, i -> CompletableFuture.runAsync(() -> AudioSource.downloadTo(this.locations[i].toFile(), this.urls[i], null, this.proxy, this.temporary), HttpUtil.DOWNLOAD_EXECUTOR).thenApplyAsync(___ -> {
                    try {
                        return new FileChannelInputStream(FileChannel.open(this.locations[i], StandardOpenOption.READ));
                    } catch (Exception e) {
                        throw new CompletionException("Failed to open channel", e);
                    }
                }, Util.ioPool()));
            } catch (Exception e) {
                throw new CompletionException("Failed to open channel", e);
            }
        }, Util.ioPool());
    }
}
