package me.jaackson.etched.client.sound.download;

import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <p>Skips the second track of a stereo track to read as mono.</p>
 *
 * @author Ocelot
 */
public class MonoWrapper implements AudioStream {

    private final AudioStream source;
    private final AudioFormat format;
    private final int sourceChannels;

    public MonoWrapper(AudioStream source) {
        this.source = source;
        AudioFormat sourceFormat = source.getFormat();
        this.sourceChannels = sourceFormat.getChannels();
        this.format = this.sourceChannels != 1 ? new AudioFormat(sourceFormat.getEncoding(), sourceFormat.getSampleRate(), sourceFormat.getSampleSizeInBits(), 1, sourceFormat.getFrameSize() / sourceFormat.getChannels(), sourceFormat.getFrameRate(), sourceFormat.isBigEndian()) : sourceFormat;
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public ByteBuffer read(int amount) throws IOException {
        ByteBuffer parent = this.source.read(amount * this.sourceChannels);
        if (this.sourceChannels == 1)
            return parent;

        ByteBuffer modified = BufferUtils.createByteBuffer(parent.limit() / this.sourceChannels);
        int step = this.format.getSampleSizeInBits() / Byte.SIZE;
        for (int j = 0; j < parent.limit(); j += step * 2)
            for (int l = 0; l < step; l++)
                modified.put(parent.get(j + l));
        modified.rewind();
        return modified;
    }

    @Override
    public void close() throws IOException {
        this.source.close();
    }
}
