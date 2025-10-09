package gg.moonflower.etched.api.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Utilizes multiple input stream futures to act as a single data stream.
 *
 * @author Ocelot
 */
public class StreamingInputStream extends InputStream {

    public static final int BUFFER_SIZE = 4;

    private final List<AsyncInputStream.InputStreamSupplier> source;
    private final Deque<InputStream> queue;
    private int index;

    public StreamingInputStream(List<AsyncInputStream.InputStreamSupplier> source) {
        this.source = source;
        this.queue = new ArrayDeque<>(BUFFER_SIZE);
        this.index = 0;
        this.queueBuffers();
    }

    private void queueBuffers() {
        while (this.queue.size() < BUFFER_SIZE && this.index < this.source.size()) {
            try {
                this.queue.add(this.source.get(this.index).open());
            } catch (Throwable e) {
                e.printStackTrace();
            }
            this.index++;
        }
    }

    private void incrementPosition() {
        try {
            this.queue.removeFirst().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.queueBuffers();
    }

    @Override
    public int read() throws IOException {
        if (this.queue.isEmpty() && (this.index == -1 || this.index >= this.source.size())) {
            return -1;
        }

        InputStream currentStream = this.queue.getFirst();
        int result = currentStream.read();
        if (result == -1) {
            this.incrementPosition();
            return this.read();
        }

        return result;
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        if (this.queue.isEmpty() && (this.index == -1 || this.index >= this.source.size())) {
            return -1;
        }

        int read = 0;
        while (read < len) {
            InputStream currentStream = this.queue.peekFirst();
            if (currentStream == null) {
                return read;
            }

            int result = currentStream.read(b, off + read, len - read);
            // If EOF, then try to continue with the next stream
            if (result == -1) {
                this.incrementPosition();
                continue;
            }

            read += result;
        }

        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        if (this.queue.isEmpty() && (this.index == -1 || this.index >= this.source.size())) {
            return 0;
        }

        InputStream currentStream = this.queue.getFirst();
        long result = currentStream.skip(n);

        if (result < n) {
            this.incrementPosition();
            return this.skip(n - result);
        }

        return result;
    }

    @Override
    public void close() {
        for (InputStream stream : this.queue) {
            try {
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.queue.clear();
        this.index = -1;
    }
}
