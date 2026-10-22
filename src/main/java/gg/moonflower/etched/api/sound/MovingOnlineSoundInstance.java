package gg.moonflower.etched.api.sound;

import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * An online sound that follows an arbitrary position, such as a block mounted on a moving contraption.
 *
 * @since 5.4.0
 */
public class MovingOnlineSoundInstance extends AbstractOnlineSoundInstance implements TickableSoundInstance {

    private final Supplier<@Nullable Vec3> position;
    private boolean stopped;

    /**
     * @param position The current position of the sound or <code>null</code> to stop playing
     */
    public MovingOnlineSoundInstance(String url, Supplier<@Nullable Vec3> position, float volume, int attenuationDistance, DownloadProgressListener progressListener, AudioSource.AudioFileType type) {
        super(url, null, attenuationDistance, SoundSource.RECORDS, progressListener, type, false);
        this.volume = volume;
        this.position = position;
        this.tick();
    }

    @Override
    public void tick() {
        Vec3 pos = this.position.get();
        if (pos == null) {
            this.stopped = true;
        } else {
            this.x = pos.x;
            this.y = pos.y;
            this.z = pos.z;
        }
    }

    @Override
    public boolean isStopped() {
        return this.stopped;
    }
}
