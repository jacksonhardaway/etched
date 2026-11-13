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
 * Etched's original asynchronous stream, with one deliberate fix: a temporarily
 * empty buffer waits for the producer instead of being reported as EOF.
 */
public class AsyncInputStream extends InputStream {

    private static final int MAX_DATA = 32768;

    private final List<byte[]> readBytes = new LinkedList<>();
    private final CompletableFuture<?> readFuture;
    private final Lock lock = new ReentrantLock();
    private final Object dataMonitor = new Object();
    private final int maxBuffers;
    private int pointer;
    private byte[] currentData;
    private volatile boolean closed;
    private volatile boolean finished;
    private volatile IOException readException;
    private CompletableFuture<?> waitFuture = CompletableFuture.completedFuture(null);

    public AsyncInputStream(InputStreamSupplier source, int bufferSize, int buffers, Executor readExecutor) throws IOException {
        this.maxBuffers = Math.max(buffers, MAX_DATA / bufferSize);
        CompletableFuture<?> initialWait = new CompletableFuture<>();
        this.readFuture = CompletableFuture.runAsync(() -> {
            try (InputStream stream = source.open()) {
                while (!this.closed) {
                    byte[] buffer = new byte[bufferSize];
                    int read;
                    int byteCount = 0;
                    while (!this.closed && byteCount < buffer.length
                            && (read = stream.read(buffer, byteCount, buffer.length - byteCount)) != -1) {
                        if (read > 0) {
                            byteCount += read;
                        }
                    }

                    if (!this.closed && byteCount > 0) {
                        if (byteCount < buffer.length) {
                            byte[] partial = new byte[byteCount];
                            System.arraycopy(buffer, 0, partial, 0, byteCount);
                            this.appendBuffer(partial);
                        } else {
                            this.appendBuffer(buffer);
                        }
                    }

                    if (!initialWait.isDone() && (this.closed || this.readBytes.size() >= buffers)) {
                        initialWait.complete(null);
                    }
                    if (byteCount < buffer.length) {
                        break;
                    }
                }
            } catch (IOException e) {
                if (!this.closed) {
                    this.readException = e;
                    initialWait.completeExceptionally(e);
                    throw new CompletionException(e);
                }
            } finally {
                this.finished = true;
                initialWait.complete(null);
                this.signalData();
            }
        }, readExecutor);

        try {
            initialWait.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException(e.getCause());
        }
    }

    private void appendBuffer(byte[] data) {
        if (this.closed) return;
        this.waitFuture.join();
        if (this.closed) return;
        this.lock.lock();
        try {
            this.readBytes.add(data);
            if (this.readBytes.size() >= this.maxBuffers) {
                this.waitFuture = new CompletableFuture<>();
            }
        } finally {
            this.lock.unlock();
        }
        this.signalData();
    }

    private void signalData() {
        synchronized (this.dataMonitor) {
            this.dataMonitor.notifyAll();
        }
    }

    private boolean nextBuffer() throws IOException {
        while (true) {
            this.lock.lock();
            try {
                this.pointer = 0;
                if (!this.readBytes.isEmpty()) {
                    this.currentData = this.readBytes.remove(0);
                    if (!this.waitFuture.isDone() && this.readBytes.size() < this.maxBuffers) {
                        this.waitFuture.complete(null);
                    }
                    return false;
                }
                if (this.readException != null) throw this.readException;
                if (this.closed || this.finished) {
                    this.currentData = null;
                    return true;
                }
            } finally {
                this.lock.unlock();
            }

            synchronized (this.dataMonitor) {
                try {
                    this.dataMonitor.wait(250L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                }
            }
        }
    }

    private void rethrowException() throws IOException {
        if (this.readException != null) throw this.readException;
        if (this.readFuture.isCompletedExceptionally()) {
            try {
                this.readFuture.join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof IOException ioException) throw ioException;
                throw new IOException(e.getCause());
            }
        }
    }

    @Override
    public int read() throws IOException {
        this.rethrowException();
        if ((this.currentData == null || this.pointer >= this.currentData.length) && this.nextBuffer()) return -1;
        return this.currentData[this.pointer++] & 0xFF;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        if (length == 0) return 0;
        this.rethrowException();
        int readCount = 0;
        while (readCount < length) {
            if ((this.currentData == null || this.pointer >= this.currentData.length) && this.nextBuffer()) {
                return readCount == 0 ? -1 : readCount;
            }
            int readSize = Math.min(this.currentData.length - this.pointer, length - readCount);
            System.arraycopy(this.currentData, this.pointer, bytes, offset + readCount, readSize);
            this.pointer += readSize;
            readCount += readSize;
        }
        return readCount;
    }

    @Override
    public long skip(long count) throws IOException {
        long skipped = 0;
        while (skipped < count) {
            if ((this.currentData == null || this.pointer >= this.currentData.length) && this.nextBuffer()) break;
            int amount = (int) Math.min(this.currentData.length - this.pointer, count - skipped);
            this.pointer += amount;
            skipped += amount;
        }
        return skipped;
    }

    @Override
    public void close() throws IOException {
        this.stopProducing();
    }

    /**
     * Stops accepting network data but preserves already-buffered bytes so the
     * audio engine can reach EOF naturally without a forced OpenAL teardown.
     */
    public void stopProducing() {
        this.closed = true;
        this.waitFuture.complete(null);
        this.signalData();
        // Do not close the HTTP socket from Minecraft's render/sound thread, nor from
        // a competing worker: the producer sees this flag after its current read and
        // closes the socket itself in its try-with-resources block.
    }

    @FunctionalInterface
    public interface InputStreamSupplier {
        InputStream open() throws IOException;
    }
}
