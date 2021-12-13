package gg.moonflower.etched.common.menu;

import gg.moonflower.etched.common.item.EtchedMusicDiscItem;
import gg.moonflower.etched.core.registry.EtchedMenus;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * @author Ocelot
 */
public class AlbumJukeboxMenu extends AbstractContainerMenu {

    public final int[] pos;
    private final Container container;
    private boolean initialized;

    public AlbumJukeboxMenu(int i, Inventory inventory) {
        this(i, inventory, new SimpleContainer(9), BlockPos.ZERO);
    }

    public AlbumJukeboxMenu(int i, Inventory inventory, Container container, BlockPos pos) {
        super(EtchedMenus.ALBUM_JUKEBOX_MENU.get(), i);
        checkContainerSize(container, 9);
        this.container = container;
        container.startOpen(inventory.player);

        this.pos = new int[]{pos.getX(), pos.getY(), pos.getZ()};
        this.addDataSlot(DataSlot.shared(this.pos, 0));
        this.addDataSlot(DataSlot.shared(this.pos, 1));
        this.addDataSlot(DataSlot.shared(this.pos, 2));

        for (int n = 0; n < 3; ++n) {
            for (int m = 0; m < 3; ++m) {
                this.addSlot(new Slot(container, m + n * 3, 62 + m * 18, 17 + n * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return EtchedMusicDiscItem.isPlayableRecord(stack);
                    }
                });
            }
        }

        for (int n = 0; n < 3; ++n) {
            for (int m = 0; m < 9; ++m) {
                this.addSlot(new Slot(inventory, m + n * 9 + 9, 8 + m * 18, 84 + n * 18));
            }
        }

        for (int n = 0; n < 9; ++n) {
            this.addSlot(new Slot(inventory, n, 8 + n * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (i < this.container.getContainerSize()) {
                if (!this.moveItemStackTo(itemStack2, this.container.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 0, this.container.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemStack2);
        }

        return itemStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    @Override
    public void setData(int index, int value) {
        super.setData(index, value);
        if (index >= 0 && index < 3)
            this.initialized = true;
    }

    @Environment(EnvType.CLIENT)
    public boolean isInitialized() {
        return initialized;
    }
}
