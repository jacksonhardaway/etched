package gg.moonflower.etched.common.radio;

import java.net.URI;

public record RadioStation(String name, String url) {

    public static final int MAX_NAME_LENGTH = 64;
    public static final int MAX_URL_LENGTH = 32768;
    public static final RadioStation EMPTY = new RadioStation("", "");

    public RadioStation {
        name = clean(name, MAX_NAME_LENGTH);
        url = clean(url, MAX_URL_LENGTH);
        if (url.isEmpty()) {
            name = "";
        }
    }

    public boolean isEmpty() {
        return this.url.isEmpty();
    }

    public String displayName() {
        if (!this.name.isEmpty()) {
            return this.name;
        }
        try {
            String host = URI.create(this.url).getHost();
            if (host != null && !host.isBlank()) {
                return host;
            }
        } catch (IllegalArgumentException ignored) {
        }
        return this.url;
    }

    private static String clean(String value, int maximumLength) {
        String cleaned = value == null ? "" : value.strip();
        return cleaned.length() <= maximumLength ? cleaned : cleaned.substring(0, maximumLength);
    }
}
