package gg.moonflower.etched.api.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class AsyncInputStreamTest {

    @Test
    void doesNotReportEndOfStreamBetweenProducerBuffers() throws Exception {
        byte[] expected = new byte[128 * 1024];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (byte) i;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (AsyncInputStream input = new AsyncInputStream(
                () -> new ChunkedInputStream(expected, 257), 8192, 8, executor)) {
            assertArrayEquals(expected, input.readAllBytes());
        } finally {
            executor.shutdownNow();
        }
    }

    private static final class ChunkedInputStream extends ByteArrayInputStream {

        private final int chunkSize;

        private ChunkedInputStream(byte[] data, int chunkSize) {
            super(data);
            this.chunkSize = chunkSize;
        }

        @Override
        public synchronized int read(byte[] buffer, int offset, int length) {
            return super.read(buffer, offset, Math.min(length, this.chunkSize));
        }
    }
}
