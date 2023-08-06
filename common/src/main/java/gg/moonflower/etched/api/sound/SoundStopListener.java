package gg.moonflower.etched.api.sound;

/**
 * <p>Listener for when the sound engine discards a sound after it has stopped.</p>
 *
 * @author Ocelot
 */
@FunctionalInterface
public interface SoundStopListener {

    /**
     * Called just before the sound is removed from the map.
     */
    void onStop();
}
