package gg.moonflower.etched.api.sound.download;

import gg.moonflower.etched.api.record.AlbumCover;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.sound.source.RawAudioSource;
import gg.moonflower.etched.api.sound.source.StreamingAudioSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.core.util.AlbumCoverCache;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.HttpUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.*;
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
    private static final Logger LOGGER = LogManager.getLogger();

    private SoundSourceManager() {
    }

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
    public static CompletableFuture<AudioSource> getAudioSource(String url, @Nullable DownloadProgressListener listener, Proxy proxy, AudioSource.AudioFileType type) throws MalformedURLException {
        Optional<SoundDownloadSource> sourceOptional = SOURCES.stream().filter(s -> s.isValidUrl(url)).findFirst();
        CompletableFuture<List<URL>> urlFuture = sourceOptional.isPresent() ? CompletableFuture.supplyAsync(() -> {
            SoundDownloadSource source = sourceOptional.get();
            try {
                return source.resolveUrl(url, listener, proxy);
            } catch (Exception e) {
                throw new CompletionException("Failed to connect to " + source.getApiName() + " API", e);
            }
        }, HttpUtil.DOWNLOAD_EXECUTOR) : CompletableFuture.completedFuture(Collections.singletonList(new URL(url)));

        return urlFuture.thenApplyAsync(urls -> {
            try {
                if (urls.isEmpty()) {
                    throw new IOException("No audio data was found at the source!");
                }
                if (urls.size() == 1) {
                    return new RawAudioSource(urls.get(0), listener, sourceOptional.map(s -> s.isTemporary(url)).orElse(false), type);
                }
                return new StreamingAudioSource(urls.toArray(URL[]::new), listener, sourceOptional.map(s -> s.isTemporary(url)).orElse(false), type);
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
    public static CompletableFuture<TrackData[]> resolveTracks(String url, @Nullable DownloadProgressListener listener, Proxy proxy) throws IOException {
        SoundDownloadSource source = SOURCES.stream().filter(s -> s.isValidUrl(url)).findFirst().orElseThrow(() -> new IOException("Unknown source for: " + url));
        return CompletableFuture.supplyAsync(() -> {
            try {
                return source.resolveTracks(url, listener, proxy).toArray(TrackData[]::new);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, HttpUtil.DOWNLOAD_EXECUTOR);
    }

    /**
     * Resolves the album cover from an external source.
     *
     * @param url      The URL to get the cover from
     * @param listener The listener for events
     * @param proxy    The connection proxy
     * @return The album cover found or nothing
     */
    public static CompletableFuture<AlbumCover> resolveAlbumCover(String url, @Nullable DownloadProgressListener listener, Proxy proxy, ResourceManager resourceManager) {
        return CompletableFuture.supplyAsync(() -> SOURCES.stream().filter(s -> s.isValidUrl(url)).findFirst().flatMap(source -> {
            try {
                return source.resolveAlbumCover(url, listener, proxy, resourceManager);
            } catch (Exception e) {
                LOGGER.error("Failed to connect to " + source.getApiName() + " API", e);
                return Optional.empty();
            }
        }), HttpUtil.DOWNLOAD_EXECUTOR).thenCompose(coverUrl -> coverUrl.map(AlbumCoverCache::requestResource).orElseGet(() -> CompletableFuture.completedFuture(AlbumCover.EMPTY)));
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
