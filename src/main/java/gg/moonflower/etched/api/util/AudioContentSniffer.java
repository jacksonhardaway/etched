package gg.moonflower.etched.api.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Rejects downloaded content that does not begin with a supported audio signature.
 */
public final class AudioContentSniffer {

    private static final int PROBE_SIZE = 64;

    private AudioContentSniffer() {
    }

    public static InputStream requireSupported(InputStream source) throws IOException {
        BufferedInputStream input = source instanceof BufferedInputStream buffered ? buffered : new BufferedInputStream(source);
        input.mark(PROBE_SIZE);
        byte[] header = input.readNBytes(PROBE_SIZE);
        input.reset();

        if (!isSupportedAudio(header)) {
            input.close();
            throw new IOException("Downloaded content is not a supported MP3, OGG, or WAV audio file");
        }
        return input;
    }

    static boolean isSupportedAudio(byte[] header) {
        return isOgg(header) || isWave(header) || isMp3(header);
    }

    private static boolean isOgg(byte[] header) {
        return header.length >= 4
                && header[0] == 'O'
                && header[1] == 'g'
                && header[2] == 'g'
                && header[3] == 'S';
    }

    private static boolean isWave(byte[] header) {
        return header.length >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'A'
                && header[10] == 'V'
                && header[11] == 'E';
    }

    private static boolean isMp3(byte[] header) {
        if (header.length >= 3 && header[0] == 'I' && header[1] == 'D' && header[2] == '3') {
            return true;
        }
        if (header.length < 4) {
            return false;
        }

        int first = Byte.toUnsignedInt(header[0]);
        int second = Byte.toUnsignedInt(header[1]);
        int third = Byte.toUnsignedInt(header[2]);
        int version = (second >> 3) & 0b11;
        int layer = (second >> 1) & 0b11;
        int bitrate = (third >> 4) & 0b1111;
        int sampleRate = (third >> 2) & 0b11;

        return first == 0xFF
                && (second & 0xE0) == 0xE0
                && version != 0b01
                && layer != 0b00
                && bitrate != 0b0000
                && bitrate != 0b1111
                && sampleRate != 0b11;
    }
}
