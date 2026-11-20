package gg.moonflower.etched.common.radio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadioStationTest {

    @Test
    void sanitizesValues() {
        RadioStation station = new RadioStation("  Reggae Paradise  ", "  https://example.com/live.mp3  ");

        assertEquals("Reggae Paradise", station.name());
        assertEquals("https://example.com/live.mp3", station.url());
    }

    @Test
    void anEmptyUrlProducesAnEmptyStation() {
        RadioStation station = new RadioStation("Unused name", " ");

        assertTrue(station.isEmpty());
        assertEquals("", station.name());
    }
}
