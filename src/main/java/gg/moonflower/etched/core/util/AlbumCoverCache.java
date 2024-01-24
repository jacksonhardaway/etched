package gg.moonflower.etched.core.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@ApiStatus.Internal
public final class AlbumCoverCache {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();
    private static final Path CACHE_FOLDER = Paths.get(Minecraft.getInstance().gameDirectory.toURI()).resolve(Etched.MOD_ID + "-cache");
    private static final Object METADATA_LOCK = new Object();
    private static final Object IO_LOCK = new Object();

    private static final Path CACHE_METADATA_LOCATION = CACHE_FOLDER.resolve("cache.json");
    private static final int METADATA_WRITE_TIME = 5000;
    private static volatile JsonObject CACHE_METADATA = new JsonObject();
    private static volatile long nextWriteTime = Long.MAX_VALUE;

    static {
        if (Files.exists(CACHE_METADATA_LOCATION)) {
            LOGGER.debug("Reading cache metadata from file.");
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(CACHE_METADATA_LOCATION.toFile()))) {
                CACHE_METADATA = JsonParser.parseReader(reader).getAsJsonObject();
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
                Util.backgroundExecutor().execute(AlbumCoverCache::writeMetadata);
            }
        });
    }

    private AlbumCoverCache() {
    }

    private static synchronized void writeMetadata() {
        LOGGER.debug("Writing cache metadata to file.");
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
     * Fetches the texture by the specified name.
     *
     * @param url     The name of the texture to fetch
     * @param timeout The amount of time the cache should remain valid
     * @param unit    The unit of time timeout is defined in
     * @param fetcher The function providing a new stream
     * @return The location of a file that can be opened with the data
     */
    @Nullable
    public static Path getPath(String url, long timeout, TimeUnit unit, Function<String, InputStream> fetcher) {
        Path imageFile = CACHE_FOLDER.resolve(DigestUtils.md5Hex(url));

        String key = DigestUtils.md5Hex(url);
        if (Files.exists(imageFile)) {
            if (CACHE_METADATA.has(key) && CACHE_METADATA.get(key).isJsonPrimitive() && CACHE_METADATA.get(key).getAsJsonPrimitive().isNumber()) {
                long now = System.currentTimeMillis();
                long expirationDate = CACHE_METADATA.get(key).getAsLong();
                if (expirationDate - now > 0) {
                    return imageFile;
                }
            }
        }

        InputStream fetchedStream = fetcher.apply(url);
        if (fetchedStream == null) {
            try {
                if (!Files.exists(CACHE_FOLDER)) {
                    synchronized (IO_LOCK) {
                        Files.createDirectory(CACHE_FOLDER);
                    }
                }
                if (!Files.exists(imageFile)) {
                    synchronized (IO_LOCK) {
                        Files.createFile(imageFile);
                    }
                }
                synchronized (METADATA_LOCK) {
                    CACHE_METADATA.addProperty(key, System.currentTimeMillis() + unit.toMillis(timeout));
                    nextWriteTime = System.currentTimeMillis() + METADATA_WRITE_TIME;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to create empty file '" + imageFile + "' for '" + url + "'", e);
            }
            return null;
        }

        try {
            synchronized (IO_LOCK) {
                if (!Files.exists(CACHE_FOLDER)) {
                    Files.createDirectory(CACHE_FOLDER);
                }
                Files.copy(fetchedStream, imageFile, StandardCopyOption.REPLACE_EXISTING);
            }
            synchronized (METADATA_LOCK) {
                CACHE_METADATA.addProperty(key, System.currentTimeMillis() + unit.toMillis(timeout));
                nextWriteTime = System.currentTimeMillis() + METADATA_WRITE_TIME;
            }
            return imageFile;
        } catch (Exception e) {
            LOGGER.error("Failed to write image '" + url + "'", e);
        } finally {
            IOUtils.closeQuietly(fetchedStream);
        }

        return null;
    }
}
