package gg.moonflower.etched.core;

import gg.moonflower.etched.api.sound.download.SoundSourceManager;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.sound.download.BandcampSource;
import gg.moonflower.etched.common.sound.download.SoundCloudSource;
import gg.moonflower.etched.core.registry.*;
import gg.moonflower.pollen.api.config.v1.ConfigManager;
import gg.moonflower.pollen.api.config.v1.PollinatedConfigType;

/**
 * @author Ocelot, Jackson
 */
public class Etched {

    public static final String MOD_ID = "etched";
    public static final EtchedConfig.Client CLIENT_CONFIG = ConfigManager.register(Etched.MOD_ID, PollinatedConfigType.CLIENT, EtchedConfig.Client::new);
    public static final EtchedConfig.Server SERVER_CONFIG = ConfigManager.register(Etched.MOD_ID, PollinatedConfigType.SERVER, EtchedConfig.Server::new);

    public static void init() {
        EtchedBlocks.BLOCKS.register();
        EtchedBlocks.BLOCK_ENTITIES.register();

        EtchedItems.REGISTRY.register();
        EtchedEntities.REGUSTRY.register();
        EtchedMenus.REGISTRY.register();
        EtchedSounds.REGISTRY.register();
        EtchedRecipes.REGISTRY.register();

        EtchedVillagers.REGISTRY.register();
        EtchedVillagers.registerTrades();
    }

    public static void postInit() {
        EtchedMessages.init();
        EtchedVillagers.registerVillages();

        SoundSourceManager.registerSource(new SoundCloudSource());
        SoundSourceManager.registerSource(new BandcampSource());
    }
}
