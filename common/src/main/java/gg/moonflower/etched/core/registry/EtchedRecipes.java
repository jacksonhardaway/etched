package gg.moonflower.etched.core.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import gg.moonflower.etched.common.recipe.CleanAlbumCoverRecipe;
import gg.moonflower.etched.common.recipe.ComplexMusicLabelRecipe;
import gg.moonflower.etched.core.Etched;
import net.minecraft.core.Registry;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;

public class EtchedRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> REGISTRY = DeferredRegister.create(Etched.MOD_ID, Registry.RECIPE_SERIALIZER_REGISTRY);

    public static final RegistrySupplier<SimpleRecipeSerializer<ComplexMusicLabelRecipe>> COMPLEX_MUSIC_LABEL = REGISTRY.register("complex_music_label", () -> new SimpleRecipeSerializer<>(ComplexMusicLabelRecipe::new));
    public static final RegistrySupplier<SimpleRecipeSerializer<CleanAlbumCoverRecipe>> CLEAN_ALBUM_COVER = REGISTRY.register("clean_album_cover", () -> new SimpleRecipeSerializer<>(CleanAlbumCoverRecipe::new));
}