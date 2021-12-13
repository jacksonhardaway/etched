package gg.moonflower.etched.core.fabric;

import gg.moonflower.etched.core.Etched;
import net.fabricmc.api.ModInitializer;

/**
 * @author Jackson
 */
public class EtchedFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Etched.PLATFORM.setup();
    }

}
