package gg.moonflower.etched.api.sound.download;

import com.mojang.datafixers.util.Pair;
import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.sound.source.RawAudioSource;
import gg.moonflower.etched.api.sound.source.StreamingAudioSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import net.minecraft.network.chat.Component;
import net.minecraft.util.HttpUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Manages all sources of sound obtained through sources besides direct downloads.
 *
 * @author Ocelot
 * @since 2.0.0
 */
public final class SoundSourceManager {

    private static final Set<SoundDownloadSource> SOURCES = new HashSet<>();

    private SoundSourceManager() {}

    /**
     * Registers a new source for sound.
     *
     * @param source The source to add
     */
    public static synchronized void registerSource(SoundDownloadSource source) {
        SOURCES.add(source);
    }

    /**
     * Retrieves an {@link AudioSource} from the specified URL.
     *
     * @param url      The URL to retrieve
     * @param listener The listener for events
     * @param proxy    The connection proxy
     * @return A future for the source
     * @throws MalformedURLException If any error occurs when resolving URLs
     */
    public static CompletableFuture<AudioSource> getAudioSource(String url, @Nullable DownloadProgressListener listener, Proxy proxy) throws MalformedURLException {
        Optional<SoundDownloadSource> source = SOURCES.stream().filter(s -> s.isValidUrl(url)).findFirst();

        return (source.isPresent() ? CompletableFuture.supplyAsync(() -> {
            try {
                return source.get().resolveUrl(url, listener, proxy).toArray(new URL[0]);
            } catch (Exception e) {
                throw new CompletionException("Failed to connect to " + source.get().getApiName() + " API", e);
            }
        }, HttpUtil.DOWNLOAD_EXECUTOR) : CompletableFuture.completedFuture(new URL[]{new URL(url)})).thenApplyAsync(urls -> {
            try {
                if (urls.length == 0)
                    throw new IOException("No audio data was found at the source!");
                if (urls.length == 1)
                    return new RawAudioSource(proxy, DigestUtils.sha1Hex(url), urls[0], listener, source.map(s -> s.isTemporary(url)).orElse(false));
                return new StreamingAudioSource(proxy, DigestUtils.sha1Hex(url), urls, listener, source.map(s -> s.isTemporary(url)).orElse(false));
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, HttpUtil.DOWNLOAD_EXECUTOR);
    }

    /**
     * Resolves the author and title of a track from an external source.
     *
     * @param url      The URL to get the track info from
     * @param listener The listener for events
     * @param proxy    The connection proxy
     * @return The track information found or nothing
     * @throws IOException If any error occurs when connecting to the sources
     */
    public static Optional<Pair<String, String>> resolveTrack(String url, @Nullable DownloadProgressListener listener, Proxy proxy) throws IOException {
        SoundDownloadSource source = SOURCES.stream().filter(s -> s.isValidUrl(url)).findFirst().orElseThrow(() -> new IOException("Unknown source for: " + url));
        try {
            return source.resolveTrack(url, listener, proxy);
        } catch (Exception e) {
            throw new IOException("Failed to connect to " + source.getApiName() + " API", e);
        }
    }

    /**
     * Retrieves the brand information for an external source.
     *
     * @param url The URL to get the brand from
     * @return The brand of that source or nothing
     */
    public static Optional<Component> getBrandText(String url) {
        return SOURCES.stream().filter(source -> source.isValidUrl(url)).findFirst().flatMap(s -> s.getBrandText(url));
    }

    /**
     * Validates the URL is for an external source.
     *
     * @param url The URL to check
     * @return Whether that URL refers to an external source
     */
    public static boolean isValidUrl(String url) {
        return SOURCES.stream().anyMatch(s -> s.isValidUrl(url));
    }
}
