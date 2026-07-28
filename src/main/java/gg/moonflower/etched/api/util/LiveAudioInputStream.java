package gg.moonflower.etched.api.util;

import java.io.FilterInputStream;
import java.io.InputStream;

/**
 * Marker wrapper for an unbounded live audio source.  Used by
 * {@link gg.moonflower.etched.api.sound.AbstractOnlineSoundInstance} to
 * detect live streams at the type level and skip {@code LoopingAudioStream}
 * wrapping — an infinite live stream never reaches EOF, so looping is both
 * unnecessary and harmful (it would attempt to rewind the same stream).
 *
 * Local files do not receive this wrapper and keep the
 * original looping behaviour.
 */
public final class LiveAudioInputStream extends FilterInputStream {

    /**
     * @param input the underlying live audio source opened by
     *              {@link gg.moonflower.etched.api.sound.source.AudioSource#openLiveConnection}
     */
    public LiveAudioInputStream(InputStream input) {
        super(input);
    }
}
