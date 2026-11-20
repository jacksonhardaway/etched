package gg.moonflower.etched.api.sound.download;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Converts supported file-sharing links into direct download URLs.
 */
public final class ShareLinks {

    private ShareLinks() {
    }

    public static String toDirectAudioUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                return value;
            }

            String host = uri.getHost().toLowerCase(Locale.ROOT);
            if ("drive.google.com".equals(host)) {
                String fileId = getGoogleDriveFileId(uri);
                if (fileId != null) {
                    return "https://drive.usercontent.google.com/download?id=" + fileId + "&export=download&confirm=t";
                }
            }

            if (("dropbox.com".equals(host) || "www.dropbox.com".equals(host)) && isDropboxSharePath(uri.getPath())) {
                return getDropboxDownloadUrl(uri);
            }
        } catch (IllegalArgumentException ignored) {
            // The regular URL handling will report malformed values to the caller.
        }

        return value;
    }

    private static String getGoogleDriveFileId(URI uri) {
        String path = uri.getPath();
        String prefix = "/file/d/";
        if (path != null && path.startsWith(prefix)) {
            int end = path.indexOf('/', prefix.length());
            String id = end < 0 ? path.substring(prefix.length()) : path.substring(prefix.length(), end);
            return isValidGoogleDriveId(id) ? id : null;
        }

        if ("/open".equals(path) || "/uc".equals(path)) {
            String id = getQueryParameter(uri.getRawQuery(), "id");
            return isValidGoogleDriveId(id) ? id : null;
        }

        return null;
    }

    private static boolean isValidGoogleDriveId(String id) {
        return id != null && !id.isBlank() && id.matches("[A-Za-z0-9_-]+");
    }

    private static boolean isDropboxSharePath(String path) {
        return path != null && (path.startsWith("/s/") || path.startsWith("/scl/fi/"));
    }

    private static String getDropboxDownloadUrl(URI uri) {
        List<String> parameters = new ArrayList<>();
        String rawQuery = uri.getRawQuery();
        if (rawQuery != null && !rawQuery.isBlank()) {
            for (String parameter : rawQuery.split("&")) {
                String rawKey = parameter.contains("=") ? parameter.substring(0, parameter.indexOf('=')) : parameter;
                String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
                if (!"dl".equalsIgnoreCase(key) && !"raw".equalsIgnoreCase(key)) {
                    parameters.add(parameter);
                }
            }
        }
        parameters.add("dl=1");

        return uri.getScheme() + "://" + uri.getRawAuthority() + uri.getRawPath() + "?" + String.join("&", parameters);
    }

    private static String getQueryParameter(String rawQuery, String expectedKey) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }

        for (String parameter : rawQuery.split("&")) {
            int separator = parameter.indexOf('=');
            String rawKey = separator < 0 ? parameter : parameter.substring(0, separator);
            String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
            if (expectedKey.equals(key)) {
                String rawValue = separator < 0 ? "" : parameter.substring(separator + 1);
                return URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
