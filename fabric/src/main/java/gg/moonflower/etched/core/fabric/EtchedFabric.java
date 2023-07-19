package gg.moonflower.etched.core.fabric;

import gg.moonflower.etched.core.Etched;
import net.fabricmc.api.ModInitializer;

public class EtchedFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Etched.init();
        Etched.postInit();
    }
}
