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

    public OnlineRecordSoundInstance(String url, Entity entity, float volume, int attenuationDistance, DownloadProgressListener progressListener, AudioSource.AudioFileType type) {
        super(url, null, attenuationDistance, SoundSource.RECORDS, progressListener, type, entity == Minecraft.getInstance().player);
        this.volume = volume;
        this.entity = entity;
    }

    public OnlineRecordSoundInstance(String url, Entity entity, int attenuationDistance, DownloadProgressListener progressListener, AudioSource.AudioFileType type) {
        this(url, entity, 4.0F, attenuationDistance, progressListener, type);
    }

    public OnlineRecordSoundInstance(String url, double x, double y, double z, float volume, int attenuationDistance, DownloadProgressListener progressListener, AudioSource.AudioFileType type) {
        this(url, null, volume, attenuationDistance, progressListener, type);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public OnlineRecordSoundInstance(String url, double x, double y, double z, int attenuationDistance, DownloadProgressListener progressListener, AudioSource.AudioFileType type) {
        this(url, x, y, z, 4.0F, attenuationDistance, progressListener, type);
    }

    @Override
    public void tick() {
        if (this.entity == null) {
            return;
        }

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
        return this.stopped;
    }
}
