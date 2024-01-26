package gg.moonflower.etched.api.util;

import javazoom.jl.decoder.*;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Dynamically converts mp3 data to raw audio as the stream is read.
 *
 * @author Ocelot
 */
public class Mp3InputStream extends InputStream {

    private final Bitstream stream;
    private final Decoder decoder;
    private final ByteBuffer buffer;

    private AudioFormat format;

    public Mp3InputStream(InputStream source) throws IOException {
        this.stream = new Bitstream(source);
        this.decoder = new Decoder();
        this.buffer = ByteBuffer.allocate(Short.BYTES * Obuffer.OBUFFERSIZE).order(ByteOrder.LITTLE_ENDIAN);
        if (this.fillBuffer()) {
            throw new IOException("Failed to find header");
        }
    }

    /**
     * Refills the buffer from the mp3 decoder.
     *
     * @return Whether the stream has reached EOF (no more headers to read)
     * @throws IOException If any error occurs while decoding mp3 data
     */
    private boolean fillBuffer() throws IOException {
        this.buffer.clear();

        try {
            Header header = this.stream.readFrame();
            if (header == null) { // EOF
                this.buffer.flip();
                return true;
            }

            if (this.format == null) {
                int channels = header.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
                this.format = new AudioFormat(header.frequency(), Short.SIZE, channels, true, false);
            }

            SampleBuffer decoderOutput = (SampleBuffer) this.decoder.decodeFrame(header, this.stream);
            short[] data = decoderOutput.getBuffer();
            this.buffer.asShortBuffer().put(data);
            this.buffer.position(data.length * Short.BYTES);
            this.buffer.flip();
        } catch (Throwable t) {
            throw new IOException(t);
        } finally {
            this.stream.closeFrame();
        }

        return false;
    }

    @Override
    public int read() throws IOException {
        if (!this.buffer.hasRemaining() && this.fillBuffer()) {
            return -1;
        }
        return ((int) this.buffer.get()) & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
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
        try {
            this.stream.close();
        } catch (JavaLayerException e) {
            throw new IOException(e);
        }
    }

    public AudioFormat getFormat() {
        return this.format;
    }
}
