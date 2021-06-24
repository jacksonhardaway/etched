package me.jaackson.etched.client.sound.download;

import net.minecraft.network.chat.Component;

/**
 * @author Ocelot
 */
public class AccumulatingDownloadProgressListener implements DownloadProgressListener {

    private final DownloadProgressListener parent;
    private final int count;
    private boolean started;
    private int success;
    private int sizesReceived;

    public AccumulatingDownloadProgressListener(DownloadProgressListener parent, int count) {
        this.parent = parent;
        this.count = count;
    }

    @Override
    public void progressStartRequest(Component component) {
        if (!this.started) {
            this.parent.progressStartRequest(component);
            this.started = true;
        }
    }

    @Override
    public void progressStartDownload(float size) {
        this.sizesReceived++;
        if (this.sizesReceived >= this.count)
            this.parent.progressStartDownload(size);
    }

    @Override
    public void progressStagePercentage(int percentage) {
    }

    @Override
    public void progressStartLoading() {
        if (this.sizesReceived >= this.count)
            this.parent.progressStartLoading();
    }

    @Override
    public void onSuccess() {
        this.success++;
        if (this.sizesReceived >= this.count)
            this.parent.progressStagePercentage((int) ((float) this.success / (float) this.sizesReceived * 100.0F));
        if (this.success >= this.count)
            this.parent.onSuccess();
    }

    @Override
    public void onFail() {
        this.parent.onFail();
    }
}
