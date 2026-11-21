package gg.moonflower.etched.common.sound.download;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

final class SoundCloudUrl {

    private SoundCloudUrl() {
    }

    static boolean isValid(String url) {
        try {
            String host = new URI(url).getHost();
            return host != null && (host.equalsIgnoreCase("soundcloud.com")
                    || host.toLowerCase(Locale.ROOT).endsWith(".soundcloud.com"));
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
