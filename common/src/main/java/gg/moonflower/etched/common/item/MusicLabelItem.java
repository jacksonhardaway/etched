package gg.moonflower.etched.common.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class MusicLabelItem extends Item implements DyeableLeatherItem {

    public MusicLabelItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getColor(ItemStack itemStack) {
        CompoundTag compoundTag = itemStack.getTagElement("display");
        return compoundTag != null && compoundTag.contains("color", 99) ? compoundTag.getInt("color") : 0xFFFFFF;
    }

    public static int getPrimaryColor(ItemStack itemStack) {
        if (itemStack.getItem() instanceof MusicLabelItem)
            return ((DyeableLeatherItem) itemStack.getItem()).getColor(itemStack);

        CompoundTag compoundTag = itemStack.getTagElement("LabelColor");
        return compoundTag != null && compoundTag.contains("Primary", 99) ? compoundTag.getInt("Primary") : 0xFFFFFF;
    }

    public static int getSecondaryColor(ItemStack itemStack) {
        if (itemStack.getItem() instanceof MusicLabelItem)
            return ((DyeableLeatherItem) itemStack.getItem()).getColor(itemStack);

        CompoundTag compoundTag = itemStack.getTagElement("LabelColor");
        return compoundTag != null && compoundTag.contains("Secondary", 99) ? compoundTag.getInt("Secondary") : 0xFFFFFF;
    }
}
