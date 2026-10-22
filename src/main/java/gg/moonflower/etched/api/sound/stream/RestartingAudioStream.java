package gg.moonflower.etched.api.sound.stream;

import net.minecraft.client.sounds.AudioStream;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Loops a stream by opening it again once it ends. Unlike {@link net.minecraft.client.sounds.LoopingAudioStream}, this doesn't keep the whole stream in memory to replay it.
 *
 * @author Ocelot
 */
public class RestartingAudioStream implements AudioStream {

    private final AudioStreamSupplier supplier;
    private final AudioFormat format;
    private AudioStream stream;

    public RestartingAudioStream(AudioStream stream, AudioStreamSupplier supplier) {
        this.supplier = supplier;
        this.format = stream.getFormat();
        this.stream = stream;
    }

    @Override
    public AudioFormat getFormat() {
        return this.format;
    }

    @Override
    public ByteBuffer read(int amount) throws IOException {
        ByteBuffer result = this.stream.read(amount);
        if (result != null && result.hasRemaining()) {
            return result;
        }

        this.stream.close();
        this.stream = this.supplier.open();
        return this.stream.read(amount);
    }

    @Override
    public void close() throws IOException {
        this.stream.close();
    }

    @FunctionalInterface
    public interface AudioStreamSupplier {

        /**
         * @return A new stream reading from the start
         * @throws IOException If any error occurs
         */
        AudioStream open() throws IOException;
    }
}
