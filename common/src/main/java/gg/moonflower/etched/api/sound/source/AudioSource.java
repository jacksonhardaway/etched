package gg.moonflower.etched.api.sound.source;

import gg.moonflower.etched.api.sound.download.SoundDownloadSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TranslatableComponent;
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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Sources of raw audio data to be played.
 *
 * @author Ocelot
 */
public interface AudioSource {

    Logger LOGGER = LogManager.getLogger();

    static Map<String, String> getDownloadHeaders() {
        Map<String, String> map = SoundDownloadSource.getDownloadHeaders();
        map.put("X-Minecraft-Username", Minecraft.getInstance().getUser().getName());
        map.put("X-Minecraft-UUID", Minecraft.getInstance().getUser().getUuid());
        return map;
    }

    static void downloadTo(File file, URL url, @Nullable DownloadProgressListener progressListener, Proxy proxy, boolean isTempFile) {
        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        if (progressListener != null && !file.exists()) {
            progressListener.progressStartRequest(new TranslatableComponent("resourcepack.requesting"));
        }

        try {
            Map<String, String> headers = getDownloadHeaders();
            httpURLConnection = (HttpURLConnection) url.openConnection(proxy);
            httpURLConnection.setInstanceFollowRedirects(true);
            float f = 0.0F;

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
                if (progressListener != null)
                    progressListener.progressStagePercentage((int) (++f / (float) headers.entrySet().size() * 100.0F));
            }

            inputStream = httpURLConnection.getInputStream();
            float contentLength = (float) httpURLConnection.getContentLength();
            int j = httpURLConnection.getContentLength();
            if (progressListener != null)
                progressListener.progressStartDownload(contentLength / 1000.0F / 1000.0F);

            if (file.exists()) {
                long l = file.length();
                if (l == (long) j)
                    return;

                if (!isTempFile) {
                    LOGGER.warn("Deleting {} as it does not match what we currently have ({} vs our {}).", file, j, l);
                    FileUtils.deleteQuietly(file);
                }
            } else if (!isTempFile && file.getParentFile() != null) {
                // Temp file is assumed to be created with parent directories
                file.getParentFile().mkdirs();
            }

            outputStream = new DataOutputStream(new FileOutputStream(file));
            if (contentLength > 104857600)
                throw new IOException("Filesize is bigger than maximum allowed (file is " + f + ", limit is 104857600)");

            int k;
            byte[] bs = new byte[4096];
            while ((k = inputStream.read(bs)) >= 0) {
                f += (float) k;
                if (progressListener != null) {
                    progressListener.progressStagePercentage((int) (f / contentLength * 100.0F));
                }

                if (f > 104857600)
                    throw new IOException("Filesize was bigger than maximum allowed (got >= " + f + ", limit was 104857600)");

                if (Thread.interrupted()) {
                    LOGGER.error("INTERRUPTED");
                    return;
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

            throw new CompletionException(var22);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
        }
    }

    /**
     * @return A future to a resource that will exist at some point in the future
     */
    CompletableFuture<InputStream> openStream();
}
