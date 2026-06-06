package gg.moonflower.etched.core.compat;

import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.DiscHandlerRegistry;

public class SophisticatedCoreCompat {

    public static void load(){
        DiscHandlerRegistry.registerHandler(new EtchedDiscHandler());
    }
}
