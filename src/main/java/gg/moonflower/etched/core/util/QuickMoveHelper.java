package gg.moonflower.etched.core.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Quick moves items from one slot to another in an easier way than manually checking for slot indices.</p>
 * <p>Actions are performed from top to bottom, so overlaps between actions will always prioritize the one added first.</p>
 *
 * @author Ocelot
 */
@Deprecated
public class QuickMoveHelper {

    private final List<Action> actions;

    public QuickMoveHelper() {
        this.actions = new ArrayList<>();
    }

    /**
     * Custom implementation of {@link AbstractContainerMenu#moveItemStackTo(ItemStack, int, int, boolean)} that respects slot restrictions.
     */
    private static boolean mergeItemStack(AbstractContainerMenu menu, ItemStack stack, int startIndex, int endIndex, boolean reverse) {
        boolean flag = false;
        int i = startIndex;
        if (reverse) {
            i = endIndex - 1;
        }

        if (stack.isStackable()) {
            while (!stack.isEmpty()) {
                if (reverse) {
                    if (i < startIndex) {
                        break;
                    }
                } else if (i >= endIndex) {
                    break;
                }

                Slot slot = menu.getSlot(i);
                ItemStack itemstack = slot.getItem();
                if (slot.mayPlace(stack) && !itemstack.isEmpty() && ItemStack.isSameItemSameTags(stack, itemstack)) {
                    int j = itemstack.getCount() + stack.getCount();
                    int maxSize = Math.min(slot.getMaxStackSize(stack), stack.getMaxStackSize());
                    if (j <= maxSize) {
                        stack.setCount(0);
                        itemstack.setCount(j);
                        slot.setChanged();
                        flag = true;
                    } else if (itemstack.getCount() < maxSize) {
                        stack.shrink(maxSize - itemstack.getCount());
                        itemstack.setCount(maxSize);
                        slot.setChanged();
                        flag = true;
                    }
                }

                if (reverse) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        if (!stack.isEmpty()) {
            if (reverse) {
                i = endIndex - 1;
            } else {
                i = startIndex;
            }

            while (true) {
                if (reverse) {
                    if (i < startIndex) {
                        break;
                    }
                } else if (i >= endIndex) {
                    break;
                }

                Slot slot1 = menu.getSlot(i);
                ItemStack itemstack1 = slot1.getItem();
                if (itemstack1.isEmpty() && slot1.mayPlace(stack)) {
                    if (stack.getCount() > slot1.getMaxStackSize(stack)) {
                        slot1.set(stack.split(slot1.getMaxStackSize(stack)));
                    } else {
                        slot1.set(stack.split(stack.getCount()));
                    }

                    slot1.setChanged();
                    flag = true;
                    break;
                }

                if (reverse) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        return flag;
    }

    /**
     * Adds a new action to the move helper.
     *
     * @param fromStart The slot index to move items from
     * @param fromSize  The amount of slots to include in the starting area
     * @param toStart   The slot index to move items to
     * @param toSize    The amount of slots to include in the ending area
     * @param reverse   Whether to start from the last slot of the to area
     */
    public QuickMoveHelper add(int fromStart, int fromSize, int toStart, int toSize, boolean reverse) {
        this.actions.add(new Action(fromStart, fromSize, toStart, toSize, reverse));
        return this;
    }

    /**
     * Performs a quick move for the specified menu from the specified slot.
     *
     * @param menu   The menu to quick move for
     * @param player The player moving the stack
     * @param slotId The slot to move from
     * @return The remaining items after the move
     */
    public ItemStack quickMoveStack(AbstractContainerMenu menu, Player player, int slotId) {
        ItemStack oldStack = ItemStack.EMPTY;
        Slot slot = menu.getSlot(slotId);
        if (slot != null && slot.hasItem()) {
            ItemStack slotItem = slot.getItem();
            oldStack = slotItem.copy();

            for (Action action : this.actions) {
                if (slotId < action.fromStart || slotId >= action.fromEnd) {
                    continue;
                }
                if (!mergeItemStack(menu, slotItem, action.toStart, action.toEnd, action.reverse)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotItem.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotItem.getCount() == oldStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotItem);
        }

        return oldStack;
    }

    /**
     * <p>An action that can take place for stacks.</p>
     *
     * @author Ocelot
     * @since 1.0.0
     */
    public static class Action {
        private final int fromStart;
        private final int fromEnd;
        private final int toStart;
        private final int toEnd;
        private final boolean reverse;

        public Action(int fromStart, int fromSize, int toStart, int toSize, boolean reverse) {
            this.fromStart = fromStart;
            this.fromEnd = fromStart + fromSize;
            this.toStart = toStart;
            this.toEnd = toStart + toSize;
            this.reverse = reverse;
        }
    }
}