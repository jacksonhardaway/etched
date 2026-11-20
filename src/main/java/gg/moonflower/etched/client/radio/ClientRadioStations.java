package gg.moonflower.etched.client.radio;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public final class ClientRadioStations {

    private static final Logger LOGGER = LogManager.getLogger();
    private static RadioStationLibrary library;

    private ClientRadioStations() {
    }

    public static RadioStationLibrary get() {
        if (library == null) {
            library = new RadioStationLibrary(Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("config")
                    .resolve("etched-radio-stations.json"));
            try {
                library.load();
            } catch (IOException exception) {
                LOGGER.error("Failed to load the personal radio station library", exception);
            }
        }
        return library;
    }
}
