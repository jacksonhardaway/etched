package gg.moonflower.etched.client.render.item;

import com.mojang.blaze3d.platform.NativeImage;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.pollen.api.util.OnlineRequest;
import gg.moonflower.pollen.pinwheel.api.client.FileCache;
import gg.moonflower.pollen.pinwheel.api.client.geometry.GeometryCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@ApiStatus.Internal
public class AlbumTextureCache implements FileCache {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Executor executor;
    private final long cacheTime;
    private final TimeUnit cacheTimeUnit;

    public AlbumTextureCache(Executor executor, long cacheTime, TimeUnit cacheTimeUnit) {
        this.executor = executor;
        this.cacheTime = cacheTime;
        this.cacheTimeUnit = cacheTimeUnit;
    }

    @Override
    public CompletableFuture<Path> requestResource(String url, boolean ignoreMissing) {
        return CompletableFuture.supplyAsync(() ->
        {
            try {
                return GeometryCache.getPath(url, this.cacheTime, this.cacheTimeUnit, s ->
                {
                    try (InputStream is = OnlineRequest.get(url)) {
                        Path tempFile = Files.createTempFile(Etched.MOD_ID + "-album-cover", null);
                        AlbumImageProcessor.apply(NativeImage.read(is), AlbumCoverItemRenderer.getOverlayImage(), 1).writeToFile(tempFile);
                        return new FileInputStream(tempFile.toFile()) {
                            @Override
                            public void close() throws IOException {
                                super.close();
                                Files.delete(tempFile);
                            }
                        };
                    } catch (IOException e) {
                        if (!ignoreMissing)
                            LOGGER.error("Failed to read data from '" + url + "'", e);
                        return null;
                    }
                });
            } catch (Exception e) {
                if (!ignoreMissing)
                    LOGGER.error("Failed to fetch resource from '" + url + "'", e);
                return null;
            }
        }, this.executor);
    }
}
