package gg.moonflower.etched.common.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class ComplexMusicLabelItem extends SimpleMusicLabelItem {

    public ComplexMusicLabelItem(Properties properties) {
        super(properties);
    }

    public static int getPrimaryColor(ItemStack stack) {
        CompoundTag compoundTag = stack.getTagElement("Label");
        return compoundTag != null && compoundTag.contains("PrimaryColor", 99) ? compoundTag.getInt("PrimaryColor") : 0xFFFFFF;
    }

    public static int getSecondaryColor(ItemStack itemStack) {
        CompoundTag compoundTag = itemStack.getTagElement("Label");
        return compoundTag != null && compoundTag.contains("SecondaryColor", 99) ? compoundTag.getInt("SecondaryColor") : 0xFFFFFF;
    }

    public static void setColor(ItemStack stack, int primary, int secondary) {
        if (!(stack.getItem() instanceof ComplexMusicLabelItem))
            return;

        CompoundTag tag = stack.getOrCreateTagElement("Label");
        tag.putInt("PrimaryColor", primary);
        tag.putInt("SecondaryColor", secondary);
    }
}
