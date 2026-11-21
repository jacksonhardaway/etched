package gg.moonflower.etched.api.sound.metadata;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class IcyMetadataRegistry {

    private static final int MAX_ENTRIES = 256;
    private static final ConcurrentHashMap<String, Metadata> ENTRIES = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<String> ORDER = new ConcurrentLinkedQueue<>();

    private IcyMetadataRegistry() {
    }

    public static Metadata get(String url) {
        return ENTRIES.getOrDefault(url, Metadata.EMPTY);
    }

    public static void begin(String url, String stationName) {
        if (ENTRIES.put(url, new Metadata(clean(stationName, 128), "")) == null) {
            ORDER.add(url);
            trim();
        }
    }

    public static void updateTitle(String url, String title) {
        ENTRIES.computeIfPresent(url, (ignored, current) ->
                new Metadata(current.stationName(), clean(title, 256)));
    }

    private static void trim() {
        while (ENTRIES.size() > MAX_ENTRIES) {
            String oldest = ORDER.poll();
            if (oldest == null) {
                return;
            }
            ENTRIES.remove(oldest);
        }
    }

    private static String clean(String value, int maximumLength) {
        String cleaned = value == null ? "" : value.strip();
        return cleaned.length() <= maximumLength ? cleaned : cleaned.substring(0, maximumLength);
    }

    public record Metadata(String stationName, String streamTitle) {
        private static final Metadata EMPTY = new Metadata("", "");
    }
}
