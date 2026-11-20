package gg.moonflower.etched.api.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FragmentedMp4ReaderTest {

    @Test
    void readsAacFramesFromFragmentedMp4() throws Exception {
        byte[] data = AacTestFixtures.fragmentedMp4();
        assertTrue(FragmentedMp4Reader.isMp4(data));

        FragmentedMp4Reader reader = new FragmentedMp4Reader(new ByteArrayInputStream(data));
        assertTrue(reader.getDecoderSpecificInfo().length > 0);

        int frameCount = 0;
        while (reader.nextFrame() != null) {
            frameCount++;
        }
        assertTrue(frameCount > 1);
    }

    @Test
    void rejectsTruncatedMediaData() throws Exception {
        byte[] data = AacTestFixtures.fragmentedMp4();
        FragmentedMp4Reader reader = new FragmentedMp4Reader(new ByteArrayInputStream(Arrays.copyOf(data, data.length - 300)));

        assertThrows(IOException.class, () -> {
            while (reader.nextFrame() != null) {
                // Consume all declared frames.
            }
        });
    }

    @Test
    void rejectsNonMp4Data() {
        assertFalse(FragmentedMp4Reader.isMp4(new byte[]{'M', 'Z', 0, 0}));
        assertThrows(IOException.class, () -> new FragmentedMp4Reader(new ByteArrayInputStream(new byte[]{0, 0, 0, 8, 'f', 'r', 'e', 'e'})));
    }
}
