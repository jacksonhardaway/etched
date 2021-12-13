package gg.moonflower.etched.client.sound.download;

import gg.moonflower.etched.client.sound.source.RawAudioSource;
import gg.moonflower.etched.client.sound.source.StreamingAudioSource;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.client.sound.source.AudioSource;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.util.HttpUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
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
    @Environment(EnvType.CLIENT)
    public static CompletableFuture<AudioSource> getAudioStream(String url, @Nullable DownloadProgressListener listener) {
        if (DOWNLOADING.containsKey(url)) {
            CompletableFuture<AudioSource> future = DOWNLOADING.get(url);
            if (!future.isDone())
                return future;
        }

        try {
            LOCK.lock();

            boolean soundCloud = SoundCloud.isValidUrl(url);
            CompletableFuture<AudioSource> future = (soundCloud ? CompletableFuture.supplyAsync(() -> {
                try {
                    return SoundCloud.resolveUrl(url, listener, Minecraft.getInstance().getProxy()).toArray(new URL[0]);
                } catch (Exception e) {
                    throw new CompletionException("Failed to connect to SoundCloud API", e);
                }
            }, HttpUtil.DOWNLOAD_EXECUTOR) : CompletableFuture.completedFuture(new URL[]{new URL(url)})).thenApplyAsync(urls -> {
                try {
                    if (urls.length == 0)
                        throw new IOException("No audio data was found at the source!");
                    if (urls.length == 1)
                        return new RawAudioSource(Minecraft.getInstance().getProxy(), DigestUtils.sha1Hex(url), urls[0], soundCloud);
                    return new StreamingAudioSource(Minecraft.getInstance().getProxy(), DigestUtils.sha1Hex(url), urls, listener, soundCloud);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }, HttpUtil.DOWNLOAD_EXECUTOR).handle((source, e) -> {
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

//    private static CompletableFuture<?> downloadTo(File file, CompletableFuture<String> urlFuture, Map<String, String> map, int i, @Nullable DownloadProgressListener progressListener, Proxy proxy, boolean isTempFile) {
//        return urlFuture.thenApplyAsync(url -> {
//            HttpURLConnection httpURLConnection = null;
//            InputStream inputStream = null;
//            OutputStream outputStream = null;
//            if (progressListener != null && !file.exists()) {
//                progressListener.progressStartRequest(new TranslatableComponent("resourcepack.requesting"));
//            }
//
//            try {
//                byte[] bs = new byte[4096];
//                URL uRL = new URL(url);
//                httpURLConnection = (HttpURLConnection) uRL.openConnection(proxy);
//                httpURLConnection.setInstanceFollowRedirects(true);
//                float f = 0.0F;
//                float g = (float) map.entrySet().size();
//
//                for (Map.Entry<String, String> entry : map.entrySet()) {
//                    httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
//                    if (progressListener != null)
//                        progressListener.progressStagePercentage((int) (++f / g * 100.0F));
//                }
//
//                inputStream = httpURLConnection.getInputStream();
//                g = (float) httpURLConnection.getContentLength();
//                int j = httpURLConnection.getContentLength();
//                if (progressListener != null)
//                    progressListener.progressStartDownload(g / 1000.0F / 1000.0F);
//
//                if (file.exists()) {
//                    long l = file.length();
//                    if (l == (long) j)
//                        return null;
//
//                    if (!isTempFile) {
//                        LOGGER.warn("Deleting {} as it does not match what we currently have ({} vs our {}).", file, j, l);
//                        FileUtils.deleteQuietly(file);
//                    }
//                } else if (!isTempFile && file.getParentFile() != null) {
//                    // Temp file is assumed to be created with parent directories
//                    file.getParentFile().mkdirs();
//                }
//
//                outputStream = new DataOutputStream(new FileOutputStream(file));
//                if (i > 0 && g > (float) i)
//                    throw new IOException("Filesize is bigger than maximum allowed (file is " + f + ", limit is " + i + ")");
//
//                int k;
//                while ((k = inputStream.read(bs)) >= 0) {
//                    f += (float) k;
//                    if (progressListener != null) {
//                        progressListener.progressStagePercentage((int) (f / g * 100.0F));
//                    }
//
//                    if (i > 0 && f > (float) i)
//                        throw new IOException("Filesize was bigger than maximum allowed (got >= " + f + ", limit was " + i + ")");
//
//                    if (Thread.interrupted()) {
//                        LOGGER.error("INTERRUPTED");
//                        return null;
//                    }
//
//                    outputStream.write(bs, 0, k);
//                }
//            } catch (Throwable var22) {
//                var22.printStackTrace();
//                if (httpURLConnection != null) {
//                    InputStream inputStream2 = httpURLConnection.getErrorStream();
//
//                    try {
//                        LOGGER.error(IOUtils.toString(inputStream2, StandardCharsets.UTF_8));
//                    } catch (IOException var21) {
//                        var21.printStackTrace();
//                    }
//                }
//
//                throw new CompletionException(var22);
//            } finally {
//                IOUtils.closeQuietly(inputStream);
//                IOUtils.closeQuietly(outputStream);
//            }
//
//            return null;
//        }, HttpUtil.DOWNLOAD_EXECUTOR);
//    }
}
