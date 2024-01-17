package gg.moonflower.etched.api.util;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Based on Java's Wave File Reader.
 *
 * @author Kara Kytle
 * @author Jan Borgersen
 * @author Florian Bomers
 */
public class WaveDataReader {

    private static final int RIFF_MAGIC = 1380533830;
    private static final int WAVE_MAGIC = 1463899717;
    private static final int FMT_MAGIC = 0x666d7420; // "fmt "
    private static final int DATA_MAGIC = 0x64617461; // "data"

    private static final int WAVE_FORMAT_PCM = 0x0001;
    private static final int WAVE_FORMAT_ALAW = 0x0006;
    private static final int WAVE_FORMAT_MULAW = 0x0007;

    public static AudioInputStream getAudioInputStream(InputStream stream) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat fileFormat = getFMT(stream);
        return new AudioInputStream(stream, fileFormat.getFormat(), fileFormat.getFrameLength());
    }

    private static AudioFileFormat getFMT(InputStream stream) throws UnsupportedAudioFileException, IOException {
        int nread = 0;
        int fmt;
        int length;
        int wav_type;
        short channels;
        long sampleRate;
        int sampleSizeInBits;
        AudioFormat.Encoding encoding;

        DataInputStream dis = new DataInputStream(stream);

        int magic = dis.readInt();
        rllong(dis);
        int waveMagic = dis.readInt();

        if ((magic != RIFF_MAGIC) || (waveMagic != WAVE_MAGIC)) {
            // not WAVE, throw UnsupportedAudioFileException
            throw new UnsupportedAudioFileException("not a WAVE file");
        }

        // find and read the "fmt" chunk
        // we break out of this loop either by hitting EOF or finding "fmt "
        while (true) {

            try {
                fmt = dis.readInt();
                nread += 4;
                if (fmt == FMT_MAGIC) {
                    // we've found the 'fmt' chunk
                    break;
                } else {
                    // else not 'fmt', skip this chunk
                    length = rllong(dis);
                    nread += 4;
                    if (length % 2 > 0) length++;
                    nread += dis.skipBytes(length);
                }
            } catch (EOFException eof) {
                // we've reached the end of the file without finding the 'fmt' chunk
                throw new UnsupportedAudioFileException("Not a valid WAV file");
            }
        }

        // Read the format chunk size.
        length = rllong(dis);
        nread += 4;

        // This is the nread position at the end of the format chunk
        int endLength = nread + length;

        // Read the wave format data out of the format chunk.

        // encoding.
        wav_type = rlshort(dis);
        nread += 2;

        if (wav_type == WAVE_FORMAT_PCM)
            encoding = AudioFormat.Encoding.PCM_SIGNED;  // if 8-bit, we need PCM_UNSIGNED, below...
        else if (wav_type == WAVE_FORMAT_ALAW)
            encoding = AudioFormat.Encoding.ALAW;
        else if (wav_type == WAVE_FORMAT_MULAW)
            encoding = AudioFormat.Encoding.ULAW;
        else {
            // we don't support any other WAVE formats....
            throw new UnsupportedAudioFileException("Not a supported WAV file");
        }
        // channels
        channels = rlshort(dis);
        nread += 2;
        if (channels <= 0) {
            throw new UnsupportedAudioFileException("Invalid number of channels");
        }

        // sample rate.
        sampleRate = rllong(dis);
        nread += 4;

        // this is the avgBytesPerSec
        rllong(dis);
        nread += 4;

        // this is blockAlign value
        rlshort(dis);
        nread += 2;

        // this is the PCM-specific value bitsPerSample
        sampleSizeInBits = rlshort(dis);
        nread += 2;
        if (sampleSizeInBits <= 0) {
            throw new UnsupportedAudioFileException("Invalid bitsPerSample");
        }

        // if sampleSizeInBits==8, we need to use PCM_UNSIGNED
        if ((sampleSizeInBits == 8) && encoding.equals(AudioFormat.Encoding.PCM_SIGNED))
            encoding = AudioFormat.Encoding.PCM_UNSIGNED;

        // skip any difference between the length of the format chunk
        // and what we read

        // if the length of the chunk is odd, there's an extra pad byte
        // at the end.  i've never seen this in the fmt chunk, but we
        // should check to make sure.

        // $$jb: 07.28.99: endLength>nread, not length>nread.
        //       This fixes #4257986
        if (endLength > nread)
            dis.skipBytes(endLength - nread);

        // we have a format now, so find the "data" chunk
        // we break out of this loop either by hitting EOF or finding "data"
        // $$kk: if "data" chunk precedes "fmt" chunk we are hosed -- can this legally happen?
        nread = 0;
        while (true) {
            try {
                int datahdr = dis.readInt();
                nread += 4;
                if (datahdr == DATA_MAGIC) {
                    // we've found the 'data' chunk
                    break;
                } else {
                    // else not 'data', skip this chunk
                    int thisLength = rllong(dis);
                    nread += 4;
                    if (thisLength % 2 > 0) thisLength++;
                    nread += dis.skipBytes(thisLength);
                }
            } catch (EOFException eof) {
                // we've reached the end of the file without finding the 'data' chunk
                throw new UnsupportedAudioFileException("Not a valid WAV file");
            }
        }
        // this is the length of the data chunk
        int dataLength = rllong(dis);

        // now build the new AudioFileFormat and return

        AudioFormat format = new AudioFormat(encoding,
                (float) sampleRate,
                sampleSizeInBits, channels,
                ((sampleSizeInBits + 7) / 8) * channels,
                (float) sampleRate, false);

        return new AudioFileFormat(AudioFileFormat.Type.WAVE,
                format,
                dataLength / format.getFrameSize());
    }

    private static int rllong(DataInputStream dis) throws IOException {
        int b1, b2, b3, b4;
        int i;

        i = dis.readInt();

        b1 = (i & 0xFF) << 24;
        b2 = (i & 0xFF00) << 8;
        b3 = (i & 0xFF0000) >> 8;
        b4 = (i & 0xFF000000) >>> 24;

        i = (b1 | b2 | b3 | b4);

        return i;
    }

    private static short rlshort(DataInputStream dis) throws IOException {
        short s;
        short high, low;

        s = dis.readShort();

        high = (short) ((s & 0xFF) << 8);
        low = (short) ((s & 0xFF00) >>> 8);

        s = (short) (high | low);

        return s;
    }
}
