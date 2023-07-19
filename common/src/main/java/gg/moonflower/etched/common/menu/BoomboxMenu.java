package gg.moonflower.etched.common.menu;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.core.registry.EtchedItems;
import gg.moonflower.etched.core.registry.EtchedMenus;
import gg.moonflower.pollen.api.container.util.v1.QuickMoveHelper;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * @author Ocelot
 */
public class BoomboxMenu extends AbstractContainerMenu {

    private static final QuickMoveHelper MOVE_HELPER = new QuickMoveHelper().
            add(0, 1, 1, 36, true). // to Inventory
                    add(1, 36, 0, 1, false); // from Inventory

    private final Container boomboxInventory;

    public BoomboxMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, -1);
    }

    public BoomboxMenu(int containerId, Inventory inventory, int index) {
        super(EtchedMenus.BOOMBOX_MENU.get(), containerId);
        this.boomboxInventory = index == -1 ? new SimpleContainer(1) : new BoomboxContainer(inventory, index);

        this.addSlot(new Slot(this.boomboxInventory, 0, 80, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof PlayableRecord;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(inventory, x + y * 9 + 9, 8 + x * 18, y * 18 + 51) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return this.getItem().getItem() != EtchedItems.BOOMBOX.get();
                    }
                });
            }
        }

        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 109) {
                @Override
                public boolean mayPickup(Player player) {
                    return this.getItem().getItem() != EtchedItems.BOOMBOX.get();
                }
            });
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.boomboxInventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return MOVE_HELPER.quickMoveStack(this, player, slot);
    }
}
