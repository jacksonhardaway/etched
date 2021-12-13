package gg.moonflower.etched.client.sound.download;

import net.minecraft.network.chat.Component;

/**
 * @author Ocelot
 */
public interface DownloadProgressListener {

    /**
     * Called when a request is started.
     *
     * @param component The component to display
     */
    void progressStartRequest(Component component);

    /**
     * Called when data download begins.
     *
     * @param size The total size of the file in megabytes.
     */
    void progressStartDownload(float size);

    /**
     * Called each time a byte is downloaded.
     *
     * @param percentage The percent downloaded for the file
     */
    void progressStagePercentage(int percentage);

    /**
     * Called when the file starts loading.
     */
    void progressStartLoading();

    /**
     * Called when the file successfully loads.
     */
    void onSuccess();

    /**
     * Called when there is an error and the file fails to load.
     */
    void onFail();
}
