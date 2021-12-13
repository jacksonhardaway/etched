package gg.moonflower.etched.client.sound;

import gg.moonflower.etched.common.entity.MinecartJukebox;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * @author Ocelot
 */
public class JukeboxMinecartSoundInstance extends AbstractTickableSoundInstance {

    private final MinecartJukebox jukebox;

    public JukeboxMinecartSoundInstance(SoundEvent soundEvent, MinecartJukebox jukebox) {
        super(soundEvent, SoundSource.RECORDS);
        this.volume = 4.0F;
        this.jukebox = jukebox;
    }

    @Override
    public void tick() {
        if (!this.jukebox.isAlive()) {
            this.stop();
        } else {
            this.x = this.jukebox.getX();
            this.y = this.jukebox.getY();
            this.z = this.jukebox.getZ();
        }
    }
}
