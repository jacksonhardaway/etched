package me.jaackson.etched.client.sound.download;

import net.minecraft.client.sounds.AudioStream;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

/**
 * <p>Hack because Minecraft doesn't properly handle sound exceptions.</p>
 *
 * @author Ocelot
 */
public enum EmptyAudioStream implements AudioStream {

    INSTANCE;

    private static final AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 22050, 8, 1, 4, 1, true);

    @Override
    public AudioFormat getFormat() {
        return FORMAT;
    }

    @Override
    public ByteBuffer read(int i) {
        return ByteBuffer.allocateDirect(0);
    }

    @Override
    public void close() {
    }
}
