package gg.moonflower.etched.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * @author Ocelot
 */
public class EntityMusicSoundInstance extends AbstractTickableSoundInstance {

    private final Entity entity;

    public EntityMusicSoundInstance(SoundEvent soundEvent, Entity entity) {
        super(soundEvent, SoundSource.RECORDS);
        this.volume = 4.0F;
        this.entity = entity;
    }

    @Override
    public void tick() {
        if (!this.entity.isAlive()) {
            this.stop();
        } else {
            this.x = this.entity.getX();
            this.y = this.entity.getY();
            this.z = this.entity.getZ();
        }
    }
}
