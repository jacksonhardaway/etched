package gg.moonflower.etched.api.sound.download;

import com.mojang.blaze3d.platform.NativeImage;
import gg.moonflower.etched.api.record.AlbumCover;
import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.sound.source.RawAudioSource;
import gg.moonflower.etched.api.sound.source.StreamingAudioSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.client.render.item.AlbumTextureCache;
import gg.moonflower.pollen.pinwheel.api.client.FileCache;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.HttpUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * Manages all sources of sound obtained through sources besides direct downloads.
 *
 * @author Ocelot
 * @since 2.0.0
 */
public final class SoundSourceManager {

    private static final Set<SoundDownloadSource> SOURCES = new HashSet<>();
    private static final Logger LOGGER = LogManager.getLogger();
    private static final FileCache ALBUM_COVER_CACHE = new AlbumTextureCache(HttpUtil.DOWNLOAD_EXECUTOR, 1, TimeUnit.DAYS);

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
                    return new RawAudioSource(DigestUtils.sha1Hex(url), urls[0], listener, source.map(s -> s.isTemporary(url)).orElse(false), type);
                return new StreamingAudioSource(DigestUtils.sha1Hex(url), urls, listener, source.map(s -> s.isTemporary(url)).orElse(false), type);
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
                return source.resolveTracks(url, listener, proxy);
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
        }), HttpUtil.DOWNLOAD_EXECUTOR).thenCompose(coverUrl -> coverUrl.map(s -> ALBUM_COVER_CACHE.requestResource(s, false).thenApplyAsync(path -> {
            try (InputStream is = Files.newInputStream(path)) {
                return AlbumCover.of(NativeImage.read(is));
            } catch (Exception e) {
                throw new CompletionException("Failed to read album cover from '" + url + "'", e);
            }
        }, Util.ioPool())).orElseGet(() -> CompletableFuture.completedFuture(AlbumCover.EMPTY)));
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
