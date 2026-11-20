package gg.moonflower.etched.api.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SafeHttpConnectionTest {

    @Test
    void acceptsPublicHttpAddresses() {
        assertDoesNotThrow(() -> SafeHttpConnection.validatePublicHttpUrl(
                URI.create("https://8.8.8.8/radio.mp3").toURL()));
    }

    @Test
    void rejectsLocalAndPrivateAddresses() {
        for (String value : new String[]{
                "http://127.0.0.1/audio",
                "http://10.0.0.1/audio",
                "http://172.16.0.1/audio",
                "http://192.168.1.1/audio",
                "http://169.254.1.1/audio",
                "http://100.64.0.1/audio",
                "http://[::1]/audio",
                "http://[fc00::1]/audio"
        }) {
            assertThrows(IOException.class, () -> SafeHttpConnection.validatePublicHttpUrl(
                    URI.create(value).toURL()), value);
        }
    }

    @Test
    void rejectsNonHttpProtocols() {
        assertThrows(IOException.class, () -> SafeHttpConnection.validatePublicHttpUrl(
                URI.create("file:///tmp/audio.mp3").toURL()));
    }
}
