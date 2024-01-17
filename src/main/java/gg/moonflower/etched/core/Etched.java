package gg.moonflower.etched.core;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Etched.MOD_ID)
public class Etched {

    public static final String MOD_ID = "etched";

    public Etched() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
    }
}
