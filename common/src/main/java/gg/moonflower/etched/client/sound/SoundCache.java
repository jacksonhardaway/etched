package gg.moonflower.etched.client.sound;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.architectury.event.events.client.ClientTickEvent;
import gg.moonflower.etched.api.sound.download.SoundSourceManager;
import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.core.Etched;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Ocelot
 */
public final class SoundCache {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();
    private static final Path CACHE_FOLDER = Minecraft.getInstance().gameDirectory.toPath().resolve(Etched.MOD_ID + "-sounds");
    private static final ReentrantLock DOWNLOAD_LOCK = new ReentrantLock();
    private static final ReentrantLock METADATA_LOCK = new ReentrantLock();
    private static final ReentrantLock IO_LOCK = new ReentrantLock();

    private static final Path CACHE_METADATA_LOCATION = CACHE_FOLDER.resolve("cache.json");
    private static final int METADATA_WRITE_TIME = 5000;
    private static volatile JsonObject CACHE_METADATA = new JsonObject();
    private static volatile long nextWriteTime = Long.MAX_VALUE;

    private static final Map<String, CompletableFuture<AudioSource>> DOWNLOADING = new HashMap<>();
    private static Map<String, Path> files = new ConcurrentHashMap<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Map<String, Path> theFiles;

            synchronized (SoundCache.class) {
                theFiles = files;
                files = null;
            }

            theFiles.keySet().forEach(CACHE_METADATA::remove);

            List<Path> toBeDeleted = new ArrayList<>(theFiles.values());
            theFiles.clear();
            Collections.reverse(toBeDeleted);
            for (Path filename : toBeDeleted) {
                try {
                    Files.deleteIfExists(filename);
                } catch (Exception ignored) {
                }
            }

            writeMetadata();
        }));

        if (Files.exists(CACHE_METADATA_LOCATION)) {
            LOGGER.debug("Reading cache metadata from file.");
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(CACHE_METADATA_LOCATION.toFile()))) {
                CACHE_METADATA = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception e) {
                LOGGER.error("Failed to load cache metadata", e);
            }
        }
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (nextWriteTime == Long.MAX_VALUE)
                return;

            if (System.currentTimeMillis() - nextWriteTime > 0) {
                nextWriteTime = Long.MAX_VALUE;
                Util.ioPool().execute(SoundCache::writeMetadata);
            }
        });
    }

    private SoundCache() {
    }

    private static synchronized void writeMetadata() {
        LOGGER.debug("Writing cache metadata to file.");
        try (FileOutputStream os = new FileOutputStream(CACHE_METADATA_LOCATION.toFile())) {
            if (!Files.exists(CACHE_FOLDER))
                Files.createDirectory(CACHE_FOLDER);
            IOUtils.write(GSON.toJson(CACHE_METADATA), os, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("Failed to write cache metadata", e);
        }
    }

    /**
     * Downloads an audio stream from the specified URL and stores it in a local cache.
     *
     * @param url The url to download the sound from
     * @return An input stream to the locally downloaded file
     */
    public static CompletableFuture<AudioSource> getAudioStream(String url, @Nullable DownloadProgressListener listener, AudioSource.AudioFileType type) {
        if (DOWNLOADING.containsKey(url)) {
            CompletableFuture<AudioSource> future = DOWNLOADING.get(url);
            if (!future.isDone())
                return future;
        }

        try {
            DOWNLOAD_LOCK.lock();

            CompletableFuture<AudioSource> future = SoundSourceManager.getAudioSource(url, listener, Minecraft.getInstance().getProxy(), type).handle((source, e) -> {
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
            DOWNLOAD_LOCK.unlock();
        }
    }

    public static boolean isValid(Path soundFile, String url) {
        String key = DigestUtils.md5Hex(url);
        if (!Files.exists(soundFile))
            return false;

        if (CACHE_METADATA.has(key) && CACHE_METADATA.get(key).isJsonPrimitive() && CACHE_METADATA.get(key).getAsJsonPrimitive().isNumber()) {
            long now = System.currentTimeMillis() / 1000L;
            long expirationDate = CACHE_METADATA.get(key).getAsLong();
            return expirationDate - now > 0;
        }

        return false;
    }

    public static void updateCache(Path soundFile, String url, long timeout, TimeUnit unit, InputStream stream) {
        try {
            try {
                IO_LOCK.lock();
                if (!Files.exists(CACHE_FOLDER))
                    Files.createDirectory(CACHE_FOLDER);
                Files.copy(stream, soundFile, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                IO_LOCK.unlock();
            }

            try {
                METADATA_LOCK.lock();
                CACHE_METADATA.addProperty(DigestUtils.md5Hex(url), System.currentTimeMillis() / 1000L + unit.toSeconds(timeout));
                nextWriteTime = System.currentTimeMillis() + METADATA_WRITE_TIME;
            } finally {
                METADATA_LOCK.unlock();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to write sound: " + url, e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public static Path resolveFilePath(String hash, boolean temporary) throws IOException {
        if (temporary)
            return getTemporaryFile(hash);
        if (!Files.exists(CACHE_FOLDER))
            Files.createDirectories(CACHE_FOLDER);
        return CACHE_FOLDER.resolve(hash);
    }

    private static Path getTemporaryFile(String hash) throws IOException {
        if (files == null)
            throw new IllegalStateException("Shutdown in progress");
        if (!files.containsKey(hash))
            files.put(hash, Files.createTempFile(hash, null));
        return files.get(hash);
    }
}
