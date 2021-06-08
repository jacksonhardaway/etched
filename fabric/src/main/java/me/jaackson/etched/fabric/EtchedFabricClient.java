package me.jaackson.etched.fabric;

import me.jaackson.etched.Etched;
import net.fabricmc.api.ClientModInitializer;

/**
 * @author Jackson
 */
public class EtchedFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Etched.clientInit();
        Etched.clientPostInit();
    }
}
