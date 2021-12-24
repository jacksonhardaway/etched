package gg.moonflower.etched.core.forge;

import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.forge.datagen.ItemModelGen;
import gg.moonflower.etched.core.forge.datagen.ItemTagGen;
import gg.moonflower.etched.core.forge.datagen.LanguageGen;
import gg.moonflower.etched.core.forge.datagen.LootTableGen;
import gg.moonflower.etched.core.forge.datagen.RecipeGen;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

/**
 * @author Jackson
 */
@Mod(Etched.MOD_ID)
public class EtchedForge {

    public EtchedForge() {
        Etched.PLATFORM.setup();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::dataSetup);
    }

    private void dataSetup(GatherDataEvent event) {
        DataGenerator dataGenerator = event.getGenerator();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        dataGenerator.addProvider(new RecipeGen(dataGenerator));
        dataGenerator.addProvider(new LootTableGen(dataGenerator));
        dataGenerator.addProvider(new ItemModelGen(dataGenerator));
        dataGenerator.addProvider(new ItemTagGen(dataGenerator, new BlockTagsProvider(dataGenerator, Etched.MOD_ID, existingFileHelper), existingFileHelper));
        dataGenerator.addProvider(new LanguageGen(dataGenerator));
    }
}
