package gg.moonflower.etched.api.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;

/**
 * Utilizes multiple input stream futures to act as a single data stream.
 *
 * @author Ocelot
 */
public class StreamingInputStream extends InputStream {

    private final URL[] urls;
    private final List<CompletableFuture<InputStream>> queue;
    private final IntFunction<CompletableFuture<InputStream>> source;
    private int index;
    private int position;

    public StreamingInputStream(URL[] urls, IntFunction<CompletableFuture<InputStream>> source) {
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

    private void incrementPosition() {
        this.position++;
        this.queueBuffers();
    }

    private InputStream getCurrentStream() {
        try {
            return this.queue.get(this.position).get();
        } catch (Exception e) {
            return InputStream.nullInputStream();
        }
    }

    @Override
    public int read() throws IOException {
        if (this.index == -1) {
            throw new IOException("EOF");
        }
        if (this.position >= this.urls.length) {
            return -1;
        }

        InputStream currentStream = this.getCurrentStream();
        int result = currentStream.read();
        if (result == -1) {
            this.incrementPosition();
            return this.read();
        }

        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (this.index == -1) {
            return -1;
        }
        if (this.position >= this.urls.length) {
            return -1;
        }

        InputStream currentStream = this.getCurrentStream();
        int result = currentStream.read(b, off, len);

        if (result == -1) {
            this.incrementPosition();
            return this.read(b, off, len);
        }

        return result;
    }

    @Override
    public long skip(long n) throws IOException {
        if (this.index == -1) {
            throw new IOException("EOF");
        }
        if (this.position >= this.urls.length) {
            return 0;
        }

        InputStream currentStream = this.getCurrentStream();
        long result = currentStream.skip(n);

        if (result < n) {
            this.incrementPosition();
            return this.skip(n - result);
        }

        return result;
    }

    @Override
    public void close() {
        for (CompletableFuture<InputStream> future : this.queue) {
            future.thenAccept(stream -> {
                try {
                    stream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        this.queue.clear();
        this.index = -1;
    }
}
