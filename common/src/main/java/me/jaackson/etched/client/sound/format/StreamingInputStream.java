package me.jaackson.etched.client.sound.format;

import net.minecraft.Util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;

/**
 * <p>Utilizes multiple input stream futures to act as a single data stream.</p>
 *
 * @author Ocelot
 */
public class StreamingInputStream extends InputStream implements SeekingStream {

    private final URL[] urls;
    private final List<CompletableFuture<FileChannelInputStream>> queue;
    private final IntFunction<CompletableFuture<FileChannelInputStream>> source;
    private int index;
    private int position;

    public StreamingInputStream(URL[] urls, IntFunction<CompletableFuture<FileChannelInputStream>> source) {
        this.urls = urls;
        this.queue = new ArrayList<>(urls.length);
        this.source = source;
        this.index = 0;
        this.position = 0;
        this.queueBuffers();
    }

    private void queueBuffers() {
        while (this.index < this.position + 3 && this.index < this.urls.length) {
            this.queue.add(this.source.apply(this.index));
            this.index++;
        }
    }

    private void incrementPosition() throws IOException {
        this.getCurrentStream().beginning();
        this.position++;
        this.queueBuffers();
    }

    private FileChannelInputStream getCurrentStream() {
        return this.queue.get(this.position).join();
    }

    @Override
    public void beginning() throws IOException {
        this.getCurrentStream().beginning();
        this.position = 0;
        this.queueBuffers();
    }

    @Override
    public int read() throws IOException {
        if (this.index == -1)
            throw new IOException("EOF");
        if (this.position >= this.urls.length)
            return -1;

        FileChannelInputStream currentStream = this.getCurrentStream();
        int result = currentStream.read();
        if (result == -1) {
            this.incrementPosition();
            return this.read();
        }

        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (this.index == -1)
            throw new IOException("EOF");
        if (this.position >= this.urls.length)
            return -1;

        FileChannelInputStream currentStream = this.getCurrentStream();
        int result = currentStream.read(b, off, len);

        if (result == -1) {
            this.incrementPosition();
            return this.read(b, off, len);
        }

        return result;
    }

    @Override
    public long skip(long n) throws IOException {
        if (this.index == -1)
            throw new IOException("EOF");
        if (this.position >= this.urls.length)
            return 0;

        FileChannelInputStream currentStream = this.getCurrentStream();
        long result = currentStream.skip(n);

        if (result == 0) {
            this.incrementPosition();
            return this.skip(n);
        }

        return result;
    }

    @Override
    public void close() {
        for (CompletableFuture<FileChannelInputStream> future : this.queue) {
            future.thenAcceptAsync(stream -> {
                try {
                    stream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, Util.backgroundExecutor());
        }
        this.queue.clear();
        this.index = -1;
    }
}
