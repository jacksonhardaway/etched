package gg.moonflower.etched.client.sound;

import gg.moonflower.etched.api.sound.download.SoundSourceManager;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.core.Etched;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Ocelot
 */
public final class SoundCache {

    private static final Path SOUND_FOLDER = Minecraft.getInstance().gameDirectory.toPath().resolve(Etched.MOD_ID + "-sounds");
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final Map<String, CompletableFuture<AudioSource>> DOWNLOADING = new HashMap<>();
    private static Map<String, Path> files = new ConcurrentHashMap<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Map<String, Path> theFiles;

            synchronized (SoundCache.class) {
                theFiles = files;
                files = null;
            }

            List<Path> toBeDeleted = new ArrayList<>(theFiles.values());
            theFiles.clear();
            Collections.reverse(toBeDeleted);
            for (Path filename : toBeDeleted) {
                try {
                    Files.deleteIfExists(filename);
                } catch (Exception ignored) {
                }
            }
        }));
    }

    private SoundCache() {
    }

    /**
     * Downloads an audio stream from the specified URL and stores it in a local cache.
     *
     * @param url The url to download the sound from
     * @return An input stream to the locally downloaded file
     */
    public static CompletableFuture<AudioSource> getAudioStream(String url, @Nullable DownloadProgressListener listener) {
        if (DOWNLOADING.containsKey(url)) {
            CompletableFuture<AudioSource> future = DOWNLOADING.get(url);
            if (!future.isDone())
                return future;
        }

        try {
            LOCK.lock();

            CompletableFuture<AudioSource> future = SoundSourceManager.getAudioSource(url, listener, Minecraft.getInstance().getProxy()).handle((source, e) -> {
                if (e != null) {
                    if (listener != null)
                        listener.onFail();
                    throw new CompletionException(e);
                }
                return source;
            }).thenApplyAsync(source -> {
                DOWNLOADING.remove(url);
                return source;
            }, Minecraft.getInstance());

            DOWNLOADING.put(url, future);
            return future;
        } catch (Exception e) {
            if (listener != null)
                listener.onFail();
            throw new CompletionException("Failed to load audio into cache", e);
        } finally {
            LOCK.unlock();
        }
    }

    public static synchronized Path resolveFilePath(String hash, boolean temporary) throws IOException {
        if (temporary)
            return getTemporaryFile(hash);
        if (!Files.exists(SOUND_FOLDER))
            Files.createDirectories(SOUND_FOLDER);
        return SOUND_FOLDER.resolve(hash);
    }

    private static synchronized Path getTemporaryFile(String hash) throws IOException {
        if (files == null)
            throw new IllegalStateException("Shutdown in progress");
        if (!files.containsKey(hash))
            files.put(hash, Files.createTempFile(hash, null));
        return files.get(hash);
    }
}
