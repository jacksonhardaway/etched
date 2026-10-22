package gg.moonflower.etched.client.search;

/**
 * A single playable search result.
 *
 * @param title   The track or station name
 * @param details Secondary information such as the artist, country or format
 * @param url     The URL to put into the radio or etching table
 */
public record MusicSearchResult(String title, String details, String url) {
}
