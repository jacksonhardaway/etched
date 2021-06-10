package me.jaackson.etched.forge;

import me.jaackson.etched.Etched;
import me.jaackson.etched.datagen.ItemModelGen;
import me.jaackson.etched.datagen.LanguageGen;
import me.jaackson.etched.datagen.LootTableGen;
import me.jaackson.etched.datagen.RecipeGen;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * @author Jackson
 */
@Mod(Etched.MOD_ID)
public class EtchedForge {

    public EtchedForge() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::commonSetup);
        bus.addListener(this::dataSetup);
        bus.addListener(this::clientSetup);

        Etched.commonInit();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> Etched::clientInit);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Etched::commonPostInit);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(Etched::clientPostInit);
    }

    private void dataSetup(GatherDataEvent event) {
        DataGenerator dataGenerator = event.getGenerator();
        dataGenerator.addProvider(new RecipeGen(dataGenerator));
        dataGenerator.addProvider(new LootTableGen(dataGenerator));
        dataGenerator.addProvider(new ItemModelGen(dataGenerator));
        dataGenerator.addProvider(new LanguageGen(dataGenerator));
    }
}
