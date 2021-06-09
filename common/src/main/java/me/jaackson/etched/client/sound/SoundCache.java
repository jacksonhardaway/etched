package me.jaackson.etched.client.sound;

import me.jaackson.etched.Etched;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.ProgressListener;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.FileInputStream;
import java.io.InputStream;
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
    private static final Map<String, CompletableFuture<InputStream>> DOWNLOADING = new HashMap<>();

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
    public static CompletableFuture<InputStream> getAudioStream(String url, @Nullable DownloadProgressListener progressListener) {
        if (DOWNLOADING.containsKey(url)) {
            CompletableFuture<InputStream> future = DOWNLOADING.get(url);
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

            CompletableFuture<InputStream> future = HttpUtil.downloadTo(file.toFile(), url, getDownloadHeaders(), 104857600, progressListener != null ? new ProgressListener() {
                @Override
                public void progressStartNoAbort(Component component) {
                }

                @Override
                public void progressStart(Component component) {
                }

                @Override
                public void progressStage(Component component) {
                    if (component instanceof TranslatableComponent) {
                        Object[] args = ((TranslatableComponent) component).getArgs();
                        if (args.length > 0) {
                            if (args[0] instanceof String && NumberUtils.isCreatable((String) args[0])) {
                                progressListener.progressStartDownload(NumberUtils.createNumber((String) args[0]).floatValue());
                            }
                        } else if (!Files.exists(file)) {
                            progressListener.progressStartRequest(component);
                        }
                    }
                }

                @Override
                public void progressStagePercentage(int i) {
                    progressListener.progressStagePercentage(i);
                }

                @Override
                public void stop() {
                }
            } : null, Minecraft.getInstance().getProxy()).<InputStream>thenApplyAsync(__ -> {
                try {
                    FileInputStream is = new FileInputStream(file.toFile());
                    if (progressListener != null)
                        progressListener.onSuccess();
                    return is;
                } catch (Exception e) {
                    if (progressListener != null)
                        progressListener.onFail();
                    throw new CompletionException(e);
                }
            }, Util.ioPool()).exceptionally(e -> {
                if (progressListener != null)
                    progressListener.onFail();
                return null;
            }).thenApplyAsync(stream -> {
                DOWNLOADING.remove(url);
                return stream;
            }, Minecraft.getInstance());
            DOWNLOADING.put(url, future);
            return future;
        } finally {
            LOCK.unlock();
        }
    }
}
