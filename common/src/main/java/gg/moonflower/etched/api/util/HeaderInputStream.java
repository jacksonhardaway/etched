package gg.moonflower.etched.api.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Used to buffer data before it needs to be used
 *
 * @author Ocelot
 */
public class HeaderInputStream extends InputStream implements SeekingStream {

    private final byte[] header;
    private final InputStream source;
    private int position;
    private int mark;

    public HeaderInputStream(byte[] header, InputStream source) {
        this.header = header;
        this.source = source;
    }

    @Override
    public void beginning() throws IOException {
        if (this.position > this.header.length)
            throw new IOException("Stream has already passed header (position: " + this.position + ", length: " + this.header.length + "). Can no longer go to beginning");
        this.position = 0;
    }

    @Override
    public int read() throws IOException {
        if (this.position < this.header.length) {
            int value = this.header[this.position];
            this.position++;
            return value & 0xff;
        }
        this.position++;
        return this.source.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (this.position < this.header.length) {
            int readLength = Math.min(this.header.length - this.position - 1, len);
            System.arraycopy(this.header, this.position, b, off, readLength);
            this.position += readLength;
            if (len == readLength)
                return readLength;

            int read = this.source.read(b, off + readLength, len - readLength);
            if (read != -1) {
                this.position += read;
                return read + readLength;
            }
            return -1;
        }

        int read = this.source.read(b, off, len);
        if (read != -1)
            this.position += read;
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        if (this.position < this.header.length) {
            long skipLength = Math.min(this.header.length - this.position, n);
            this.position += skipLength;
            if (n == skipLength)
                return skipLength;

            return this.source.skip(n - skipLength) + skipLength;
        }

        long skipped = this.source.skip(n);
        this.position += skipped;
        return this.source.skip(n);
    }

    @Override
    public void close() throws IOException {
        this.source.close();
    }
}
