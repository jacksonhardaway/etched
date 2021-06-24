package me.jaackson.etched.client.sound.source;

import me.jaackson.etched.client.sound.download.SoundCache;
import me.jaackson.etched.client.sound.format.FileChannelInputStream;
import net.minecraft.Util;
import net.minecraft.util.HttpUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @author Ocelot
 */
public class RawAudioSource implements AudioSource {

    private final Path location;
    private final CompletableFuture<?> downloadFuture;
    private CompletableFuture<InputStream> stream;

    public RawAudioSource(Proxy proxy, String hash, URL url, boolean temporary) throws IOException {
        this.location = SoundCache.resolveFilePath(hash, temporary);
        this.downloadFuture = CompletableFuture.runAsync(() -> AudioSource.downloadTo(this.location.toFile(), url, null, proxy, temporary), HttpUtil.DOWNLOAD_EXECUTOR);
    }

    @Override
    public CompletableFuture<InputStream> openStream() {
        if (this.stream != null)
            return this.stream;
        return this.stream = this.downloadFuture.thenApplyAsync(__ -> {
            try {
                return new FileChannelInputStream(FileChannel.open(this.location, StandardOpenOption.READ));
            } catch (Exception e) {
                throw new CompletionException("Failed to open channel", e);
            }
        }, Util.ioPool());
    }
}
