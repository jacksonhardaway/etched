package gg.moonflower.etched.core.forge;

import dev.architectury.platform.forge.EventBuses;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.EtchedClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Etched.MOD_ID)
public class EtchedForge {

    public EtchedForge() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(Etched.MOD_ID, eventBus);
        eventBus.addListener(this::commonInit);
        eventBus.addListener(this::clientInit);

        Etched.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> EtchedClient::init);
    }

    private void commonInit(FMLCommonSetupEvent event) {
        Etched.postInit();
    }

    private void clientInit(FMLClientSetupEvent event) {
        EtchedClient.postInit();
    }

//    private void dataSetup(GatherDataEvent event) {
//        DataGenerator dataGenerator = event.getGenerator();
//        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
//        dataGenerator.addProvider(new RecipeGen(dataGenerator));
//        dataGenerator.addProvider(new LootTableGen(dataGenerator));
//        dataGenerator.addProvider(new ItemModelGen(dataGenerator));
//        dataGenerator.addProvider(new ItemTagGen(dataGenerator, new BlockTagsProvider(dataGenerator, Etched.MOD_ID, existingFileHelper), existingFileHelper));
//        dataGenerator.addProvider(new LanguageGen(dataGenerator));
//    }
}
