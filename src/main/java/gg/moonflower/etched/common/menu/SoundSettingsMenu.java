package gg.moonflower.etched.common.menu;

public interface SoundSettingsMenu {

    /**
     * The smallest radius in blocks a music player can be heard from.
     */
    int MIN_RADIUS = 4;

    /**
     * The largest radius in blocks a music player can be heard from.
     */
    int MAX_RADIUS = 128;

    /**
     * Sets how loud the music player behind this menu is.
     *
     * @param volume The new volume from 0 to 1
     */
    void setVolume(float volume);

    /**
     * Sets how far away the music player behind this menu can be heard.
     *
     * @param radius The new radius in blocks
     */
    void setRadius(int radius);
}
