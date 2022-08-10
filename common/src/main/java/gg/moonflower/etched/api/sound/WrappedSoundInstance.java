package gg.moonflower.etched.api.sound;

import net.minecraft.client.resources.sounds.SoundInstance;

/**
 * Provides the source sound instance for a wrapped sound instance
 * @author Jackson
 */
public interface WrappedSoundInstance {

    /**
     * @return The parent sound instance
     */
    SoundInstance getParent();
}
