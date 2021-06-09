package me.jaackson.etched.common.item;

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
}
