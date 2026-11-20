package gg.moonflower.etched.api.util;

import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Reads AAC samples from an audio-only fragmented MP4 stream in one forward pass.
 */
public final class FragmentedMp4Reader {

    private static final int MAX_HEADER_BOX_SIZE = 1024 * 1024;
    private static final int MAX_SAMPLES_PER_FRAGMENT = 65_536;
    private static final int MAX_SAMPLE_SIZE = 1024 * 1024;

    private final DataInputStream stream;
    private final Deque<Integer> sampleSizes = new ArrayDeque<>();
    private byte[] decoderSpecificInfo;
    private long position;
    private long moofStart;
    private long sampleDataPosition = -1;
    private long mdatRemaining;

    public FragmentedMp4Reader(InputStream source) throws IOException {
        this.stream = new DataInputStream(source);
        while (this.decoderSpecificInfo == null) {
            if (!this.readBox()) {
                throw new IOException("Failed to find an AAC decoder configuration in fragmented MP4 data");
            }
        }
    }

    public static boolean isMp4(byte[] data) {
        return data.length >= 8
                && data[4] == 'f'
                && data[5] == 't'
                && data[6] == 'y'
                && data[7] == 'p';
    }

    public byte[] getDecoderSpecificInfo() {
        return this.decoderSpecificInfo.clone();
    }

    public byte @Nullable [] nextFrame() throws IOException {
        while (true) {
            if (this.mdatRemaining > 0 && !this.sampleSizes.isEmpty()) {
                int size = this.sampleSizes.removeFirst();
                if (size > this.mdatRemaining) {
                    throw new IOException("AAC sample is larger than the remaining MP4 media data");
                }
                byte[] frame = new byte[size];
                this.readFully(frame);
                this.mdatRemaining -= size;
                return frame;
            }

            if (this.mdatRemaining > 0) {
                this.skip(this.mdatRemaining);
                this.mdatRemaining = 0;
            }

            if (!this.readBox()) {
                if (!this.sampleSizes.isEmpty()) {
                    throw new IOException("Fragmented MP4 ended before all declared AAC samples were found");
                }
                return null;
            }
        }
    }

    private boolean readBox() throws IOException {
        long boxStart = this.position;
        int sizeField;
        try {
            sizeField = this.readInt();
        } catch (EOFException exception) {
            return false;
        }

        long size = Integer.toUnsignedLong(sizeField);
        String type = this.readType();
        int headerSize = 8;
        if (size == 1) {
            size = this.readLong();
            headerSize = 16;
        }
        long payloadSize = size == 0 ? Long.MAX_VALUE : size - headerSize;
        if (payloadSize < 0) {
            throw new IOException("Invalid size for MP4 box '" + type + "': " + size);
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
            case "mdat" -> this.beginMediaData(payloadSize);
            default -> this.skip(payloadSize);
        }
        return true;
    }

    private void beginMediaData(long payloadSize) throws IOException {
        this.mdatRemaining = payloadSize;
        if (this.sampleDataPosition < 0) {
            return;
        }

        long toSkip = this.sampleDataPosition - this.position;
        if (toSkip < 0 || toSkip > this.mdatRemaining) {
            throw new IOException("Invalid AAC sample offset in fragmented MP4 data");
        }
        this.skip(toSkip);
        this.mdatRemaining -= toSkip;
    }

    private byte[] readPayload(String type, long size) throws IOException {
        if (size > MAX_HEADER_BOX_SIZE) {
            throw new IOException("MP4 box '" + type + "' is too large: " + size);
        }
        byte[] data = new byte[(int) size];
        this.readFully(data);
        return data;
    }

    private void readFragment(ByteBuffer moof) throws IOException {
        this.sampleSizes.clear();
        this.sampleDataPosition = -1;

        try {
            ByteBuffer traf = findBox(moof, "traf");
            if (traf == null) {
                throw new IOException("MP4 movie fragment has no track");
            }

            long baseDataOffset = this.moofStart;
            int defaultSampleSize = 0;
            ByteBuffer tfhd = findBox(traf.duplicate(), "tfhd");
            if (tfhd != null) {
                int flags = tfhd.getInt() & 0xFFFFFF;
                tfhd.getInt(); // track_ID
                if ((flags & 0x000001) != 0) {
                    baseDataOffset = tfhd.getLong();
                }
                if ((flags & 0x000002) != 0) {
                    tfhd.getInt(); // sample_description_index
                }
                if ((flags & 0x000008) != 0) {
                    tfhd.getInt(); // default_sample_duration
                }
                if ((flags & 0x000010) != 0) {
                    defaultSampleSize = tfhd.getInt();
                }
                if ((flags & 0x000020) != 0) {
                    tfhd.getInt(); // default_sample_flags
                }
            }

            ByteBuffer trun = findBox(traf.duplicate(), "trun");
            if (trun == null) {
                throw new IOException("MP4 track fragment has no samples");
            }
            int flags = trun.getInt() & 0xFFFFFF;
            int sampleCount = trun.getInt();
            if (sampleCount <= 0 || sampleCount > MAX_SAMPLES_PER_FRAGMENT) {
                throw new IOException("Invalid MP4 sample count: " + sampleCount);
            }
            if ((flags & 0x000001) != 0) {
                this.sampleDataPosition = baseDataOffset + trun.getInt();
            }
            if ((flags & 0x000004) != 0) {
                trun.getInt(); // first_sample_flags
            }

            for (int index = 0; index < sampleCount; index++) {
                if ((flags & 0x000100) != 0) {
                    trun.getInt(); // sample_duration
                }
                int sampleSize = (flags & 0x000200) != 0 ? trun.getInt() : defaultSampleSize;
                if (sampleSize <= 0 || sampleSize > MAX_SAMPLE_SIZE) {
                    throw new IOException("Invalid AAC sample size in fragmented MP4 data: " + sampleSize);
                }
                this.sampleSizes.add(sampleSize);
                if ((flags & 0x000400) != 0) {
                    trun.getInt(); // sample_flags
                }
                if ((flags & 0x000800) != 0) {
                    trun.getInt(); // sample_composition_time_offset
                }
            }
        } catch (BufferUnderflowException | IndexOutOfBoundsException exception) {
            throw new IOException("Truncated MP4 movie fragment", exception);
        }
    }

    private static @Nullable ByteBuffer findBox(ByteBuffer parent, String expectedType) {
        while (parent.remaining() >= 8) {
            int start = parent.position();
            long size = Integer.toUnsignedLong(parent.getInt());
            byte[] name = new byte[4];
            parent.get(name);
            int headerSize = 8;
            if (size == 1) {
                if (parent.remaining() < 8) {
                    return null;
                }
                size = parent.getLong();
                headerSize = 16;
            } else if (size == 0) {
                size = parent.limit() - start;
            }

            long endLong = start + size;
            if (size < headerSize || endLong > parent.limit() || endLong > Integer.MAX_VALUE) {
                return null;
            }
            int end = (int) endLong;
            int payloadStart = start + headerSize;
            parent.position(end);

            if (expectedType.equals(new String(name, StandardCharsets.ISO_8859_1))) {
                ByteBuffer payload = parent.duplicate();
                payload.position(payloadStart);
                payload.limit(end);
                return payload.slice();
            }
        }
        return null;
    }

    private static byte[] findDecoderSpecificInfo(ByteBuffer moov) throws IOException {
        ByteBuffer esds = findEsds(moov);
        if (esds == null) {
            throw new IOException("No AAC track was found in fragmented MP4 data");
        }

        try {
            esds.getInt(); // version and flags
            while (esds.hasRemaining()) {
                int tag = Byte.toUnsignedInt(esds.get());
                int length = readDescriptorLength(esds);
                if (length < 0 || length > esds.remaining()) {
                    throw new IOException("Invalid AAC descriptor length in fragmented MP4 data");
                }

                switch (tag) {
                    case 0x03 -> {
                        esds.getShort(); // ES_ID
                        int flags = Byte.toUnsignedInt(esds.get());
                        if ((flags & 0x80) != 0) {
                            esds.getShort();
                        }
                        if ((flags & 0x40) != 0) {
                            int urlLength = Byte.toUnsignedInt(esds.get());
                            esds.position(esds.position() + urlLength);
                        }
                        if ((flags & 0x20) != 0) {
                            esds.getShort();
                        }
                    }
                    case 0x04 -> esds.position(esds.position() + 13);
                    case 0x05 -> {
                        byte[] config = new byte[length];
                        esds.get(config);
                        return config;
                    }
                    default -> esds.position(esds.position() + length);
                }
            }
        } catch (BufferUnderflowException | IndexOutOfBoundsException exception) {
            throw new IOException("Truncated AAC descriptor in fragmented MP4 data", exception);
        }
        throw new IOException("No AAC decoder configuration was found in fragmented MP4 data");
    }

    private static @Nullable ByteBuffer findEsds(ByteBuffer moov) {
        ByteBuffer tracks = moov.duplicate();
        ByteBuffer trak;
        while ((trak = findBox(tracks, "trak")) != null) {
            ByteBuffer stbl = path(trak.duplicate(), "mdia", "minf", "stbl");
            ByteBuffer stsd = stbl != null ? findBox(stbl, "stsd") : null;
            if (stsd == null || stsd.remaining() < 8) {
                continue;
            }

            stsd.position(8); // version, flags and entry_count
            ByteBuffer mp4a = findBox(stsd, "mp4a");
            if (mp4a == null || mp4a.remaining() <= 28) {
                continue;
            }

            mp4a.position(28);
            ByteBuffer esds = findBox(mp4a, "esds");
            if (esds != null) {
                return esds;
            }
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

    private static int readDescriptorLength(ByteBuffer buffer) throws IOException {
        int length = 0;
        for (int index = 0; index < 4; index++) {
            if (!buffer.hasRemaining()) {
                throw new IOException("Truncated AAC descriptor length");
            }
            int value = Byte.toUnsignedInt(buffer.get());
            length = (length << 7) | (value & 0x7F);
            if ((value & 0x80) == 0) {
                return length;
            }
        }
        throw new IOException("AAC descriptor length is too long");
    }

    private int readInt() throws IOException {
        int value = this.stream.readInt();
        this.position += Integer.BYTES;
        return value;
    }

    private long readLong() throws IOException {
        long value = this.stream.readLong();
        this.position += Long.BYTES;
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
