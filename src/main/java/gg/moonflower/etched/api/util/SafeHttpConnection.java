package gg.moonflower.etched.api.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

/**
 * Opens player-provided HTTP URLs without allowing access to the local network.
 */
public final class SafeHttpConnection {

    private static final int MAX_REDIRECTS = 5;

    private SafeHttpConnection() {
    }

    public static HttpURLConnection open(URL initialUrl, Proxy proxy, String method,
                                         Map<String, String> headers, int connectTimeout, int readTimeout) throws IOException {
        URL currentUrl = initialUrl;
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            validatePublicHttpUrl(currentUrl);
            URLConnection rawConnection = currentUrl.openConnection(proxy != null ? proxy : Proxy.NO_PROXY);
            if (!(rawConnection instanceof HttpURLConnection connection)) {
                throw new IOException("Only HTTP and HTTPS URLs are supported");
            }

            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setRequestMethod(method);
            headers.forEach(connection::setRequestProperty);

            int response = connection.getResponseCode();
            if (!isRedirect(response)) {
                return connection;
            }

            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (location == null || location.isBlank()) {
                throw new IOException("HTTP redirect has no destination");
            }
            if (redirect == MAX_REDIRECTS) {
                throw new IOException("Too many HTTP redirects");
            }
            try {
                currentUrl = URI.create(currentUrl.toExternalForm()).resolve(location).toURL();
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid HTTP redirect destination", e);
            }
        }
        throw new IOException("Too many HTTP redirects");
    }

    static void validatePublicHttpUrl(URL url) throws IOException {
        String protocol = url.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            throw new IOException("Only HTTP and HTTPS URLs are supported");
        }

        String host = url.getHost();
        if (host == null || host.isBlank()) {
            throw new IOException("URL has no host");
        }
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }

        InetAddress[] addresses = InetAddress.getAllByName(host);
        if (addresses.length == 0) {
            throw new IOException("URL host has no address");
        }
        for (InetAddress address : addresses) {
            if (isPrivate(address)) {
                throw new IOException("Local and private network addresses are not allowed");
            }
        }
        // shortcut: DNS is checked immediately before connecting; pin addresses if URLs become public untrusted content.
    }

    private static boolean isRedirect(int response) {
        return response == HttpURLConnection.HTTP_MOVED_PERM
                || response == HttpURLConnection.HTTP_MOVED_TEMP
                || response == HttpURLConnection.HTTP_SEE_OTHER
                || response == 307
                || response == 308;
    }

    private static boolean isPrivate(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();
        if (address instanceof Inet6Address) {
            return (bytes[0] & 0xFE) == 0xFC;
        }

        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);
        return first == 0
                || first >= 224
                || first == 100 && second >= 64 && second <= 127
                || first == 198 && (second == 18 || second == 19);
    }
}
