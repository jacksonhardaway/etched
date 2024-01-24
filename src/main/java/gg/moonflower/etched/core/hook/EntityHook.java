package gg.moonflower.etched.core.hook;

import gg.moonflower.etched.core.registry.EtchedBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EntityHook {

    public static void warpRadio(ServerLevel level, ItemEntity entity) {
        if (level.dimension() == Level.NETHER) {
            ItemStack oldStack = entity.getItem();
            if (oldStack.getItem() != EtchedBlocks.RADIO.get().asItem())
                return;

            ItemStack newStack = new ItemStack(EtchedBlocks.PORTAL_RADIO_ITEM.get(), oldStack.getCount());
            newStack.setTag(oldStack.getTag());
            entity.setItem(newStack);
        }
    }
}
