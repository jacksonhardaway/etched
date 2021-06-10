package me.jaackson.etched.datagen;

import me.jaackson.etched.EtchedRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

public class RecipeGen extends RecipeProvider {

    public RecipeGen(DataGenerator generator) {
        super(generator);
    }

    @Override
    protected void buildShapelessRecipes(Consumer<FinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(EtchedRegistry.ALBUM_JUKEBOX.get())
                .pattern("RHR")
                .pattern("RJR")
                .pattern("RCR")
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('H', Items.HOPPER)
                .define('J', Items.JUKEBOX)
                .define('C', Tags.Items.CHESTS_WOODEN)
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .unlockedBy("has_jukebox", has(Items.JUKEBOX))
                .save(consumer);
    }
}