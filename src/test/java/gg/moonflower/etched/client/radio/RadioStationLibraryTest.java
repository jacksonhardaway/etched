package gg.moonflower.etched.client.radio;

import gg.moonflower.etched.common.radio.RadioStation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadioStationLibraryTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void savesSortsAndReloadsStations() throws Exception {
        Path file = this.temporaryDirectory.resolve("stations.json");
        RadioStationLibrary library = new RadioStationLibrary(file);
        library.add(new RadioStation("Zulu", "https://example.com/zulu.mp3"));
        library.add(new RadioStation("Alpha", "https://example.com/alpha.mp3"));

        RadioStationLibrary reloaded = new RadioStationLibrary(file);
        reloaded.load();

        assertEquals("Alpha", reloaded.stations().getFirst().name());
        assertEquals(2, reloaded.stations().size());
    }

    @Test
    void detectsEquivalentDropboxLinks() throws Exception {
        RadioStationLibrary library = new RadioStationLibrary(this.temporaryDirectory.resolve("stations.json"));
        library.add(new RadioStation("Test", "https://www.dropbox.com/s/test/test.mp3?dl=0"));
        library.add(new RadioStation("Duplicate", "https://www.dropbox.com/s/test/test.mp3?dl=1"));

        assertEquals(1, library.stations().size());
        assertTrue(library.containsUrl("https://www.dropbox.com/s/test/test.mp3?raw=1"));
    }

    @Test
    void renamesAndRemovesWithoutChangingTheUrl() throws Exception {
        String url = "https://example.com/station.mp3";
        RadioStationLibrary library = new RadioStationLibrary(this.temporaryDirectory.resolve("stations.json"));
        library.add(new RadioStation("Old name", url));
        library.rename(url, "New name");

        assertEquals(new RadioStation("New name", url), library.stations().getFirst());
        library.remove(url);
        assertFalse(library.containsUrl(url));
    }
}
