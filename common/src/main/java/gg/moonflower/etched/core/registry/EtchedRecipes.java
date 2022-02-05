package gg.moonflower.etched.core.registry;

import gg.moonflower.etched.common.recipe.CleanAlbumCoverRecipe;
import gg.moonflower.etched.common.recipe.ComplexMusicLabelRecipe;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.pollen.api.registry.PollinatedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;

import java.util.function.Supplier;

public class EtchedRecipes {

    public static final PollinatedRegistry<RecipeSerializer<?>> RECIPES = PollinatedRegistry.create(Registry.RECIPE_SERIALIZER, Etched.MOD_ID);

    public static final Supplier<SimpleRecipeSerializer<ComplexMusicLabelRecipe>> COMPLEX_MUSIC_LABEL = RECIPES.register("complex_music_label", () -> new SimpleRecipeSerializer<>(ComplexMusicLabelRecipe::new));
    public static final Supplier<SimpleRecipeSerializer<CleanAlbumCoverRecipe>> CLEAN_ALBUM_COVER = RECIPES.register("clean_album_cover", () -> new SimpleRecipeSerializer<>(CleanAlbumCoverRecipe::new));
}