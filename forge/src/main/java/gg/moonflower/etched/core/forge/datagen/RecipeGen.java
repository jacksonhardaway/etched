package gg.moonflower.etched.core.forge.datagen;

import gg.moonflower.etched.core.registry.EtchedBlocks;
import gg.moonflower.etched.core.registry.EtchedItems;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

public class RecipeGen extends RecipeProvider {

    public RecipeGen(DataGenerator generator) {
        super(generator);
    }

    @Override
    protected void buildCraftingRecipes(Consumer<FinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(EtchedBlocks.ETCHING_TABLE.get())
                .pattern(" DI")
                .pattern("PPP")
                .define('D', Tags.Items.GEMS_DIAMOND)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_diamond", has(Tags.Items.GEMS_DIAMOND))
                .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
                .save(consumer);
        ShapedRecipeBuilder.shaped(EtchedBlocks.ALBUM_JUKEBOX.get())
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
        ShapedRecipeBuilder.shaped(EtchedItems.MUSIC_LABEL.get())
                .pattern(" P ")
                .pattern("P P")
                .pattern(" P ")
                .define('P', Items.PAPER)
                .unlockedBy("has_blank_music_disc", has(EtchedItems.BLANK_MUSIC_DISC.get()))
                .save(consumer);
        ShapedRecipeBuilder.shaped(EtchedItems.JUKEBOX_MINECART.get())
                .pattern("A")
                .pattern("B")
                .define('A', Items.JUKEBOX)
                .define('B', Items.MINECART)
                .unlockedBy("has_minecart", has(Items.MINECART))
                .save(consumer);

        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ItemTags.MUSIC_DISCS), EtchedItems.BLANK_MUSIC_DISC.get(), 0.2F, 200).unlockedBy("has_music_disc", has(ItemTags.MUSIC_DISCS)).save(consumer);
    }
}