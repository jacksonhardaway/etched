package gg.moonflower.etched.common.menu;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.common.item.AlbumCoverItem;
import gg.moonflower.etched.core.registry.EtchedItems;
import gg.moonflower.etched.core.registry.EtchedMenus;
import gg.moonflower.pollen.api.util.QuickMoveHelper;
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
public class AlbumCoverMenu extends AbstractContainerMenu {

    private static final QuickMoveHelper MOVE_HELPER = new QuickMoveHelper().
            add(0, 9, 9, 36, true). // to Inventory
                    add(9, 36, 0, 9, false); // from Inventory

    private final Inventory inventory;
    private final Container albumCoverInventory;
    private final int albumCoverIndex;

    public AlbumCoverMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, -1);
    }

    public AlbumCoverMenu(int containerId, Inventory inventory, int albumCoverIndex) {
        super(EtchedMenus.ALBUM_COVER_MENU.get(), containerId);
        this.albumCoverInventory = albumCoverIndex == -1 ? new SimpleContainer(AlbumCoverItem.MAX_RECORDS) : new AlbumCoverContainer(inventory, albumCoverIndex);
        this.albumCoverIndex = albumCoverIndex;
        this.inventory = inventory;

        for (int n = 0; n < 3; ++n) {
            for (int m = 0; m < 3; ++m) {
                this.addSlot(new Slot(this.albumCoverInventory, m + n * 3, 62 + m * 18, 17 + n * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return isValid(stack);
                    }
                });
            }
        }

        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(inventory, x + y * 9 + 9, 8 + x * 18, y * 18 + 84) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return this.getItem().getItem() != EtchedItems.ALBUM_COVER.get();
                    }
                });
            }
        }

        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 142) {
                @Override
                public boolean mayPickup(Player player) {
                    return this.getItem().getItem() != EtchedItems.ALBUM_COVER.get();
                }
            });
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.albumCoverInventory.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (this.albumCoverIndex == -1)
            return;
        ItemStack cover = this.inventory.getItem(this.albumCoverIndex);
        if (!AlbumCoverItem.getCoverStack(cover).isPresent()) {
            for (int i = 0; i < this.albumCoverInventory.getContainerSize(); i++) {
                ItemStack stack = this.albumCoverInventory.getItem(i);
                if (!stack.isEmpty()) {
                    AlbumCoverItem.setCover(cover, stack);
                    break;
                }
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return MOVE_HELPER.quickMoveStack(this, player, slot);
    }

    public static boolean isValid(ItemStack stack) {
        return PlayableRecord.isPlayableRecord(stack) && stack.getItem() != EtchedItems.ALBUM_COVER.get();
    }
}
