package gg.moonflower.etched.client.sound.format;

import java.io.IOException;

/**
 * <p>An input stream capable of seeking through data.</p>
 *
 * @author Ocelot
 */
public interface SeekingStream {

    /**
     * Sets the position of the data to the start.
     *
     * @throws IOException If an error occurs
     */
    void beginning() throws IOException;
}
