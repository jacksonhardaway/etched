package me.jaackson.etched.fabric;

import me.jaackson.etched.Etched;
import me.jaackson.etched.EtchedRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;

/**
 * @author Jackson
 */
public class EtchedFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Etched.commonInit();
        Etched.commonPostInit();


    }

}
