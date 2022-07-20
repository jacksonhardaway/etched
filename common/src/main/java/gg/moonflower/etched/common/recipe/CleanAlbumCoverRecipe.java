package gg.moonflower.etched.common.recipe;

import gg.moonflower.etched.common.item.AlbumCoverItem;
import gg.moonflower.etched.core.registry.EtchedRecipes;
import gg.moonflower.pollen.api.crafting.grindstone.PollenGrindstoneRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class CleanAlbumCoverRecipe implements PollenGrindstoneRecipe {

    private final ResourceLocation id;

    public CleanAlbumCoverRecipe(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public boolean matches(Container container, Level level) {
        ItemStack stack = ItemStack.EMPTY;
        for (int j = 0; j < 2; ++j) {
            ItemStack itemStack = container.getItem(j);
            if (!itemStack.isEmpty()) {
                if (!stack.isEmpty())
                    return false;
                stack = itemStack;
            }
        }

        return AlbumCoverItem.getCoverStack(stack).isPresent();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack assemble(Container container) {
        ItemStack result = container.getItem(0).isEmpty() ? container.getItem(1) : container.getItem(0).copy();
        result.setCount(1);
        AlbumCoverItem.setCover(result, ItemStack.EMPTY);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w * h >= 1;
    }

    @Override
    public ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return EtchedRecipes.CLEAN_ALBUM_COVER.get();
    }

    @Override
    public int getResultExperience() {
        return 0;
    }
}
