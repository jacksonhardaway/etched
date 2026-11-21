package gg.moonflower.etched.api.sound.download;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShareLinksTest {

    @Test
    void convertsGoogleDriveFileLink() {
        assertEquals(
                "https://drive.usercontent.google.com/download?id=abc_DEF-123&export=download&confirm=t",
                ShareLinks.toDirectAudioUrl("https://drive.google.com/file/d/abc_DEF-123/view?usp=sharing")
        );
    }

    @Test
    void convertsGoogleDriveQueryLink() {
        assertEquals(
                "https://drive.usercontent.google.com/download?id=abc_DEF-123&export=download&confirm=t",
                ShareLinks.toDirectAudioUrl("https://drive.google.com/open?id=abc_DEF-123")
        );
    }

    @Test
    void convertsDropboxShareLinkAndKeepsItsToken() {
        assertEquals(
                "https://www.dropbox.com/scl/fi/example/song.mp3?rlkey=secret&dl=1",
                ShareLinks.toDirectAudioUrl("https://www.dropbox.com/scl/fi/example/song.mp3?rlkey=secret&dl=0")
        );
    }

    @Test
    void leavesUnknownOrInsecureHostsUntouched() {
        String insecure = "http://drive.google.com/file/d/abc/view";
        String lookalike = "https://drive.google.com.evil.test/file/d/abc/view";

        assertEquals(insecure, ShareLinks.toDirectAudioUrl(insecure));
        assertEquals(lookalike, ShareLinks.toDirectAudioUrl(lookalike));
    }
}
