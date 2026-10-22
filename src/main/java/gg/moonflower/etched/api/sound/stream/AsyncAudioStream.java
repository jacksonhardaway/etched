package gg.moonflower.etched.api.sound.stream;

import net.minecraft.client.sounds.AudioStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Decodes another audio stream on a separate thread so reads never wait for the network. Minecraft reads streams on the sound thread
 * and the main thread waits for the sound thread whenever any sound is played, so a blocking read would freeze the game.
 * If the decoder falls behind, silence is played until it catches up.
 *
 * @author Ocelot
 */
public class AsyncAudioStream implements AudioStream {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final float CHUNK_SECONDS = 0.25F;
    private static final float PREBUFFER_SECONDS = 5.0F; // Minecraft queues 4 seconds as soon as the sound starts
    private static final float MAX_BUFFERED_SECONDS = 20.0F;
    private static final long MAX_PREBUFFER_WAIT_NANOS = TimeUnit.SECONDS.toNanos(10);

    private final AudioStream source;
    private final AudioFormat format;
    private final int frameSize;
    private final int maxBufferedBytes;
    private final Deque<byte[]> buffers;
    private final Lock lock;
    private final Condition changed;
    private int bufferedBytes;
    private int pointer;
    private boolean finished;
    private IOException error;
    private volatile boolean closed;

    public AsyncAudioStream(AudioStream source, Executor executor) {
        this.source = source;
        this.format = source.getFormat();
        this.frameSize = Math.max(1, this.format.getFrameSize());
        this.maxBufferedBytes = this.secondsToBytes(MAX_BUFFERED_SECONDS);
        this.buffers = new ArrayDeque<>();
        this.lock = new ReentrantLock();
        this.changed = this.lock.newCondition();

        CompletableFuture.runAsync(this::decode, executor);

        // Fill the initial buffer on the calling thread, which is never the sound thread
        int prebufferBytes = this.secondsToBytes(PREBUFFER_SECONDS);
        try {
            this.lock.lock();
            long remainingNanos = MAX_PREBUFFER_WAIT_NANOS;
            while (this.bufferedBytes < prebufferBytes && !this.finished && remainingNanos > 0) {
                remainingNanos = this.changed.awaitNanos(remainingNanos);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            this.lock.unlock();
        }
    }

    private int secondsToBytes(float seconds) {
        return Math.max(1, (int) (this.format.getSampleRate() * seconds)) * this.frameSize;
    }

    private void decode() {
        int chunkSize = this.secondsToBytes(CHUNK_SECONDS);
        try {
            while (!this.closed) {
                ByteBuffer data = this.source.read(chunkSize);
                if (data == null || !data.hasRemaining()) {
                    break;
                }

                byte[] bytes = new byte[data.remaining()];
                data.get(bytes);

                try {
                    this.lock.lock();
                    while (this.bufferedBytes >= this.maxBufferedBytes && !this.closed) {
                        this.changed.await();
                    }
                    this.buffers.add(bytes);
                    this.bufferedBytes += bytes.length;
                    this.changed.signalAll();
                } finally {
                    this.lock.unlock();
                }
            }
        } catch (IOException e) {
            this.error = e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            this.error = new IOException(t);
        } finally {
            try {
                this.lock.lock();
                this.finished = true;
                this.changed.signalAll();
            } finally {
                this.lock.unlock();
            }

            try {
                this.source.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close audio stream", e);
            }
        }
    }

    @Override
    public AudioFormat getFormat() {
        return this.format;
    }

    @Override
    public ByteBuffer read(int amount) throws IOException {
        try {
            this.lock.lock();
            if (this.finished && this.bufferedBytes == 0) {
                if (this.error != null) {
                    IOException error = this.error;
                    this.error = null; // Only report the failure once
                    throw error;
                }
                return BufferUtils.createByteBuffer(0);
            }

            // Only take whole frames unless this is the end of the stream, otherwise padding would misalign the samples after it
            int length = Math.min(amount, this.bufferedBytes);
            if (!this.finished) {
                length -= length % this.frameSize;
            }

            int size = this.finished ? length : amount;
            ByteBuffer result = BufferUtils.createByteBuffer(size);
            int remaining = length;
            while (remaining > 0) {
                byte[] buffer = this.buffers.getFirst();
                int count = Math.min(buffer.length - this.pointer, remaining);
                result.put(buffer, this.pointer, count);
                this.pointer += count;
                remaining -= count;
                if (this.pointer >= buffer.length) {
                    this.buffers.removeFirst();
                    this.pointer = 0;
                }
            }
            this.bufferedBytes -= length;
            this.changed.signalAll();

            // The decoder fell behind, so fill the rest with silence instead of waiting for it
            byte silence = AudioFormat.Encoding.PCM_UNSIGNED.equals(this.format.getEncoding()) ? (byte) 0x80 : 0;
            while (result.hasRemaining()) {
                result.put(silence);
            }

            result.rewind();
            return result;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public void close() {
        this.closed = true;
        try {
            this.lock.lock();
            this.changed.signalAll();
        } finally {
            this.lock.unlock();
        }
        // The decode thread closes the source once it sees the closed flag. Closing it here could block the sound thread on the network
    }
}
