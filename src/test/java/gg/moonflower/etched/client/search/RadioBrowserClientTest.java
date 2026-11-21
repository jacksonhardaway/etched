package gg.moonflower.etched.client.search;

import org.junit.jupiter.api.Test;

import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadioBrowserClientTest {

    @Test
    void keepsOnlyUniqueDirectPlayableStreams() throws Exception {
        String response = """
                [
                  {"name":"Radio One","stationuuid":"one","url_resolved":"https://radio.test/one.mp3","codec":"MP3","bitrate":128,"countrycode":"CH","hls":false},
                  {"name":"Duplicate","stationuuid":"duplicate","url_resolved":"https://radio.test/one.mp3","codec":"MP3","bitrate":128,"countrycode":"CH","hls":0},
                  {"name":"AAC Radio","stationuuid":"two","url":"https://radio.test/two.aac","codec":"AAC+","bitrate":64,"countrycode":"BE","hls":false},
                  {"name":"Generic HLS","stationuuid":"hls","url":"https://radio.test/live.m3u8","codec":"AAC","bitrate":64,"countrycode":"FR","hls":true},
                  {"name":"Numeric HLS","stationuuid":"hls-number","url":"https://radio.test/live-2.m3u8","codec":"AAC","bitrate":64,"countrycode":"FR","hls":1},
                  {"name":"Opus","stationuuid":"opus","url":"https://radio.test/opus","codec":"OGG","bitrate":96,"countrycode":"FR","hls":false},
                  {"name":"Unsafe","stationuuid":"unsafe","url":"file:///tmp/audio.mp3","codec":"MP3","bitrate":128,"countrycode":"FR","hls":false}
                ]
                """;

        List<RadioBrowserClient.Station> stations = RadioBrowserClient.parseStations(
                response.getBytes(StandardCharsets.UTF_8),
                "https://de1.api.radio-browser.info"
        );

        assertEquals(2, stations.size());
        assertEquals("Radio One", stations.get(0).name());
        assertEquals("CH | MP3 128 kbps", stations.get(0).details());
        assertEquals("AAC Radio", stations.get(1).name());
        assertEquals("https://radio.test/two.aac", stations.get(1).url());
    }

    @Test
    void cancellationDisconnectsTheActiveRequest() throws Exception {
        RadioBrowserClient.RequestControl control = new RadioBrowserClient.RequestControl();
        TestConnection connection = new TestConnection();
        control.attach(connection);

        control.cancel();

        assertTrue(connection.disconnected);
        assertThrows(InterruptedIOException.class, control::checkCancelled);
    }

    private static final class TestConnection extends HttpURLConnection {

        private boolean disconnected;

        private TestConnection() throws Exception {
            super(URI.create("https://example.com").toURL());
        }

        @Override
        public void disconnect() {
            this.disconnected = true;
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {
        }
    }
}
