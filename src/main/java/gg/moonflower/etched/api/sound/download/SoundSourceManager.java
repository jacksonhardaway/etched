package gg.moonflower.etched.api.sound.download;

import gg.moonflower.etched.api.record.AlbumCover;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.sound.source.RawAudioSource;
import gg.moonflower.etched.api.sound.source.StreamingAudioSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.client.AlbumCoverCache;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
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
     */
    public static CompletableFuture<AudioSource> getAudioSource(String url, @Nullable DownloadProgressListener listener, Proxy proxy, AudioSource.AudioFileType type) {
        Optional<SoundDownloadSource> sourceOptional = SOURCES.stream().filter(s -> s.isValidUrl(url)).findFirst();
        if (sourceOptional.isEmpty()) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return new RawAudioSource(new URI(ShareLinks.toDirectUrl(url)).toURL(), listener, false, type);
                } catch (Throwable t) {
                    throw new CompletionException("Failed to download audio: " + url, t);
                }
            }, Util.nonCriticalIoPool());
        }

        SoundDownloadSource source = sourceOptional.get();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Collection<URL> urls = source.resolveUrl(url, listener, proxy);
                if (urls.isEmpty()) {
                    throw new IOException("No audio data was found at the source!");
                }
                if (urls.size() == 1) {
                    return new RawAudioSource(urls.iterator().next(), listener, source.isTemporary(url), type);
                }
                return new StreamingAudioSource(urls.toArray(URL[]::new), listener, source.isTemporary(url), type);
            } catch (Throwable t) {
                throw new CompletionException("Failed to connect to " + source.getApiName() + " API", t);
            }
        }, Util.nonCriticalIoPool());
    }

    /**
     * Resolves the author and title of a track from an external source.
     *
     * @param url      The URL to get the track info from
     * @param listener The listener for events
     * @param proxy    The connection proxy
     * @return The track information found or nothing
     */
    public static CompletableFuture<TrackData[]> resolveTracks(String url, @Nullable DownloadProgressListener listener, Proxy proxy) {
        Optional<SoundDownloadSource> sourceOptional = SOURCES.stream().filter(s -> s.isValidUrl(url)).findFirst();
        if (sourceOptional.isEmpty()) {
            return CompletableFuture.failedFuture(new IOException("Unknown source for: " + url));
        }

        SoundDownloadSource source = sourceOptional.get();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return source.resolveTracks(url, listener, proxy).toArray(TrackData[]::new);
            } catch (Throwable t) {
                throw new CompletionException("Failed to connect to " + source.getApiName() + " API", t);
            }
        }, Util.nonCriticalIoPool());
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
        Optional<SoundDownloadSource> sourceOptional = SOURCES.stream().filter(s -> s.isValidUrl(url)).findFirst();
        if (sourceOptional.isEmpty()) {
            return CompletableFuture.completedFuture(AlbumCover.EMPTY);
        }

        SoundDownloadSource source = sourceOptional.get();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<String> coverUrl = source.resolveAlbumCover(url, listener, proxy, resourceManager);
                return coverUrl.map(AlbumCoverCache::requestResource).orElseGet(() -> CompletableFuture.completedFuture(AlbumCover.EMPTY));
            } catch (Throwable t) {
                LOGGER.error("Failed to connect to {} API", source.getApiName(), t);
                return CompletableFuture.completedFuture(AlbumCover.EMPTY);
            }
        }, Util.nonCriticalIoPool()).thenCompose(future -> future);
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
