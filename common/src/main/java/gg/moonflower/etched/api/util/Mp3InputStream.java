package gg.moonflower.etched.api.util;

import javazoom.jl.decoder.*;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

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
        this.buffer = BufferUtils.createByteBuffer(Short.BYTES * Obuffer.OBUFFERSIZE);
        if (this.fillBuffer())
            throw new IOException("Failed to find header");
    }

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
        if (this.buffer.position() >= this.buffer.limit() && this.fillBuffer())
            return -1;
        return ((int) this.buffer.get()) & 0xFF;
    }

    @Override
    public void close() throws IOException {
        this.source.close();
    }

    public AudioFormat getFormat() {
        return format;
    }
}
