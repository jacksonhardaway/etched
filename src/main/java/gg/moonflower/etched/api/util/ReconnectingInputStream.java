package gg.moonflower.etched.api.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Objects;

/**
 * Keeps a live source open by reconnecting when the server closes or resets
 * the current HTTP connection. Before the first audio byte is received,
 * failures are bounded so an invalid station does not hang loading forever.
 */
public final class ReconnectingInputStream extends InputStream {

    private static final int MAX_STARTUP_FAILURES = 3;
    private static final long INITIAL_RETRY_DELAY_MILLIS = 250L;
    private static final long MAX_RETRY_DELAY_MILLIS = 5000L;

    private final AsyncInputStream.InputStreamSupplier source;
    private final Object stateLock;

    private volatile boolean closed;
    private InputStream current;
    private boolean receivedData;
    private int startupFailures;
    private long retryDelayMillis;

    public ReconnectingInputStream(AsyncInputStream.InputStreamSupplier source) {
        this.source = Objects.requireNonNull(source, "source");
        this.stateLock = new Object();
        this.retryDelayMillis = INITIAL_RETRY_DELAY_MILLIS;
    }

    /**
     * Opens a new connection via {@link #source} on first access and
     * returns the cached instance for subsequent calls within the same
     * connection lifecycle.  After a reconnect the cache is cleared and
     * a new connection is opened on the next call.
     */
    private InputStream getCurrent() throws IOException {
        synchronized (this.stateLock) {
            if (this.closed) {
                throw new IOException("Stream closed");
            }
            if (this.current != null) {
                return this.current;
            }
        }

        InputStream opened = Objects.requireNonNull(this.source.get(), "source returned null");
        synchronized (this.stateLock) {
            if (this.closed) {
                opened.close();
                throw new IOException("Stream closed");
            }
            if (this.current == null) {
                this.current = opened;
                return opened;
            }
        }

        opened.close();
        synchronized (this.stateLock) {
            return this.current;
        }
    }

    /**
     * Records that at least one audio byte has been received from the
     * current connection.  Resets startup failure counters and retry
     * delay — the station URL is confirmed valid, so subsequent
     * reconnects are not bounded.
     */
    private void markDataReceived() {
        synchronized (this.stateLock) {
            this.receivedData = true;
            this.startupFailures = 0;
            this.retryDelayMillis = INITIAL_RETRY_DELAY_MILLIS;
        }
    }

    /**
     * Closes the current connection, sleeps with exponential backoff
     * (250ms → 500ms → … → 5s), then lets {@link #getCurrent} open
     * a fresh connection on the next read attempt.
     * Before the first audio byte has been received ({@link #receivedData}
     * is {@code false}), a maximum of {@value #MAX_STARTUP_FAILURES}
     * consecutive reconnect attempts are permitted — this prevents an
     * invalid URL from retrying forever during loading.
     */
    private void reconnect(IOException cause) throws IOException {
        InputStream stream;
        long delay;
        synchronized (this.stateLock) {
            stream = this.current;
            this.current = null;
            if (this.closed) {
                return;
            }

            if (!this.receivedData && ++this.startupFailures >= MAX_STARTUP_FAILURES) {
                IOException failure = new IOException("Live audio source failed before playback started", cause);
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException closeError) {
                        failure.addSuppressed(closeError);
                    }
                }
                throw failure;
            }

            delay = this.retryDelayMillis;
            this.retryDelayMillis = Math.min(MAX_RETRY_DELAY_MILLIS, this.retryDelayMillis * 2L);
        }

        if (stream != null) {
            try {
                stream.close();
            } catch (IOException closeError) {
                cause.addSuppressed(closeError);
            }
        }

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            InterruptedIOException interrupted = new InterruptedIOException("Interrupted while reconnecting live audio");
            interrupted.initCause(e);
            interrupted.addSuppressed(cause);
            throw interrupted;
        }
    }

    @Override
    public int read() throws IOException {
        byte[] one = new byte[1];
        int read = this.read(one, 0, 1);
        return read < 0 ? -1 : one[0] & 0xFF;
    }

    /**
     * Reads from the current connection, transparently reconnecting when
     * the server closes or resets the HTTP stream.  Returns {@code -1}
     * only when the stream is explicitly {@linkplain #close() closed}
     * — an infinite live source never signals EOF on its own.
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }

        while (!this.closed) {
            IOException disconnectCause;
            try {
                int read = this.getCurrent().read(b, off, len);
                if (read > 0) {
                    this.markDataReceived();
                    return read;
                }
                if (read == 0) {
                    Thread.onSpinWait();
                    continue;
                }
                disconnectCause = new EOFException("Live audio server closed the connection");
            } catch (IOException e) {
                disconnectCause = e;
            }

            if (this.closed) {
                return -1;
            }
            this.reconnect(disconnectCause);
        }
        return -1;
    }

    @Override
    public int available() throws IOException {
        synchronized (this.stateLock) {
            return this.current == null ? 0 : this.current.available();
        }
    }

    /**
     * Marks the stream as closed and closes any open underlying
     * connection.  Idempotent — subsequent calls are ignored.
     */
    @Override
    public void close() throws IOException {
        InputStream stream;
        synchronized (this.stateLock) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            stream = this.current;
            this.current = null;
        }
        if (stream != null) {
            stream.close();
        }
    }
}
