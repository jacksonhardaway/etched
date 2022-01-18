package gg.moonflower.etched.common.recipe;

import gg.moonflower.etched.common.item.ComplexMusicLabelItem;
import gg.moonflower.etched.common.item.MusicLabelItem;
import gg.moonflower.etched.common.item.SimpleMusicLabelItem;
import gg.moonflower.etched.core.registry.EtchedItems;
import gg.moonflower.etched.core.registry.EtchedRecipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class ComplexMusicLabelRecipe extends CustomRecipe {

    public ComplexMusicLabelRecipe(ResourceLocation resourceLocation) {
        super(resourceLocation);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        int count = 0;

        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack itemStack = inv.getItem(i);
            if (!itemStack.isEmpty()) {
                if (!(itemStack.getItem() instanceof MusicLabelItem)) {
                    return false;
                }
                count++;
            }
        }

        return count == 2;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv) {
        List<ItemStack> labels = new ArrayList<>(2);
        for (int j = 0; j < inv.getContainerSize(); ++j) {
            ItemStack stack = inv.getItem(j);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof MusicLabelItem) {
                    ItemStack copy = stack.copy();
                    copy.setCount(1);
                    labels.add(copy);
                }
                if (labels.size() > 2)
                    return ItemStack.EMPTY;
            }
        }

        if (labels.size() != 2)
            return ItemStack.EMPTY;

        ItemStack stack = this.getResultItem();
        SimpleMusicLabelItem.setTitle(stack, SimpleMusicLabelItem.getTitle(labels.get(0)));
        SimpleMusicLabelItem.setAuthor(stack, SimpleMusicLabelItem.getAuthor(labels.get(0)));
        ComplexMusicLabelItem.setColor(stack, MusicLabelItem.getLabelColor(labels.get(0)), MusicLabelItem.getLabelColor(labels.get(1)));
        return stack;
    }

    @Override
    public ItemStack getResultItem() {
        return new ItemStack(EtchedItems.COMPLEX_MUSIC_LABEL.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return EtchedRecipes.COMPLEX_MUSIC_LABEL.get();
    }
}