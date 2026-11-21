package gg.moonflower.etched.api.util;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SafeHttpConnectionTest {

    @Test
    void acceptsPublicHttpAddresses() {
        assertDoesNotThrow(() -> SafeHttpConnection.validatePublicHttpUrl(
                URI.create("https://8.8.8.8/radio.mp3").toURL()));
    }

    @Test
    void rejectsLocalAndPrivateAddresses() {
        for (String value : new String[]{
                "http://127.0.0.1/audio",
                "http://10.0.0.1/audio",
                "http://172.16.0.1/audio",
                "http://192.168.1.1/audio",
                "http://169.254.1.1/audio",
                "http://100.64.0.1/audio",
                "http://[::1]/audio",
                "http://[fc00::1]/audio"
        }) {
            assertThrows(IOException.class, () -> SafeHttpConnection.validatePublicHttpUrl(
                    URI.create(value).toURL()), value);
        }
    }

    @Test
    void rejectsNonHttpProtocols() {
        assertThrows(IOException.class, () -> SafeHttpConnection.validatePublicHttpUrl(
                URI.create("file:///tmp/audio.mp3").toURL()));
    }

    @Test
    void rejectsRedirectsToPrivateAddresses() throws Exception {
        HttpServer proxyServer = redirectingProxy("http://127.0.0.1/audio", new AtomicInteger());
        try {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyServer.getAddress());
            assertThrows(IOException.class, () -> SafeHttpConnection.open(
                    URI.create("http://8.8.8.8/radio").toURL(), proxy, "GET", Map.of(), 1_000, 1_000));
        } finally {
            proxyServer.stop(0);
        }
    }

    @Test
    void rejectsRedirectLoopsAfterTheConfiguredLimit() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer proxyServer = redirectingProxy("/loop", requests);
        try {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyServer.getAddress());
            IOException exception = assertThrows(IOException.class, () -> SafeHttpConnection.open(
                    URI.create("http://8.8.8.8/loop").toURL(), proxy, "GET", Map.of(), 1_000, 1_000));
            assertEquals("Too many HTTP redirects", exception.getMessage());
            assertEquals(6, requests.get());
        } finally {
            proxyServer.stop(0);
        }
    }

    private static HttpServer redirectingProxy(String location, AtomicInteger requests) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            exchange.getResponseHeaders().add("Location", location);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.start();
        return server;
    }
}
