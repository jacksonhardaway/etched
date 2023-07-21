//package gg.moonflower.etched.core.forge.datagen;
//
//import com.google.common.collect.Maps;
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.JsonElement;
//import gg.moonflower.etched.core.Etched;
//import gg.moonflower.etched.core.registry.EtchedItems;
//import net.minecraft.data.DataGenerator;
//import net.minecraft.data.DataProvider;
//import net.minecraft.data.HashCache;
//import net.minecraft.data.models.model.ModelLocationUtils;
//import net.minecraft.data.models.model.ModelTemplate;
//import net.minecraft.data.models.model.ModelTemplates;
//import net.minecraft.data.models.model.TextureMapping;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.item.Item;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import java.nio.file.Path;
//import java.util.Map;
//import java.util.function.BiConsumer;
//import java.util.function.BiFunction;
//import java.util.function.Supplier;
//
//public class ItemModelGen implements DataProvider {
//
//    private static final Logger LOGGER = LogManager.getLogger();
//    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
//    private final DataGenerator dataGenerator;
//    private BiConsumer<ResourceLocation, Supplier<JsonElement>> consumer;
//
//    public ItemModelGen(DataGenerator dataGenerator) {
//        this.dataGenerator = dataGenerator;
//    }
//
//    private static Path createModelPath(Path dataFolder, ResourceLocation name) {
//        return dataFolder.resolve("assets/" + name.getNamespace() + "/models/" + name.getPath() + ".json");
//    }
//
//    @Override
//    public void run(HashCache cache) {
//        Path path = this.dataGenerator.getOutputFolder();
//        Map<ResourceLocation, Supplier<JsonElement>> map1 = Maps.newHashMap();
//        this.consumer = (location, json) ->
//        {
//            Supplier<JsonElement> supplier = map1.put(location, json);
//            if (supplier != null) {
//                throw new IllegalStateException("Duplicate model definition for " + location);
//            }
//        };
//        this.register();
//        this.saveCollection(cache, path, map1, ItemModelGen::createModelPath);
//    }
//
//    private void register() {
//        this.generateFlatItem(EtchedItems.BLANK_MUSIC_DISC.get(), ModelTemplates.FLAT_ITEM);
//        this.generateFlatItem(EtchedItems.JUKEBOX_MINECART.get(), ModelTemplates.FLAT_ITEM);
//    }
//
//    private void generateFlatItem(Item item, ModelTemplate p_240076_2_) {
//        p_240076_2_.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(item), this.consumer);
//    }
//
//    private void generateFlatItem(Item item, String p_240077_2_, ModelTemplate p_240077_3_) {
//        p_240077_3_.create(ModelLocationUtils.getModelLocation(item, p_240077_2_), TextureMapping.layer0(TextureMapping.getItemTexture(item, p_240077_2_)), this.consumer);
//    }
//
//    private void generateFlatItem(Item item, Item p_240075_2_, ModelTemplate p_240075_3_) {
//        p_240075_3_.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(p_240075_2_), this.consumer);
//    }
//
//    private <T> void saveCollection(HashCache cache, Path dataFolder, Map<T, ? extends Supplier<JsonElement>> models, BiFunction<Path, T, Path> resolver) {
//        models.forEach((p_240088_3_, p_240088_4_) ->
//        {
//            Path path = resolver.apply(dataFolder, p_240088_3_);
//
//            try {
//                DataProvider.save(GSON, cache, p_240088_4_.get(), path);
//            } catch (Exception exception) {
//                LOGGER.error("Couldn't save {}", path, exception);
//            }
//        });
//    }
//
//    @Override
//    public String getName() {
//        return Etched.MOD_ID + " Block State Definitions";
//    }
//}
