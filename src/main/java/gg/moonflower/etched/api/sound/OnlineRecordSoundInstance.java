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
    private float radioVolume;
    private boolean radioMuted;
    private boolean radioSettingsEnabled;
    private int radioRange = 32;

    public OnlineRecordSoundInstance(String url, Entity entity, float volume, int attenuationDistance, DownloadProgressListener progressListener, AudioSource.AudioFileType type) {
        super(url, null, attenuationDistance, SoundSource.RECORDS, progressListener, type, entity == Minecraft.getInstance().player);
        this.volume = volume;
        this.radioVolume = volume;
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
        if (this.radioSettingsEnabled) {
            this.updateRadioVolume();
        }
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

    public void setRadioMuted(boolean muted) {
        this.radioMuted = muted;
        this.updateRadioVolume();
    }

    public void setRadioVolume(float volume) {
        this.setRadioSettings(volume, this.radioRange);
    }

    public void setRadioSettings(float volume, int range) {
        this.radioSettingsEnabled = true;
        this.radioVolume = Math.max(0.0F, Math.min(1.0F, volume));
        this.radioRange = Math.max(1, Math.min(32, range));
        this.updateRadioVolume();
    }

    private void updateRadioVolume() {
        if (this.radioMuted) {
            this.volume = 0.0F;
            return;
        }
        if (!this.radioSettingsEnabled || Minecraft.getInstance().player == null) {
            this.volume = this.radioVolume;
            return;
        }

        double distance = Math.sqrt(Minecraft.getInstance().player.distanceToSqr(this.x, this.y, this.z));
        double desiredFalloff = Math.max(0.0D, 1.0D - distance / this.radioRange);
        double nativeFalloff = Math.max(0.0D, 1.0D - distance / 32.0D);
        this.volume = desiredFalloff <= 0.0D || nativeFalloff <= 0.0001D
                ? 0.0F
                : (float) Math.max(0.0D, Math.min(1.0D, this.radioVolume * desiredFalloff / nativeFalloff));
    }
}
