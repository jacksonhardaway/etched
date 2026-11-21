package gg.moonflower.etched.api.sound.metadata;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IcyMetadataInputStreamTest {

    @Test
    void stripsMetadataAndReportsTitle() throws Exception {
        byte[] source = stream("abcde", "StreamTitle='Bob Marley - Exodus';", "fghij");
        List<String> titles = new ArrayList<>();

        try (IcyMetadataInputStream input = new IcyMetadataInputStream(
                new ByteArrayInputStream(source), 5, titles::add)) {
            assertArrayEquals("abcdefghij".getBytes(StandardCharsets.US_ASCII), input.readAllBytes());
        }

        assertEquals(List.of("Bob Marley - Exodus"), titles);
    }

    @Test
    void worksAcrossSmallReads() throws Exception {
        byte[] source = stream("abcde", "StreamTitle='First';", "fghij");
        ByteArrayOutputStream audio = new ByteArrayOutputStream();

        try (IcyMetadataInputStream input = new IcyMetadataInputStream(
                new ByteArrayInputStream(source), 5, ignored -> {
                })) {
            byte[] buffer = new byte[3];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                audio.write(buffer, 0, read);
            }
        }

        assertEquals("abcdefghij", audio.toString(StandardCharsets.US_ASCII));
    }

    private static byte[] stream(String firstAudio, String metadata, String secondAudio) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(firstAudio.getBytes(StandardCharsets.US_ASCII));
        byte[] value = metadata.getBytes(StandardCharsets.ISO_8859_1);
        int blocks = (value.length + 15) / 16;
        output.write(blocks);
        output.write(value);
        output.write(new byte[blocks * 16 - value.length]);
        output.write(secondAudio.getBytes(StandardCharsets.US_ASCII));
        return output.toByteArray();
    }
}
