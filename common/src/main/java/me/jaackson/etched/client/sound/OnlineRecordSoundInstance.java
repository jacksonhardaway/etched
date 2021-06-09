package me.jaackson.etched.client.sound;

import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ocelot
 */
public class OnlineRecordSoundInstance extends AbstractOnlineSoundInstance {

    public OnlineRecordSoundInstance(String url, double x, double y, double z, SoundSource source, @Nullable DownloadProgressListener progressListener) {
        super(url, null, source, progressListener);
        this.volume = 4.0F;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
