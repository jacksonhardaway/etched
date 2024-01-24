package gg.moonflower.etched.common.menu;

import gg.moonflower.etched.common.item.BoomboxItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * @author Ocelot
 */
public class BoomboxContainer implements Container {

    private final Inventory inventory;
    private final int index;
    private final ItemStack boombox;
    private final NonNullList<ItemStack> keys;

    public BoomboxContainer(Inventory inventory, int index) {
        this.inventory = inventory;
        this.index = index;
        this.boombox = inventory.getItem(index);
        this.keys = NonNullList.of(ItemStack.EMPTY, BoomboxItem.getRecord(this.boombox));
    }

    private void update() {
        BoomboxItem.setRecord(this.boombox, this.keys.get(0));
    }

    @Override
    public int getContainerSize() {
        return this.keys.size();
    }

    @Override
    public boolean isEmpty() {
        return this.keys.isEmpty() || this.keys.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int index) {
        if (index < 0 || index >= this.keys.size()) {
            return ItemStack.EMPTY;
        }
        return this.keys.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack result = ContainerHelper.removeItem(this.keys, index, count);
        this.update();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack result = ContainerHelper.takeItem(this.keys, index);
        this.update();
        return result;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index < 0 || index >= this.keys.size()) {
            return;
        }
        this.keys.set(index, stack);
        this.update();
    }

    @Override
    public void setChanged() {
        this.update();
    }

    @Override
    public boolean stillValid(Player player) {
        return ItemStack.matches(this.inventory.getItem(this.index), this.boombox);
    }

    @Override
    public void clearContent() {
        this.keys.clear();
        this.update();
    }
}
