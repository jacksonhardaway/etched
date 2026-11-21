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
 * Decodes ADTS AAC and AAC frames stored in a fragmented MP4 stream.
 */
public class AacInputStream extends InputStream {

    private static final int PROBE_SIZE = 4096;
    private static final int ADTS_HEADER_SIZE = 7;
    private static final int MAX_CONSECUTIVE_CORRUPT_FRAMES = 10;

    private final InputStream source;
    private final FrameSource frames;
    private final Decoder decoder;
    private final SampleBuffer sampleBuffer;
    private ByteBuffer buffer;
    private AudioFormat format;

    public AacInputStream(InputStream source) throws IOException {
        byte[] probe = source.readNBytes(PROBE_SIZE);
        InputStream replayableSource = new SequenceInputStream(new ByteArrayInputStream(probe), source);

        if (FragmentedMp4Reader.isMp4(probe)) {
            FragmentedMp4Reader reader = new FragmentedMp4Reader(replayableSource);
            this.frames = reader::nextFrame;
            this.decoder = Decoder.create(reader.getDecoderSpecificInfo());
        } else if (isAdts(probe)) {
            ADTSDemultiplexer demultiplexer = new ADTSDemultiplexer(replayableSource);
            this.frames = () -> {
                try {
                    return demultiplexer.readNextFrame();
                } catch (EOFException exception) {
                    return null;
                }
            };
            this.decoder = Decoder.create(demultiplexer.getDecoderInfo());
        } else {
            throw new IOException("Failed to find an ADTS or fragmented MP4 AAC header");
        }

        this.source = source;
        this.sampleBuffer = new SampleBuffer();
        this.sampleBuffer.setBigEndian(false);
        this.buffer = ByteBuffer.allocate(0);

        if (this.fillBuffer()) {
            throw new IOException("Failed to find an AAC frame");
        }
    }

    static boolean isAdts(byte[] data) {
        for (int offset = 0; offset < data.length; offset++) {
            int frameLength = getAdtsFrameLength(data, offset);
            if (frameLength > 0 && getAdtsFrameLength(data, offset + frameLength) > 0) {
                return true;
            }
        }
        return false;
    }

    private static int getAdtsFrameLength(byte[] data, int offset) {
        if (offset < 0 || offset + ADTS_HEADER_SIZE > data.length) {
            return -1;
        }
        if (Byte.toUnsignedInt(data[offset]) != 0xFF || (Byte.toUnsignedInt(data[offset + 1]) & 0xF6) != 0xF0) {
            return -1;
        }
        if (((Byte.toUnsignedInt(data[offset + 2]) >> 2) & 0x0F) > 12) {
            return -1;
        }

        int frameLength = ((Byte.toUnsignedInt(data[offset + 3]) & 0x03) << 11)
                | (Byte.toUnsignedInt(data[offset + 4]) << 3)
                | (Byte.toUnsignedInt(data[offset + 5]) >> 5);
        return frameLength >= ADTS_HEADER_SIZE ? frameLength : -1;
    }

    private boolean fillBuffer() throws IOException {
        int corruptFrames = 0;
        while (true) {
            byte[] frame = this.frames.next();
            if (frame == null) {
                return true;
            }

            try {
                this.decoder.decodeFrame(frame, this.sampleBuffer);
            } catch (RuntimeException exception) {
                if (this.format == null || ++corruptFrames > MAX_CONSECUTIVE_CORRUPT_FRAMES) {
                    throw new IOException("Failed to decode AAC frame", exception);
                }
                continue;
            }

            if (this.format == null) {
                this.format = new AudioFormat(
                        this.sampleBuffer.getSampleRate(),
                        this.sampleBuffer.getBitsPerSample(),
                        this.sampleBuffer.getChannels(),
                        true,
                        false
                );
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
        return Byte.toUnsignedInt(this.buffer.get());
    }

    @Override
    public int read(byte @NotNull [] destination, int offset, int length) throws IOException {
        int read = 0;
        while (read < length) {
            if (!this.buffer.hasRemaining() && this.fillBuffer()) {
                return read > 0 ? read : -1;
            }

            int chunkLength = Math.min(this.buffer.remaining(), length - read);
            this.buffer.get(destination, offset + read, chunkLength);
            read += chunkLength;
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

        byte @Nullable [] next() throws IOException;
    }
}
