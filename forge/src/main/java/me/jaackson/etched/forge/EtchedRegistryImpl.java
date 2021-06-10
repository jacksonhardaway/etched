package me.jaackson.etched.forge;

import me.jaackson.etched.bridge.forge.RegistryBridgeImpl;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class EtchedRegistryImpl {
    public static void register() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        RegistryBridgeImpl.ITEMS.register(bus);
        RegistryBridgeImpl.BLOCKS.register(bus);
        RegistryBridgeImpl.MENU_TYPES.register(bus);
        RegistryBridgeImpl.SOUND_EVENTS.register(bus);
    }
}
