package gg.moonflower.etched.api.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AacInputStreamTest {

    @Test
    void decodesAdtsAacToPcm() throws Exception {
        try (AacInputStream input = new AacInputStream(new ByteArrayInputStream(AacTestFixtures.adts()))) {
            assertEquals(44_100, input.getFormat().getSampleRate());
            assertTrue(input.getFormat().getChannels() > 0);
            assertTrue(input.readAllBytes().length > 0);
        }
    }

    @Test
    void decodesFragmentedMp4AacToPcm() throws Exception {
        try (AacInputStream input = new AacInputStream(new ByteArrayInputStream(AacTestFixtures.fragmentedMp4()))) {
            assertEquals(44_100, input.getFormat().getSampleRate());
            assertTrue(input.getFormat().getChannels() > 0);
            assertTrue(input.readAllBytes().length > 0);
        }
    }
}
