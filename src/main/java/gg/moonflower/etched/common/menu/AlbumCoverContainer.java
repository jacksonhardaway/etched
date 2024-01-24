package gg.moonflower.etched.common.menu;

import gg.moonflower.etched.common.item.AlbumCoverItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * @author Ocelot
 */
public class AlbumCoverContainer implements Container {

    private final Inventory inventory;
    private final int index;
    private final ItemStack albumCover;
    private final NonNullList<ItemStack> records;

    public AlbumCoverContainer(Inventory inventory, int index) {
        this.inventory = inventory;
        this.index = index;
        this.albumCover = inventory.getItem(index);
        this.records = NonNullList.withSize(AlbumCoverItem.MAX_RECORDS, ItemStack.EMPTY);

        List<ItemStack> keys = AlbumCoverItem.getRecords(this.albumCover);
        for (int i = 0; i < keys.size(); i++)
            this.records.set(i, keys.get(i));
    }

    private void update() {
        AlbumCoverItem.setRecords(this.albumCover, this.records);
    }

    @Override
    public int getContainerSize() {
        return this.records.size();
    }

    @Override
    public boolean isEmpty() {
        return this.records.isEmpty() || this.records.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int index) {
        if (index < 0 || index >= this.records.size())
            return ItemStack.EMPTY;
        return this.records.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack result = ContainerHelper.removeItem(this.records, index, count);
        this.update();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack result = ContainerHelper.takeItem(this.records, index);
        this.update();
        return result;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index < 0 || index >= this.records.size())
            return;
        this.records.set(index, stack);
        this.update();
    }

    @Override
    public void setChanged() {
        this.update();
    }

    @Override
    public boolean stillValid(Player player) {
        return ItemStack.matches(this.inventory.getItem(this.index), this.albumCover);
    }

    @Override
    public void clearContent() {
        this.records.clear();
        this.update();
    }
}
