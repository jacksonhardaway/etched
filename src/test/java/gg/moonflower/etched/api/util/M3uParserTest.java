package gg.moonflower.etched.api.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class M3uParserTest {

    @Test
    void resolvesRelativeFmp4Segments() throws Exception {
        String playlist = """
                #EXTM3U
                #EXT-X-VERSION:7
                #EXT-X-MAP:BYTERANGE="720@0",URI="init.mp4"
                #EXTINF:2.0,
                segment-1.m4s
                #EXTINF:2.0,
                ../segment-2.m4s
                """;

        List<String> urls = M3uParser.parse(
                new ByteArrayInputStream(playlist.getBytes(StandardCharsets.UTF_8)),
                URI.create("https://audio.example.test/live/playlist.m3u8")
        ).stream().map(Object::toString).toList();

        assertEquals(List.of(
                "https://audio.example.test/live/init.mp4",
                "https://audio.example.test/live/segment-1.m4s",
                "https://audio.example.test/segment-2.m4s"
        ), urls);
    }

    @Test
    void rejectsUnsupportedPlaylistVersions() {
        String playlist = "#EXTM3U\n#EXT-X-VERSION:8\n#EXTINF:2.0,\nsegment.m4s\n";
        assertThrows(IOException.class, () -> M3uParser.parse(
                new ByteArrayInputStream(playlist.getBytes(StandardCharsets.UTF_8)),
                URI.create("https://audio.example.test/playlist.m3u8")
        ));
    }
}
