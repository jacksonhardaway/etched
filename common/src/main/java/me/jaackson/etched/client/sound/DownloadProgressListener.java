package me.jaackson.etched.client.sound;

import net.minecraft.network.chat.Component;

/**
 * @author Ocelot
 */
public interface DownloadProgressListener {

    void progressStartRequest(Component component);

    void progressStartDownload(int size);

    void progressStagePercentage(int percentage);

    void onSuccess();

    void onFail();
}
