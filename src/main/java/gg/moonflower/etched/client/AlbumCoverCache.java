package gg.moonflower.etched.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import gg.moonflower.etched.api.record.AlbumCover;
import gg.moonflower.etched.client.render.item.AlbumCoverItemRenderer;
import gg.moonflower.etched.client.render.item.AlbumImageProcessor;
import gg.moonflower.etched.core.Etched;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.HttpUtil;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

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

    public static CompletableFuture<AlbumCover> requestResource(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return AlbumCoverCache.getPath(url);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, HttpUtil.DOWNLOAD_EXECUTOR).thenApplyAsync(path -> {
            try (FileInputStream is = new FileInputStream(path.toFile())) {
                return AlbumCover.of(AlbumImageProcessor.apply(NativeImage.read(is), AlbumCoverItemRenderer.getOverlayImage()));
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, Util.ioPool()).handle((result, throwable) -> {
            if (throwable != null) {
                if (throwable instanceof CompletionException) {
                    throwable = throwable.getCause();
                }

                LOGGER.error("Failed to load album cover from '" + url + "'", throwable);
            }

            return result != null ? result : AlbumCover.EMPTY;
        });
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

    private static @Nullable Path getPath(String url) throws IOException {
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

        InputStream fetchedStream = get(url);
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
                    CACHE_METADATA.addProperty(key, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
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
                CACHE_METADATA.addProperty(key, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
                nextWriteTime = System.currentTimeMillis() + METADATA_WRITE_TIME;
            }
            return imageFile;
        } finally {
            IOUtils.closeQuietly(fetchedStream);
        }
    }

    public static InputStream get(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        InputStream stream = connection.getInputStream();

        if (connection.getResponseCode() != 200) {
            IOException exception = new IOException("Failed to connect to '" + url + "'. " + connection.getResponseCode() + " " + connection.getResponseMessage());
            try {
                stream.close();
            } catch (Throwable e) {
                exception.addSuppressed(e);
            }
            throw exception;
        }

        return stream;
    }
}
