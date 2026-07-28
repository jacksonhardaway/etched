package gg.moonflower.etched.api.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Asynchronously reads data from a source stream into a bounded in-memory
 * queue for another thread to consume.
 *
 * A background producer thread fetches chunks from the source and appends
 * them to an internal queue.  The consumer (caller of {@code read()})
 * blocks on an empty queue until data arrives, the producer reaches real
 * EOF, or {@link #close()} is called.  Temporary network pauses never
 * produce a false EOF — the consumer simply waits.
 *
 * The producer is protected against busy-looping on stream end and the
 * consumer receives one internal chunk per {@code read()} call, avoiding
 * the byte-overwrite bug present in earlier versions.
 *
 * @author Ocelot
 * @since 1.2.0
 */
public class AsyncInputStream extends InputStream {

    /** Keep at least this much source data buffered when the requested buffer count is smaller. */
    private static final int MIN_BUFFERED_DATA = 32768;

    private final Deque<byte[]> readBytes;
    private final CompletableFuture<Void> initialWait;
    private final CompletableFuture<Void> readFuture;
    private final ReentrantLock lock;
    private final Condition notEmpty;
    private final Condition notFull;
    private final int initialBuffers;
    private final int maxBuffers;

    private int pointer;
    private byte[] currentData;
    private volatile boolean closed;
    private volatile InputStream sourceStream;
    private boolean eof;
    private IOException failure;

    public AsyncInputStream(InputStreamSupplier source, int bufferSize, int buffers, Executor readExecutor) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(readExecutor, "readExecutor");
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize must be positive");
        }
        if (buffers <= 0) {
            throw new IllegalArgumentException("buffers must be positive");
        }

        this.readBytes = new ArrayDeque<>();
        this.initialWait = new CompletableFuture<>();
        this.lock = new ReentrantLock();
        this.notEmpty = this.lock.newCondition();
        this.notFull = this.lock.newCondition();
        this.initialBuffers = buffers;
        int minimumBuffers = Math.max(1, (MIN_BUFFERED_DATA + bufferSize - 1) / bufferSize);
        this.maxBuffers = Math.max(buffers, minimumBuffers);

        try {
            this.readFuture = CompletableFuture.runAsync(() -> this.readLoop(source, bufferSize), readExecutor);
        } catch (RuntimeException e) {
            throw new IOException("Failed to start asynchronous stream reader", e);
        }

        try {
            this.initialWait.join(); // Wait for pre-buffering, true EOF, or an opening/read failure.
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException(cause);
        }
    }

    /**
     * Producer loop running on the background executor.  Reads chunks from the
     * source stream and appends them to the internal queue.  Exits when the
     * source reaches EOF, when {@link #closed} is set by {@link #close()},
     * or when an I/O error occurs.
     */
    private void readLoop(InputStreamSupplier source, int bufferSize) {
        IOException problem = null;
        try (InputStream stream = Objects.requireNonNull(source.get(), "source returned null")) {
            this.sourceStream = stream;
            boolean endOfStream = false;

            while (!this.closed && !endOfStream) {
                byte[] buffer = new byte[bufferSize];
                int byteCount = 0;

                while (!this.closed && byteCount < buffer.length) {
                    int read = stream.read(buffer, byteCount, buffer.length - byteCount);
                    if (read < 0) {
                        endOfStream = true;
                        break;
                    }
                    if (read == 0) {
                        // InputStream implementations should not return zero for a non-empty request,
                        // but handling it here prevents a busy loop around a broken network wrapper.
                        int singleByte = stream.read();
                        if (singleByte < 0) {
                            endOfStream = true;
                            break;
                        }
                        buffer[byteCount++] = (byte) singleByte;
                    } else {
                        byteCount += read;
                    }
                }

                if (byteCount > 0) {
                    byte[] data = byteCount == buffer.length ? buffer : Arrays.copyOf(buffer, byteCount);
                    if (!this.appendBuffer(data)) {
                        break;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (!this.closed) {
                InterruptedIOException interrupted = new InterruptedIOException("Asynchronous stream reader was interrupted");
                interrupted.initCause(e);
                problem = interrupted;
            }
        } catch (IOException e) {
            if (!this.closed) {
                problem = e;
            }
        } catch (Throwable t) {
            if (!this.closed) {
                problem = new IOException("Failed to read asynchronous stream", t);
            }
        } finally {
            this.sourceStream = null;
            this.finishReading(problem);
        }
    }

    /**
     * Appends a data chunk to the internal queue, blocking if the queue
     * has reached {@link #maxBuffers} capacity.  Signals {@link #notEmpty}
     * to wake any consumer waiting for data, and completes
     * {@link #initialWait} once enough chunks are buffered.
     *
     * @return {@code false} if the stream has been closed
     */
    private boolean appendBuffer(byte[] data) throws InterruptedException {
        this.lock.lockInterruptibly();
        try {
            while (!this.closed && this.readBytes.size() >= this.maxBuffers) {
                this.notFull.await();
            }
            if (this.closed) {
                return false;
            }

            this.readBytes.addLast(data);
            if (this.readBytes.size() >= this.initialBuffers) {
                this.initialWait.complete(null);
            }
            this.notEmpty.signalAll();
            return true;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Marks the producer as finished (real EOF or error) and wakes all
     * waiting threads.  If the producer failed during initial buffering
     * the error is passed through {@link #initialWait} so the constructor
     * can throw it.
     *
     * @param problem the I/O error, or {@code null} for a clean EOF
     */
    private void finishReading(IOException problem) {
        this.lock.lock();
        try {
            this.eof = true;
            this.failure = problem;
            if (!this.initialWait.isDone()) {
                if (problem != null) {
                    this.initialWait.completeExceptionally(problem);
                } else {
                    // A finite stream may legitimately contain fewer than initialBuffers chunks.
                    this.initialWait.complete(null);
                }
            }
            this.notEmpty.signalAll();
            this.notFull.signalAll();
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Selects the next buffered chunk. An empty queue is not EOF while the producer is still alive:
     * readers wait for data, a real EOF, a failure, or close().
     */
    private boolean ensureBuffer() throws IOException {
        if (!this.closed && this.currentData != null && this.pointer < this.currentData.length) {
            return true;
        }

        try {
            this.lock.lockInterruptibly();
            try {
                this.currentData = null;
                this.pointer = 0;

                while (!this.closed && this.readBytes.isEmpty() && !this.eof && this.failure == null) {
                    this.notEmpty.await();
                }

                if (this.closed) {
                    return false;
                }
                if (!this.readBytes.isEmpty()) {
                    this.currentData = this.readBytes.removeFirst();
                    this.notFull.signalAll();
                    return true;
                }
                if (this.failure != null) {
                    throw this.failure;
                }
                return false; // The producer reached true EOF and no buffered data remains.
            } finally {
                this.lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            InterruptedIOException interrupted = new InterruptedIOException("Interrupted while waiting for streamed data");
            interrupted.initCause(e);
            throw interrupted;
        }
    }

    @Override
    public int read() throws IOException {
        if (!this.ensureBuffer()) {
            return -1;
        }
        return this.currentData[this.pointer++] & 0xFF;
    }

    /**
     * Returns at most one internal chunk per call.  The old implementation
     * used a while loop that copied every chunk to the same destination
     * offset {@code off}, overwriting previously read bytes.  Returning a
     * single chunk per call follows {@link InputStream}'s contract.
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        Objects.requireNonNull(b, "b");
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        if (!this.ensureBuffer()) {
            return -1;
        }

        int readSize = Math.min(this.currentData.length - this.pointer, len);
        System.arraycopy(this.currentData, this.pointer, b, off, readSize);
        this.pointer += readSize;
        return readSize;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0 || !this.ensureBuffer()) {
            return 0;
        }

        int skipped = (int) Math.min((long) (this.currentData.length - this.pointer), n);
        this.pointer += skipped;
        return skipped;
    }

    /**
     * Returns the total bytes available across the current chunk and all
     * queued chunks, clamped to {@link Integer#MAX_VALUE}.
     */
    @Override
    public int available() {
        if (this.closed) {
            return 0;
        }

        this.lock.lock();
        try {
            long available = this.currentData == null ? 0 : this.currentData.length - this.pointer;
            for (byte[] data : this.readBytes) {
                available += data.length;
                if (available >= Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
            }
            return (int) available;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Closes the underlying source stream to unblock the producer thread
     * (which may be stuck in a network read), wakes all waiting consumers,
     * and cancels the reader future.  Does not block on producer exit.
     */
    @Override
    public void close() throws IOException {
        InputStream stream;
        this.lock.lock();
        try {
            if (this.closed) {
                return;
            }
            this.closed = true;
            this.currentData = null;
            this.pointer = 0;
            this.readBytes.clear();
            stream = this.sourceStream;
            this.initialWait.complete(null);
            this.notEmpty.signalAll();
            this.notFull.signalAll();
        } finally {
            this.lock.unlock();
        }

        IOException closeFailure = null;
        if (stream != null) {
            try {
                stream.close(); // Unblock a producer currently stuck in the network read.
            } catch (IOException e) {
                closeFailure = e;
            }
        }
        this.readFuture.cancel(true); // Do not wait forever for a misbehaving source implementation.
        if (closeFailure != null) {
            throw closeFailure;
        }
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
