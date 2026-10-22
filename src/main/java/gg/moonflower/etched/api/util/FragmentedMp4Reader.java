package gg.moonflower.etched.api.util;

import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Reads AAC frames from a fragmented MP4 stream (an init segment followed by <code>moof</code>/<code>mdat</code> fragments), such as HLS audio.
 * The stream is read strictly forward, so it works on network streams.
 */
public class FragmentedMp4Reader {

    private static final int MAX_HEADER_BOX_SIZE = 1024 * 1024; // moov and moof are read into memory, so guard against corrupt sizes

    private final DataInputStream stream;
    private final Deque<Integer> sampleSizes;
    private byte[] decoderSpecificInfo;
    private long position;
    private long moofStart;
    private long dataOffset;
    private long mdatRemaining;

    public FragmentedMp4Reader(InputStream source) throws IOException {
        this.stream = new DataInputStream(source);
        this.sampleSizes = new ArrayDeque<>();
        this.dataOffset = -1;

        while (this.decoderSpecificInfo == null) { // The decoder config is in moov, which comes before any samples
            if (!this.readBox()) {
                throw new IOException("Failed to find AAC decoder config");
            }
        }
    }

    /**
     * Checks whether the data starts with an MP4 file type box.
     *
     * @param data The start of the stream
     * @return Whether the data looks like MP4
     */
    public static boolean isMp4(byte[] data) {
        return data.length >= 8 && data[4] == 'f' && data[5] == 't' && data[6] == 'y' && data[7] == 'p';
    }

    /**
     * @return The AudioSpecificConfig to create the AAC decoder with
     */
    public byte[] getDecoderSpecificInfo() {
        return this.decoderSpecificInfo;
    }

    /**
     * @return The next raw AAC frame or <code>null</code> if the stream has ended
     * @throws IOException If any error occurs while reading the stream
     */
    public byte @Nullable [] nextFrame() throws IOException {
        while (true) {
            if (this.mdatRemaining > 0 && !this.sampleSizes.isEmpty()) {
                int size = this.sampleSizes.removeFirst();
                if (size > this.mdatRemaining) {
                    throw new IOException("Sample is larger than the remaining media data");
                }
                byte[] frame = new byte[size];
                this.readFully(frame);
                this.mdatRemaining -= size;
                return frame;
            }

            if (this.mdatRemaining > 0) { // All known samples were read, so ignore the rest of the media data
                this.skip(this.mdatRemaining);
                this.mdatRemaining = 0;
            }

            if (!this.readBox()) {
                return null;
            }
        }
    }

    /**
     * Reads the next top-level box.
     *
     * @return <code>false</code> if the stream has ended
     */
    private boolean readBox() throws IOException {
        long boxStart = this.position;
        int header;
        try {
            header = this.readInt();
        } catch (EOFException e) {
            return false;
        }

        long size = header & 0xFFFFFFFFL;
        String type = this.readType();
        int headerSize = 8;
        if (size == 1) {
            size = this.readLong();
            headerSize = 16;
        }
        long payloadSize = size == 0 ? Long.MAX_VALUE : size - headerSize; // A size of 0 means the box extends to the end of the stream
        if (payloadSize < 0) {
            throw new IOException("Invalid size for box '" + type + "': " + size);
        }

        switch (type) {
            case "moov" -> {
                if (this.decoderSpecificInfo == null) {
                    this.decoderSpecificInfo = findDecoderSpecificInfo(ByteBuffer.wrap(this.readPayload(type, payloadSize)));
                } else {
                    this.skip(payloadSize);
                }
            }
            case "moof" -> {
                this.moofStart = boxStart;
                this.readFragment(ByteBuffer.wrap(this.readPayload(type, payloadSize)));
            }
            case "mdat" -> {
                this.mdatRemaining = payloadSize;
                if (this.dataOffset >= 0) { // Samples start at an offset from the moof box
                    long toSkip = this.moofStart + this.dataOffset - this.position;
                    if (toSkip > 0) {
                        this.skip(toSkip);
                        this.mdatRemaining -= toSkip;
                    }
                }
            }
            default -> this.skip(payloadSize);
        }
        return true;
    }

    private byte[] readPayload(String type, long size) throws IOException {
        if (size > MAX_HEADER_BOX_SIZE) {
            throw new IOException("Box '" + type + "' is too large: " + size);
        }
        byte[] data = new byte[(int) size];
        this.readFully(data);
        return data;
    }

    /**
     * Reads the sample sizes of the first track in a movie fragment.
     */
    private void readFragment(ByteBuffer moof) throws IOException {
        this.sampleSizes.clear();
        this.dataOffset = -1;

        ByteBuffer traf = findBox(moof, "traf");
        if (traf == null) {
            throw new IOException("Movie fragment has no track");
        }

        int defaultSampleSize = 0;
        ByteBuffer tfhd = findBox(traf.duplicate(), "tfhd");
        if (tfhd != null) {
            int flags = tfhd.getInt() & 0xFFFFFF;
            tfhd.getInt(); // track_ID
            if ((flags & 0x01) != 0) {
                tfhd.getLong(); // base_data_offset
            }
            if ((flags & 0x02) != 0) {
                tfhd.getInt(); // sample_description_index
            }
            if ((flags & 0x08) != 0) {
                tfhd.getInt(); // default_sample_duration
            }
            if ((flags & 0x10) != 0) {
                defaultSampleSize = tfhd.getInt();
            }
        }

        ByteBuffer trun = findBox(traf.duplicate(), "trun");
        if (trun == null) {
            throw new IOException("Track fragment has no samples");
        }
        int flags = trun.getInt() & 0xFFFFFF;
        int sampleCount = trun.getInt();
        if ((flags & 0x001) != 0) {
            this.dataOffset = trun.getInt();
        }
        if ((flags & 0x004) != 0) {
            trun.getInt(); // first_sample_flags
        }
        for (int i = 0; i < sampleCount; i++) {
            if ((flags & 0x100) != 0) {
                trun.getInt(); // sample_duration
            }
            this.sampleSizes.add((flags & 0x200) != 0 ? trun.getInt() : defaultSampleSize);
            if ((flags & 0x400) != 0) {
                trun.getInt(); // sample_flags
            }
            if ((flags & 0x800) != 0) {
                trun.getInt(); // sample_composition_time_offset
            }
        }
    }

    /**
     * Finds the first direct child box with the specified type.
     *
     * @return The payload of the box or <code>null</code> if there is no such box
     */
    private static @Nullable ByteBuffer findBox(ByteBuffer parent, String type) {
        while (parent.remaining() >= 8) {
            int start = parent.position();
            int size = parent.getInt();
            byte[] name = new byte[4];
            parent.get(name);
            if (size < 8 || start + size > parent.limit()) {
                return null;
            }
            parent.position(start + size);
            if (type.equals(new String(name, StandardCharsets.ISO_8859_1))) {
                return parent.slice(start + 8, size - 8);
            }
        }
        return null;
    }

    /**
     * Searches the movie box for the AudioSpecificConfig stored in the elementary stream descriptor.
     */
    private static byte[] findDecoderSpecificInfo(ByteBuffer moov) throws IOException {
        ByteBuffer esds = findEsds(moov);
        if (esds == null) {
            throw new IOException("No AAC track found");
        }

        esds.getInt(); // version and flags
        while (esds.hasRemaining()) {
            int tag = esds.get() & 0xFF;
            int length = readDescriptorLength(esds);
            switch (tag) {
                case 0x03 -> { // ES_Descriptor
                    esds.getShort(); // ES_ID
                    int flags = esds.get() & 0xFF;
                    if ((flags & 0x80) != 0) {
                        esds.getShort(); // dependsOn_ES_ID
                    }
                    if ((flags & 0x40) != 0) {
                        esds.position(esds.position() + (esds.get() & 0xFF)); // URL
                    }
                    if ((flags & 0x20) != 0) {
                        esds.getShort(); // OCR_ES_Id
                    }
                }
                case 0x04 -> esds.position(esds.position() + 13); // DecoderConfigDescriptor fields before its children
                case 0x05 -> { // DecoderSpecificInfo
                    byte[] config = new byte[length];
                    esds.get(config);
                    return config;
                }
                default -> esds.position(esds.position() + length);
            }
        }
        throw new IOException("No AAC decoder config found");
    }

    /**
     * Walks the box tree from moov to the esds box of the first AAC track.
     */
    private static @Nullable ByteBuffer findEsds(ByteBuffer moov) {
        ByteBuffer trak = findBox(moov, "trak");
        while (trak != null) {
            ByteBuffer stbl = path(trak.duplicate(), "mdia", "minf", "stbl");
            ByteBuffer stsd = stbl != null ? findBox(stbl, "stsd") : null;
            if (stsd != null) {
                stsd.position(8); // version, flags and entry_count
                ByteBuffer mp4a = findBox(stsd, "mp4a");
                if (mp4a != null && mp4a.remaining() > 28) {
                    mp4a.position(28); // Fields of the audio sample entry before its children
                    ByteBuffer esds = findBox(mp4a, "esds");
                    if (esds != null) {
                        return esds;
                    }
                }
            }
            trak = findBox(moov, "trak");
        }
        return null;
    }

    private static @Nullable ByteBuffer path(ByteBuffer box, String... types) {
        for (String type : types) {
            box = findBox(box, type);
            if (box == null) {
                return null;
            }
        }
        return box;
    }

    private static int readDescriptorLength(ByteBuffer buffer) {
        int length = 0;
        for (int i = 0; i < 4; i++) {
            int b = buffer.get() & 0xFF;
            length = (length << 7) | (b & 0x7F);
            if ((b & 0x80) == 0) {
                break;
            }
        }
        return length;
    }

    private int readInt() throws IOException {
        int value = this.stream.readInt();
        this.position += 4;
        return value;
    }

    private long readLong() throws IOException {
        long value = this.stream.readLong();
        this.position += 8;
        return value;
    }

    private String readType() throws IOException {
        byte[] type = new byte[4];
        this.readFully(type);
        return new String(type, StandardCharsets.ISO_8859_1);
    }

    private void readFully(byte[] data) throws IOException {
        this.stream.readFully(data);
        this.position += data.length;
    }

    /**
     * Skips data. Reaching the end of the stream is not an error since the next box read will report it.
     */
    private void skip(long count) throws IOException {
        while (count > 0) {
            long skipped = this.stream.skip(count);
            if (skipped <= 0) {
                if (this.stream.read() == -1) {
                    return;
                }
                skipped = 1;
            }
            count -= skipped;
            this.position += skipped;
        }
    }
}
