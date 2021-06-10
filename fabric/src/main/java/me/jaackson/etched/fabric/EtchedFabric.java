package me.jaackson.etched.fabric;

import me.jaackson.etched.Etched;
import net.fabricmc.api.ModInitializer;

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
