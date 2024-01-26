package gg.moonflower.etched.client.sound;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import gg.moonflower.etched.api.sound.download.SoundSourceManager;
import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.core.Etched;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Ocelot
 * @see AudioSource#downloadTo(URL, boolean, DownloadProgressListener, AudioSource.AudioFileType)
 */
@ApiStatus.Internal
public final class SoundCache {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(CacheMetadata.class, new MetadataSerializer()).create();
    private static final Path CACHE_FOLDER = Minecraft.getInstance().gameDirectory.toPath().resolve(Etched.MOD_ID + "-sounds");
    private static final ReentrantLock DOWNLOAD_LOCK = new ReentrantLock();
    private static final ReentrantLock METADATA_LOCK = new ReentrantLock();
    private static final ReentrantLock IO_LOCK = new ReentrantLock();
    private static final Type CACHE_METADATA_TYPE = new TypeToken<Map<String, CacheMetadata>>() {
    }.getType();

    private static final Path CACHE_METADATA_LOCATION = CACHE_FOLDER.resolve("cache.json");
    private static final int METADATA_WRITE_TIME = 5000;
    private static volatile Map<String, CacheMetadata> CACHE_METADATA = new HashMap<>();
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
                CACHE_METADATA = GSON.fromJson(reader, CACHE_METADATA_TYPE);
            } catch (Exception e) {
                LOGGER.error("Failed to load cache metadata", e);
            }
        }
        MinecraftForge.EVENT_BUS.<TickEvent.ClientTickEvent>addListener(event -> {
            if (nextWriteTime == Long.MAX_VALUE) {
                return;
            }

            if (System.currentTimeMillis() - nextWriteTime > 0) {
                nextWriteTime = Long.MAX_VALUE;
                Util.ioPool().execute(SoundCache::writeMetadata);
            }
        });
//        ClientTickEvent.CLIENT_POST.register(client -> {
//            if (nextWriteTime == Long.MAX_VALUE)
//                return;
//
//            if (System.currentTimeMillis() - nextWriteTime > 0) {
//                nextWriteTime = Long.MAX_VALUE;
//                Util.ioPool().execute(SoundCache::writeMetadata);
//            }
//        });
    }

    private SoundCache() {
    }

    private static synchronized void writeMetadata() {
        LOGGER.debug("Writing cache metadata to file.");
        try {
            METADATA_LOCK.lock();
            CACHE_METADATA.keySet().removeIf(name -> !Files.exists(CACHE_FOLDER.resolve(name)));
        } finally {
            METADATA_LOCK.unlock();
        }

        try (FileOutputStream os = new FileOutputStream(CACHE_METADATA_LOCATION.toFile())) {
            if (!Files.exists(CACHE_FOLDER)) {
                Files.createDirectory(CACHE_FOLDER);
            }
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
            if (!future.isDone()) {
                return future;
            }
        }

        try {
            DOWNLOAD_LOCK.lock();

            CompletableFuture<AudioSource> future = SoundSourceManager.getAudioSource(url, listener, Minecraft.getInstance().getProxy(), type).handle((source, e) -> {
                if (e != null) {
                    if (listener != null) {
                        listener.onFail();
                    }
                    throw new CompletionException(e);
                }
                return source;
            }).handle((source, throwable) -> {
                Minecraft.getInstance().execute(() -> DOWNLOADING.remove(url));
                if (throwable != null) {
                    if (throwable instanceof CompletionException e) {
                        throw e;
                    }
                    throw new CompletionException(throwable);
                }
                return source;
            });

            DOWNLOADING.put(url, future);
            return future;
        } catch (Exception e) {
            if (listener != null) {
                listener.onFail();
            }
            throw new CompletionException("Failed to load audio into cache", e);
        } finally {
            DOWNLOAD_LOCK.unlock();
        }
    }

    public static @Nullable CacheMetadata getMetadata(String url) {
        return CACHE_METADATA.get(DigestUtils.md5Hex(url));
    }

    public static void updateCacheMetadata(String url, @Nullable CacheMetadata metadata) {
        try {
            METADATA_LOCK.lock();
            String key = DigestUtils.md5Hex(url);
            if (metadata != null) {
                CACHE_METADATA.put(key, metadata);
            } else {
                CACHE_METADATA.remove(key);
            }
            nextWriteTime = System.currentTimeMillis() + METADATA_WRITE_TIME;
        } finally {
            METADATA_LOCK.unlock();
        }
    }

    public static void updateCache(Path soundFile, String url, InputStream stream, CacheMetadata metadata) throws IOException {
        try {
            IO_LOCK.lock();
            if (!Files.exists(CACHE_FOLDER)) {
                Files.createDirectory(CACHE_FOLDER);
            }
            Files.copy(stream, soundFile, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            IO_LOCK.unlock();
        }

        updateCacheMetadata(url, metadata);
    }

    public static Path resolveFilePath(String url, boolean temporary) throws IOException {
        String key = DigestUtils.md5Hex(url);
        if (temporary) {
            if (files == null) {
                throw new IllegalStateException("Shutdown in progress");
            }
            if (!files.containsKey(key)) {
                files.put(key, Files.createTempFile(key, null));
            }
            return files.get(key);
        }
        if (!Files.exists(CACHE_FOLDER)) {
            Files.createDirectories(CACHE_FOLDER);
        }
        return CACHE_FOLDER.resolve(key);
    }

    /**
     * @param expiration   The date this object will expire at
     * @param noCache      Whether the cache must validate the response with the server before reusing the cache
     * @param staleIfError Whether a stale response can be used if the server returns an error
     */
    public record CacheMetadata(long expiration,
                                boolean noCache,
                                boolean staleIfError) {

        public boolean isFresh() {
            return this.expiration - System.currentTimeMillis() / 1000L > 0;
        }
    }

    private static class MetadataSerializer implements JsonDeserializer<CacheMetadata>, JsonSerializer<CacheMetadata> {

        @Override
        public CacheMetadata deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonObject()) {
                JsonObject object = json.getAsJsonObject();
                long expiration = object.get("expiration").getAsLong();
                boolean noCache = object.get("noCache").getAsBoolean();
                boolean staleIfError = object.get("staleIfError").getAsBoolean();
                return new CacheMetadata(expiration, noCache, staleIfError);
            }
            return new CacheMetadata(json.getAsLong(), false, false);
        }

        @Override
        public JsonElement serialize(CacheMetadata src, Type typeOfSrc, JsonSerializationContext context) {
            if (!src.noCache && !src.staleIfError) {
                return new JsonPrimitive(src.expiration);
            }

            JsonObject json = new JsonObject();
            json.addProperty("expiration", src.expiration);
            if (src.noCache) {
                json.addProperty("noCache", true);
            }
            if (src.staleIfError) {
                json.addProperty("staleIfError", true);
            }
            return json;
        }
    }
}
