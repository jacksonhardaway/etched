package gg.moonflower.etched.api.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioContentSnifferTest {

    @Test
    void recognizesCurrentAudioFormats() {
        assertTrue(AudioContentSniffer.isSupportedAudio(new byte[]{'O', 'g', 'g', 'S'}));
        assertTrue(AudioContentSniffer.isSupportedAudio(new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E'}));
        assertTrue(AudioContentSniffer.isSupportedAudio(new byte[]{(byte) 0xFF, (byte) 0xF1, 0x50, (byte) 0x80, 0x01, 0x1F, (byte) 0xFC}));
        assertTrue(AudioContentSniffer.isSupportedAudio(new byte[]{0, 0, 0, 24, 'f', 't', 'y', 'p'}));
        assertTrue(AudioContentSniffer.isSupportedAudio(new byte[]{'I', 'D', '3'}));
        assertTrue(AudioContentSniffer.isSupportedAudio(new byte[]{(byte) 0xFF, (byte) 0xFB, (byte) 0x90, 0x00}));
    }

    @Test
    void rejectsExecutablesScriptsAndRandomData() {
        assertFalse(AudioContentSniffer.isSupportedAudio(new byte[]{'M', 'Z', 0, 0}));
        assertFalse(AudioContentSniffer.isSupportedAudio("@echo off\r\ncalc.exe".getBytes()));
        assertFalse(AudioContentSniffer.isSupportedAudio(new byte[]{1, 2, 3, 4}));
    }

    @Test
    void preservesTheStreamAfterInspection() throws IOException {
        byte[] audio = new byte[]{'I', 'D', '3', 4, 5, 6};
        try (InputStream input = AudioContentSniffer.requireSupported(new ByteArrayInputStream(audio))) {
            assertArrayEquals(audio, input.readAllBytes());
        }
    }

    @Test
    void closesAndRejectsUnsupportedContent() {
        assertThrows(IOException.class, () -> AudioContentSniffer.requireSupported(
                new ByteArrayInputStream(new byte[]{'M', 'Z', 0, 0})
        ));
    }
}
