package gg.moonflower.etched.api.sound.download;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns "share" links from file hosting services into links that download the file directly.
 */
public final class ShareLinks {

    private static final Pattern DRIVE_FILE_PATTERN = Pattern.compile("^/file/d/([\\w-]+)");
    private static final Pattern DRIVE_ID_PATTERN = Pattern.compile("(?:^|&)id=([\\w-]+)");
    private static final Pattern DROPBOX_DL_PATTERN = Pattern.compile("(^|&)(?:dl|raw)=[^&]*");

    private ShareLinks() {
    }

    /**
     * Converts a Google Drive or Dropbox share link into a direct download link.
     *
     * @param url The URL to convert
     * @return The direct download URL or the same URL if it is not a share link
     */
    public static String toDirectUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                return url;
            }

            String path = uri.getPath() == null ? "" : uri.getPath();
            String query = uri.getRawQuery() == null ? "" : uri.getRawQuery();
            if (host.equals("drive.google.com")) {
                Matcher matcher = DRIVE_FILE_PATTERN.matcher(path);
                String id = matcher.find() ? matcher.group(1) : null;
                if (id == null && (path.equals("/open") || path.equals("/uc"))) {
                    matcher = DRIVE_ID_PATTERN.matcher(query);
                    id = matcher.find() ? matcher.group(1) : null;
                }
                // confirm=t skips the virus scan warning page Drive shows for large files
                return id != null ? "https://drive.usercontent.google.com/download?id=" + id + "&export=download&confirm=t" : url;
            }
            if ((host.equals("www.dropbox.com") || host.equals("dropbox.com")) && (path.startsWith("/s/") || path.startsWith("/scl/fi/"))) {
                String newQuery = DROPBOX_DL_PATTERN.matcher(query).replaceAll("");
                newQuery = newQuery.startsWith("&") ? newQuery.substring(1) : newQuery;
                newQuery = newQuery.isEmpty() ? "dl=1" : newQuery + "&dl=1";
                return "https://www.dropbox.com" + uri.getRawPath() + "?" + newQuery;
            }
        } catch (URISyntaxException ignored) {
        }
        return url;
    }

    /**
     * Checks to see if the URL is a share link that downloads the file with a GET request only.
     *
     * @param url The URL to check
     * @return Whether HEAD requests should be avoided for the URL
     */
    public static boolean requiresGet(String url) {
        try {
            String host = new URI(url).getHost();
            return "www.dropbox.com".equals(host) || "drive.usercontent.google.com".equals(host);
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
