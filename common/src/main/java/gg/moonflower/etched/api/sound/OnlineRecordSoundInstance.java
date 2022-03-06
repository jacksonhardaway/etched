package gg.moonflower.etched.api.sound;

import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * @author Ocelot
 */
public class OnlineRecordSoundInstance extends AbstractOnlineSoundInstance implements TickableSoundInstance {

    private final Entity entity;
    private boolean stopped;

    public OnlineRecordSoundInstance(String url, Entity entity, DownloadProgressListener progressListener, AudioSource.AudioFileType type) {
        super(url, null, SoundSource.RECORDS, progressListener, type, entity == Minecraft.getInstance().player);
        this.volume = 4.0F;
        this.entity = entity;
    }

    public OnlineRecordSoundInstance(String url, double x, double y, double z, DownloadProgressListener progressListener, AudioSource.AudioFileType type) {
        this(url, null, progressListener, type);
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
