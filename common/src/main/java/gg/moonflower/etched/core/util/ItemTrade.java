package gg.moonflower.etched.core.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Random;
import java.util.function.Supplier;

// TODO Make an API in Pollen to make this easier
public class ItemTrade implements VillagerTrades.ItemListing {

    private final Supplier<Item> item;
    private final int emeralds;
    private final int itemCount;
    private final int maxUses;
    private final int xpGain;
    private final float priceMultiplier;
    private final boolean sellToVillager;

    public ItemTrade(Supplier<Item> Item, int emeralds, int itemCount, int maxUses, int xpGain) {
        this(Item, emeralds, itemCount, maxUses, xpGain, 0.05F, false);
    }

    public ItemTrade(Supplier<Item> Item, int emeralds, int itemCount, int maxUses, int xpGain, boolean sellToVillager) {
        this(Item, emeralds, itemCount, maxUses, xpGain, 0.05F, sellToVillager);
    }

    public ItemTrade(Supplier<Item> Item, int emeralds, int itemCount, int maxUses, int xpGain, float priceMultiplier, boolean sellToVillager) {
        this.item = Item;
        this.emeralds = emeralds;
        this.itemCount = itemCount;
        this.maxUses = maxUses;
        this.xpGain = xpGain;
        this.priceMultiplier = priceMultiplier;
        this.sellToVillager = sellToVillager;
    }

    public MerchantOffer getOffer(Entity entity, Random random) {
        ItemStack emeralds = new ItemStack(Items.EMERALD, this.emeralds);
        ItemStack item = new ItemStack(this.item.get(), this.itemCount);

        return new MerchantOffer(this.sellToVillager ? item : emeralds, this.sellToVillager ? emeralds : item, this.maxUses, this.xpGain, this.priceMultiplier);
    }
}
