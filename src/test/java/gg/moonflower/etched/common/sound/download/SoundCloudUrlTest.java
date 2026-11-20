package gg.moonflower.etched.common.sound.download;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundCloudUrlTest {

    @Test
    void recognizesOnlySoundCloudHosts() {
        assertTrue(SoundCloudUrl.isValid("https://soundcloud.com/artist/track"));
        assertTrue(SoundCloudUrl.isValid("https://m.soundcloud.com/artist/track"));
        assertFalse(SoundCloudUrl.isValid("https://fakesoundcloud.com/artist/track"));
        assertFalse(SoundCloudUrl.isValid("https://soundcloud.com.example.org/artist/track"));
    }
}
