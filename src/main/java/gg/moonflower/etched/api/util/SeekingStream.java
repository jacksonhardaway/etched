package gg.moonflower.etched.api.util;

import java.io.IOException;

/**
 * An input stream capable of seeking through data.
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
