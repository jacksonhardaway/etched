package gg.moonflower.etched.api.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * An input stream that utilizes {@link FileChannel} to allow for marking and rewinding.
 *
 * @author Ocelot
 */
public class FileChannelInputStream extends InputStream implements SeekingStream {

    private final FileChannel channel;
    private long mark;

    public FileChannelInputStream(FileChannel channel) {
        this.channel = channel;
        this.mark = 0;
    }

    @Override
    public void beginning() throws IOException {
        this.channel.position(0L);
    }

    @Override
    public synchronized int read() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        int read = this.channel.read(buffer);
        buffer.flip();
        return read > 0 ? (buffer.get(0) & 0xff) : -1;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        ByteBuffer buffer = ByteBuffer.allocate(len);
        int read = this.channel.read(buffer);
        if (read == -1)
            return -1;

        buffer.flip();
        buffer.get(b, off, read);
        return read;
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        long startPos = this.channel.position();
        this.channel.position(Math.min(this.channel.position() + n, this.channel.size()));
        return this.channel.position() - startPos;
    }

    @Override
    public synchronized int available() throws IOException {
        return (int) (this.channel.size() - this.channel.position());
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public synchronized void mark(int readlimit) {
        try {
            this.mark = this.channel.position();
        } catch (IOException e) {
            this.mark = 0;
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        this.channel.position(this.mark);
    }

    @Override
    public void close() throws IOException {
        this.channel.close();
    }
}
