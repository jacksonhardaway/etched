package gg.moonflower.etched.api.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Parses URLs from a M3u file.
 *
 * @author Ocelot
 */
public final class M3uParser {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Parses a list of URLs from the specified reader.
     *
     * @param stream The stream to get data from
     * @return A list of all URLs in the file
     * @throws IOException If any error occurs reading the data
     */
    public static List<URL> parse(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        List<String> lines = new LinkedList<>();
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            if (!line.isBlank()) {
                lines.add(line.trim());
            }
        }

        if (lines.isEmpty()) {
            return Collections.emptyList();
        }

        List<URL> entries = new LinkedList<>();
        Iterator<String> iterator = lines.iterator();

        // Parse header first
        boolean extendedHeader = false;
        int version = 0;
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (line.equals("#EXTM3U")) {
                extendedHeader = true;
                continue;
            }

            if (line.startsWith("#EXT-X-VERSION:")) {
                version = Integer.parseInt(line.substring(15));
                continue;
            }

            if (line.startsWith("#EXT-X-MAP:URI=")) {
                String url = line.substring(16, line.length() - 1);
                try {
                    entries.add(new URI(url).toURL());
                } catch (Exception e) {
                    throw new IOException("Failed to parse map URL: " + url, e);
                }
                continue;
            }

            // Assume the header is done
            if (line.startsWith("#EXTINF")) {
                // validate
                if (!extendedHeader) {
                    throw new IOException("Invalid Header");
                }
                if (version != 6) {
                    throw new IOException("Invalid M3u Version, expected 6, got " + version);
                }
                break;
            }
        }

        if (!iterator.hasNext()) {
            throw new IOException("No body was provided");
        }

        // Try parsing the body now
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (line.startsWith("#")) {
                continue;
            }

            // It's fine if these URLs have issues, just continue anyway
            try {
                entries.add(new URI(line).toURL());
            } catch (Exception e) {
                LOGGER.warn("Could not parse as location: {}", line, e);
            }
        }

        return entries;
    }
}
