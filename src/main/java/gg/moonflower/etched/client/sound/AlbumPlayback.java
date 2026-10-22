package gg.moonflower.etched.client.sound;

/**
 * The client side progress through the discs of an album jukebox that isn't a placed block, such as one on a moving contraption.
 */
public interface AlbumPlayback {

    /**
     * @return The slot of the disc playing or <code>-1</code> if nothing is
     */
    int getPlayingIndex();

    /**
     * @return The track playing on the current disc
     */
    int getTrack();

    /**
     * Skips to the next track.
     */
    void next();

    /**
     * Goes back to the previous track.
     */
    void previous();
}
