package gg.moonflower.etched.api.sound;

import gg.moonflower.etched.api.util.DownloadProgressListener;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * @author Ocelot
 */
public class OnlineRecordSoundInstance extends AbstractOnlineSoundInstance implements TickableSoundInstance {

    private final Entity entity;
    private boolean stopped;

    public OnlineRecordSoundInstance(String url, Entity entity, DownloadProgressListener progressListener) {
        super(url, null, SoundSource.RECORDS, progressListener);
        this.volume = 4.0F;
        this.entity = entity;
    }

    public OnlineRecordSoundInstance(String url, double x, double y, double z, DownloadProgressListener progressListener) {
        this(url, null, progressListener);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void tick() {
        if (this.entity == null)
            return;

        if (!this.entity.isAlive()) {
            this.stopped = true;
        } else {
            this.x = this.entity.getX();
            this.y = this.entity.getY();
            this.z = this.entity.getZ();
        }
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }
}
