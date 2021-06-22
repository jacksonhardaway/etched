package me.jaackson.etched.client.sound;

import me.jaackson.etched.client.sound.download.DownloadProgressListener;
import me.jaackson.etched.common.entity.MinecartJukebox;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ocelot
 */
public class OnlineRecordSoundInstance extends AbstractOnlineSoundInstance implements TickableSoundInstance {

    private final MinecartJukebox jukebox;
    private boolean stopped;

    public OnlineRecordSoundInstance(String url, MinecartJukebox jukebox, DownloadProgressListener progressListener) {
        super(url, null, SoundSource.RECORDS, progressListener);
        this.volume = 4.0F;
        this.jukebox = jukebox;
    }

    public OnlineRecordSoundInstance(String url, double x, double y, double z, DownloadProgressListener progressListener) {
        this(url, null, progressListener);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void tick() {
        if (this.jukebox == null)
            return;

        if (!this.jukebox.isAlive()) {
            this.stopped = true;
        } else {
            this.x = this.jukebox.getX();
            this.y = this.jukebox.getY();
            this.z = this.jukebox.getZ();
        }
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }
}
