package gg.moonflower.etched.api.util;

import net.sourceforge.jaad.SampleBuffer;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.adts.ADTSDemultiplexer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;

/**
 * Dynamically converts AAC data to raw audio as the stream is read. Supports ADTS frames (.aac, .aacp radio streams)
 * and fragmented MP4 (HLS audio, such as SoundCloud).
 */
public class AacInputStream extends InputStream {

    private static final int PROBE_SIZE = 4096; // Less than the 8192 mark used when detecting formats
    private static final int ADTS_HEADER_SIZE = 7;
    private static final int MAX_CORRUPT_FRAMES = 10; // Live streams can contain a few broken frames, so skip them instead of stopping

    private final InputStream source;
    private final FrameSource frames;
    private final Decoder decoder;
    private final SampleBuffer sampleBuffer;
    private ByteBuffer buffer;

    private AudioFormat format;

    public AacInputStream(InputStream source) throws IOException {
        // Only probe a small amount of data so callers can still reset the stream and try another format
        byte[] probe = source.readNBytes(PROBE_SIZE);
        InputStream stream = new SequenceInputStream(new ByteArrayInputStream(probe), source);
        if (FragmentedMp4Reader.isMp4(probe)) {
            FragmentedMp4Reader reader = new FragmentedMp4Reader(stream);
            this.frames = reader::nextFrame;
            this.decoder = Decoder.create(reader.getDecoderSpecificInfo());
        } else if (isAdts(probe)) {
            ADTSDemultiplexer demultiplexer = new ADTSDemultiplexer(stream);
            this.frames = () -> {
                try {
                    return demultiplexer.readNextFrame();
                } catch (EOFException e) {
                    return null;
                }
            };
            this.decoder = Decoder.create(demultiplexer.getDecoderInfo());
        } else {
            throw new IOException("Failed to find ADTS or MP4 header");
        }

        this.source = source;
        this.sampleBuffer = new SampleBuffer();
        this.sampleBuffer.setBigEndian(false);
        this.buffer = ByteBuffer.allocate(0);

        if (this.fillBuffer()) {
            throw new IOException("Failed to find frame");
        }
    }

    /**
     * Checks for two consecutive ADTS frame headers. MP3 frames can never match since they never use layer 0.
     *
     * @param data The start of the stream
     * @return Whether the data contains ADTS frames
     */
    private static boolean isAdts(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            int frameLength = getAdtsFrameLength(data, i);
            if (frameLength > 0 && getAdtsFrameLength(data, i + frameLength) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return The length of the ADTS frame starting at the offset or <code>-1</code> if there is no valid header there
     */
    private static int getAdtsFrameLength(byte[] data, int offset) {
        if (offset + ADTS_HEADER_SIZE > data.length) {
            return -1;
        }
        if ((data[offset] & 0xFF) != 0xFF || (data[offset + 1] & 0xF6) != 0xF0) { // Sync word and layer 0
            return -1;
        }
        if (((data[offset + 2] >> 2) & 0x0F) > 12) { // Invalid sample rate index
            return -1;
        }
        int frameLength = ((data[offset + 3] & 0x03) << 11) | ((data[offset + 4] & 0xFF) << 3) | ((data[offset + 5] & 0xE0) >> 5);
        return frameLength >= ADTS_HEADER_SIZE ? frameLength : -1;
    }

    /**
     * Refills the buffer from the aac decoder.
     *
     * @return Whether the stream has reached EOF (no more frames to read)
     * @throws IOException If any error occurs while decoding aac data
     */
    private boolean fillBuffer() throws IOException {
        int corruptFrames = 0;
        while (true) {
            byte[] frame = this.frames.next();
            if (frame == null) {
                return true;
            }

            try {
                this.decoder.decodeFrame(frame, this.sampleBuffer);
            } catch (RuntimeException e) { // Corrupt frames can fail with any exception inside the decoder
                if (this.format == null || ++corruptFrames > MAX_CORRUPT_FRAMES) {
                    throw new IOException(e);
                }
                continue;
            }

            if (this.format == null) {
                this.format = new AudioFormat(this.sampleBuffer.getSampleRate(), this.sampleBuffer.getBitsPerSample(), this.sampleBuffer.getChannels(), true, false);
            }

            this.buffer = ByteBuffer.wrap(this.sampleBuffer.getData());
            return false;
        }
    }

    @Override
    public int read() throws IOException {
        if (!this.buffer.hasRemaining() && this.fillBuffer()) {
            return -1;
        }
        return ((int) this.buffer.get()) & 0xFF;
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        int read = 0;
        while (read < len) {
            if (!this.buffer.hasRemaining() && this.fillBuffer()) {
                return read > 0 ? read : -1;
            }

            int readLength = Math.min(this.buffer.remaining(), len - read);
            this.buffer.get(b, off + read, readLength);
            read += readLength;
        }

        return read;
    }

    @Override
    public int available() {
        return this.buffer.remaining();
    }

    @Override
    public void close() throws IOException {
        this.source.close();
    }

    public AudioFormat getFormat() {
        return this.format;
    }

    @FunctionalInterface
    private interface FrameSource {

        /**
         * @return The next AAC frame or <code>null</code> at the end of the stream
         */
        byte @Nullable [] next() throws IOException;
    }
}
