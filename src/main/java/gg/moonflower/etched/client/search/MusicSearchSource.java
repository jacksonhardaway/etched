package gg.moonflower.etched.client.search;

import gg.moonflower.etched.api.sound.download.SoundDownloadSource;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.List;

/**
 * A searchable catalog of music that returns URLs Etched can play.
 */
public interface MusicSearchSource {

    int CONNECT_TIMEOUT = 5000;
    int READ_TIMEOUT = 10000;

    /**
     * Opens a GET request with the Etched download headers.
     *
     * @param url   The URL to request
     * @param proxy The proxy to connect through
     * @return The open connection. The response code has not been read yet
     * @throws IOException If the connection could not be opened
     */
    static HttpURLConnection open(URL url, Proxy proxy) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setInstanceFollowRedirects(true);
        SoundDownloadSource.getDownloadHeaders().forEach(connection::setRequestProperty);
        return connection;
    }

    /**
     * Reads the body of a successful response.
     *
     * @param connection The connection to read from
     * @return The response body
     * @throws IOException If the server responded with an error
     */
    static InputStream read(HttpURLConnection connection) throws IOException {
        int response = connection.getResponseCode();
        if (response != 200) {
            throw new IOException(response + " " + connection.getResponseMessage());
        }
        return connection.getInputStream();
    }

    /**
     * @return The name to display in the source selector
     */
    Component getName();

    /**
     * Searches for music. This is called off-thread.
     *
     * @param query The text the player typed
     * @param proxy The proxy to connect through
     * @return All results that can be played
     * @throws IOException If the search could not be completed
     */
    List<MusicSearchResult> search(String query, Proxy proxy) throws IOException;
}
