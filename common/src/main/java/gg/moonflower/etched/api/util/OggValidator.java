package gg.moonflower.etched.api.util;

import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.stb.STBVorbis.*;

public class OggValidator {

    public static void validate(InputStream stream) throws IOException {
        long handle = 0L;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer buffer = memoryStack.malloc(32);
            IntBuffer intBuffer = memoryStack.mallocInt(1);
            IntBuffer intBuffer2 = memoryStack.mallocInt(1);

            while (handle == 0L) {
                if (!fillFromStream(stream, buffer))
                    throw new IOException("Failed to find Ogg header");

                handle = stb_vorbis_open_pushdata(buffer, intBuffer, intBuffer2, null);
                int j = intBuffer2.get(0);
                if (j == VORBIS_need_more_data) {
                    buffer.position(0);
                    buffer.limit(0);
                } else if (j != 0) {
                    throw new IOException("Failed to read Ogg file: " + j);
                }
            }
        } finally {
            if (handle != 0L)
                stb_vorbis_close(handle);
        }
    }

    private static boolean fillFromStream(InputStream stream, ByteBuffer buffer) throws IOException {
        int i = buffer.limit();
        int j = buffer.capacity() - i;
        if (j == 0) {
            return true;
        } else {
            byte[] bs = new byte[j];
            int k = stream.read(bs);
            if (k == -1) {
                return false;
            } else {
                int l = buffer.position();
                buffer.limit(i + k);
                buffer.position(i);
                buffer.put(bs, 0, k);
                buffer.position(l);
                return true;
            }
        }
    }
}
