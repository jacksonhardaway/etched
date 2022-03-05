package gg.moonflower.etched.api.util;

import javazoom.jl.decoder.*;
import org.jetbrains.annotations.NotNull;

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

    private final InputStream source;
    private final Bitstream stream;
    private final Decoder decoder;
    private final ByteBuffer buffer;

    private SampleBuffer output;
    private AudioFormat format;

    public Mp3InputStream(InputStream source) throws IOException {
        this.source = source;
        this.stream = new Bitstream(source);
        this.decoder = new Decoder();
        this.buffer = ByteBuffer.allocate(Short.BYTES * Obuffer.OBUFFERSIZE).order(ByteOrder.LITTLE_ENDIAN);
        if (this.fillBuffer())
            throw new IOException("Failed to find header");
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
            if (header == null) // EOF
                return true;

            if (this.output == null) {
                int channels = header.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
                this.output = new SampleBuffer(header.sample_frequency(), channels);
                this.decoder.setOutputBuffer(this.output);
                this.format = new AudioFormat(header.frequency(), 16, channels, true, false);
            }

            Obuffer decoderOutput = this.decoder.decodeFrame(header, this.stream);
            if (decoderOutput != this.output)
                throw new IOException("Output buffers are different.");

            for (short value : this.output.getBuffer())
                this.buffer.putShort(value);
            this.buffer.flip();
        } catch (JavaLayerException e) {
            throw new IOException(e);
        }

        this.stream.closeFrame();
        return false;
    }

    @Override
    public int read() throws IOException {
        if (!this.buffer.hasRemaining() && this.fillBuffer())
            return -1;
        return ((int) this.buffer.get()) & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int readAmount = 0;

        boolean eof = false;
        while (readAmount < len && (this.buffer.hasRemaining() || !(eof = this.fillBuffer()))) {
            int readLength = Math.min(this.buffer.remaining(), len - readAmount);
            this.buffer.get(b, off + readAmount, readLength);
            readAmount += readLength;
        }

        return eof ? -1 : readAmount;
    }

    @Override
    public void close() throws IOException {
        this.source.close();
    }

    public AudioFormat getFormat() {
        return format;
    }
}
