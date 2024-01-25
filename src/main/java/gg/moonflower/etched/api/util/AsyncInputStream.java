package gg.moonflower.etched.api.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Asynchronously reads data from one stream into a buffer for another thread to read from.
 *
 * @author Ocelot
 * @since 1.2.0
 */
public class AsyncInputStream extends InputStream {

    private static final int MAX_DATA = 32768; // A maximum of 32KB can be loaded into memory

    private final List<byte[]> readBytes;
    private final CompletableFuture<?> readFuture;
    private final Lock lock;
    private final int maxBuffers;
    private int pointer;
    private byte[] currentData;
    private volatile boolean closed;
    private CompletableFuture<?> waitFuture;

    public AsyncInputStream(InputStreamSupplier source, int bufferSize, int buffers, Executor readExecutor) throws IOException {
        this.readBytes = new LinkedList<>();
        this.lock = new ReentrantLock();
        this.maxBuffers = Math.max(buffers, MAX_DATA / bufferSize); // At least X buffers, even if it exceeds the data limit

        CompletableFuture<?> initialWait = new CompletableFuture<>();
        this.waitFuture = CompletableFuture.completedFuture(null); // Nothing to wait for initially
        this.readFuture = CompletableFuture.runAsync(() -> {
            try (InputStream stream = source.get()) { // Create stream off-thread to prevent threaded stream issues
                while (!this.closed) {
                    byte[] buffer = new byte[bufferSize];
                    int read, byteCount = 0;
                    while (!this.closed && byteCount < buffer.length && (read = stream.read(buffer, byteCount, buffer.length - byteCount)) != -1) { // Read from stream until closed
                        byteCount += read;
                    }

                    if (!this.closed && byteCount > 0) { // Only append buffers if data is read and not closed
                        if (byteCount < buffer.length) {
                            byte[] newBuffer = new byte[byteCount];
                            System.arraycopy(buffer, 0, newBuffer, 0, newBuffer.length);
                            this.appendBuffer(newBuffer);
                        } else {
                            this.appendBuffer(buffer);
                        }
                    }

                    if (!initialWait.isDone() && (this.closed || this.readBytes.size() >= buffers)) { // Complete initial wait if enough is read or buffer is closed
                        initialWait.complete(null);
                    }
                }
            } catch (IOException e) {
                if (!initialWait.isDone()) {
                    initialWait.completeExceptionally(e);
                }
                throw new CompletionException(e);
            }
        }, readExecutor);

        try {
            initialWait.join(); // Wait for initial buffers to fill before continuing
        } catch (CompletionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else {
                throw new IOException(e.getCause());
            }
        }
    }

    private void appendBuffer(byte[] data) {
        if (this.closed) { // If closed, no point in adding new buffers
            return;
        }
        this.waitFuture.join();
        if (this.closed) { // close() unlocks this thread after closed has been set
            return;
        }
        try {
            this.lock.lock();
            this.readBytes.add(data);
            if (this.readBytes.size() >= this.maxBuffers) {
                this.waitFuture = new CompletableFuture<>(); // Enough data has been read, wait until some is read
            }
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * @return <code>true</code> if EOF has been reached
     */
    private boolean nextBuffer() {
        try {
            this.lock.lock();
            this.pointer = 0;
            if (!this.waitFuture.isDone() && this.readBytes.size() < this.maxBuffers) {
                this.waitFuture.complete(null); // Unlock read thread after enough data is read
            }
            if (this.readBytes.isEmpty()) {
                this.currentData = null;
                return true;
            }
            this.currentData = this.readBytes.remove(0);
            return false;
        } finally {
            this.lock.unlock();
        }
    }

    private void rethrowException() throws IOException {
        if (this.readFuture.isCompletedExceptionally()) {
            try {
                this.readFuture.join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                } else {
                    throw new IOException(e.getCause());
                }
            }
        }
    }

    @Override
    public int read() throws IOException {
        this.rethrowException();
        if ((this.currentData == null || this.pointer >= this.currentData.length) && this.nextBuffer()) {
            return -1;
        }
        return this.currentData[this.pointer++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        this.rethrowException();
        if ((this.currentData == null || this.pointer >= this.currentData.length) && this.nextBuffer()) {
            return -1;
        }

        int readCount = 0;
        while (readCount < len) {
            if ((this.currentData == null || this.pointer >= this.currentData.length) && this.nextBuffer()) {
                return readCount;
            }

            int readSize = Math.min(this.currentData.length - this.pointer, len - readCount);
            System.arraycopy(this.currentData, this.pointer, b, off, readSize);
            readCount += readSize;
            this.pointer += readSize;
        }

        return readCount;
    }

    @Override
    public long skip(long n) {
        if ((this.currentData == null || this.pointer >= this.currentData.length) && this.nextBuffer()) {
            return 0;
        }

        long result = 0;
        while (result < n) {
            if ((this.currentData == null || this.pointer >= this.currentData.length) && this.nextBuffer()) {
                return result;
            }

            long readSize = Math.min(this.currentData.length - this.pointer, n - result);
            result += readSize;
            this.pointer += (int) readSize;
        }

        return result;
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        this.waitFuture.complete(null); // Force read thread to stop waiting
        this.readFuture.join();
    }

    /**
     * Provides an {@link AsyncInputStream} with a new stream on the correct thread.
     *
     * @author Ocelot
     * @since 1.2.0
     */
    @FunctionalInterface
    public interface InputStreamSupplier {

        /**
         * @return A newly opened stream
         * @throws IOException If any error occurs
         */
        InputStream get() throws IOException;
    }
}
