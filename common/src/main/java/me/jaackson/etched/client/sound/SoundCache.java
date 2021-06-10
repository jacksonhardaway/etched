package me.jaackson.etched.client.sound;

import me.jaackson.etched.Etched;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.HttpUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Ocelot
 */
public class SoundCache {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Path SOUND_FOLDER = Minecraft.getInstance().gameDirectory.toPath().resolve(Etched.MOD_ID + "-sounds");
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final Map<String, CompletableFuture<Path>> DOWNLOADING = new HashMap<>();

    private static Map<String, String> getDownloadHeaders() {
        Map<String, String> map = new HashMap<>();
        map.put("X-Minecraft-Username", Minecraft.getInstance().getUser().getName());
        map.put("X-Minecraft-UUID", Minecraft.getInstance().getUser().getUuid());
        map.put("X-Minecraft-Version", SharedConstants.getCurrentVersion().getName());
        map.put("X-Minecraft-Version-ID", SharedConstants.getCurrentVersion().getId());
        map.put("User-Agent", "Minecraft Java/" + SharedConstants.getCurrentVersion().getName());
        return map;
    }

    /**
     * Downloads an audio stream from the specified URL and stores it in a local cache.
     *
     * @param url The url to download the sound from
     * @return An input stream to the locally downloaded file
     */
    public static CompletableFuture<Path> getAudioStream(String url, @Nullable DownloadProgressListener progressListener) {
        if (DOWNLOADING.containsKey(url)) {
            CompletableFuture<Path> future = DOWNLOADING.get(url);
            if (!future.isDone())
                return future;
        }

        try {
            LOCK.lock();

            Path file = SOUND_FOLDER.resolve(DigestUtils.md5Hex(url));
            try {
                if (!Files.exists(SOUND_FOLDER))
                    Files.createDirectories(SOUND_FOLDER);
            } catch (Exception e) {
                if (progressListener != null)
                    progressListener.onFail();
                throw new CompletionException(e);
            }

            // TODO add config for file size download limit?
            CompletableFuture<Path> future = downloadTo(file.toFile(), url, getDownloadHeaders(), 104857600, progressListener, Minecraft.getInstance().getProxy()).thenApplyAsync(__ -> {
                try {
                    if (!Files.exists(file))
                        throw new FileNotFoundException();
                    if (progressListener != null)
                        progressListener.onSuccess();
                    return file;
                } catch (Exception e) {
                    if (progressListener != null)
                        progressListener.onFail();
                    throw new CompletionException(e);
                }
            }, Util.ioPool()).exceptionally(e -> {
                if (progressListener != null)
                    progressListener.onFail();
                return null;
            }).thenApplyAsync(path -> {
                DOWNLOADING.remove(url);
                return path;
            }, Minecraft.getInstance());

            DOWNLOADING.put(url, future);
            return future;
        } finally {
            LOCK.unlock();
        }
    }

    private static CompletableFuture<?> downloadTo(File file, String string, Map<String, String> map, int i, @Nullable DownloadProgressListener progressListener, Proxy proxy) {
        return CompletableFuture.supplyAsync(() -> {
            HttpURLConnection httpURLConnection = null;
            InputStream inputStream = null;
            OutputStream outputStream = null;
            if (progressListener != null && !file.exists()) {
                progressListener.progressStartRequest(new TranslatableComponent("resourcepack.requesting"));
            }

            try {
                try {
                    byte[] bs = new byte[4096];
                    URL uRL = new URL(string);
                    httpURLConnection = (HttpURLConnection) uRL.openConnection(proxy);
                    httpURLConnection.setInstanceFollowRedirects(true);
                    float f = 0.0F;
                    float g = (float) map.entrySet().size();

                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
                        if (progressListener != null)
                            progressListener.progressStagePercentage((int) (++f / g * 100.0F));
                    }

                    inputStream = httpURLConnection.getInputStream();
                    g = (float) httpURLConnection.getContentLength();
                    int j = httpURLConnection.getContentLength();
                    if (progressListener != null)
                        progressListener.progressStartDownload(g / 1000.0F / 1000.0F);

                    if (file.exists()) {
                        long l = file.length();
                        if (l == (long) j)
                            return null;

                        LOGGER.warn("Deleting {} as it does not match what we currently have ({} vs our {}).", file, j, l);
                        FileUtils.deleteQuietly(file);
                    } else if (file.getParentFile() != null) {
                        file.getParentFile().mkdirs();
                    }

                    if (progressListener != null)
                        progressListener.progressStartRequest(new TranslatableComponent("resourcepack.requesting"));

                    outputStream = new DataOutputStream(new FileOutputStream(file));
                    if (i > 0 && g > (float) i)
                        throw new IOException("Filesize is bigger than maximum allowed (file is " + f + ", limit is " + i + ")");

                    int k;
                    while ((k = inputStream.read(bs)) >= 0) {
                        f += (float) k;
                        if (progressListener != null) {
                            progressListener.progressStagePercentage((int) (f / g * 100.0F));
                        }

                        if (i > 0 && f > (float) i)
                            throw new IOException("Filesize was bigger than maximum allowed (got >= " + f + ", limit was " + i + ")");

                        if (Thread.interrupted()) {
                            LOGGER.error("INTERRUPTED");
                            return null;
                        }

                        outputStream.write(bs, 0, k);
                    }
                } catch (Throwable var22) {
                    var22.printStackTrace();
                    if (httpURLConnection != null) {
                        InputStream inputStream2 = httpURLConnection.getErrorStream();

                        try {
                            LOGGER.error(IOUtils.toString(inputStream2, StandardCharsets.UTF_8));
                        } catch (IOException var21) {
                            var21.printStackTrace();
                        }
                    }
                }

                return null;
            } finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }, HttpUtil.DOWNLOAD_EXECUTOR);
    }
}
