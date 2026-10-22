package gg.moonflower.etched.common.menu;

/**
 * A music player inventory that also stores how its music sounds.
 */
public interface SoundSettingsContainer {

    /**
     * @param volume The new volume from 0 to 1
     */
    void setVolume(float volume);

    /**
     * @param radius The new radius in blocks
     */
    void setRadius(int radius);
}
