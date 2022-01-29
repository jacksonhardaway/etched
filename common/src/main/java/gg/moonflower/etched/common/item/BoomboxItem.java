package gg.moonflower.etched.common.item;

import gg.moonflower.etched.common.menu.BoomboxMenu;
import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandlerImpl;
import gg.moonflower.pollen.api.event.events.lifecycle.TickEvents;
import gg.moonflower.pollen.api.util.NbtConstants;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Map;

public class BoomboxItem extends Item {

    private static final Map<Integer, ItemStack> PLAYING_RECORDS = new Int2ObjectArrayMap<>();

    public BoomboxItem(Properties properties) {
        super(properties);
    }

    static {
        TickEvents.LIVING_PRE.register(entity -> {
            if (!entity.level.isClientSide())
                return true;
            
            ItemStack newPlayingRecord = ItemStack.EMPTY;
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = entity.getItemInHand(hand);
                if (stack.getItem() instanceof BoomboxItem) {
                    ItemStack record = getRecord(stack);
                    if (!record.isEmpty()) {
                        newPlayingRecord = record;
                    }
                }
            }

            if (newPlayingRecord.isEmpty() && entity instanceof Player) {
                Inventory inventory = ((Player) entity).inventory;
                for (ItemStack stack : inventory.items) {
                    if (stack.getItem() instanceof BoomboxItem) {
                        ItemStack record = getRecord(stack);
                        if (!record.isEmpty()) {
                            newPlayingRecord = record;
                        }
                    }
                }
            }

            if (!ItemStack.matches(PLAYING_RECORDS.getOrDefault(entity.getId(), ItemStack.EMPTY), newPlayingRecord)) {
                EtchedClientPlayPacketHandlerImpl.playBoombox(entity, newPlayingRecord);
                if (newPlayingRecord.isEmpty()) {
                    PLAYING_RECORDS.remove(entity.getId());
                } else {
                    PLAYING_RECORDS.put(entity.getId(), newPlayingRecord);
                }
            }

            return true;
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int index = player.inventory.findSlotMatchingItem(stack);
        if (index == -1)
            return InteractionResultHolder.pass(stack);

        if (!level.isClientSide()) {
            player.awardStat(Stats.ITEM_USED.get(this));
            player.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return stack.getHoverName();
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                    return new BoomboxMenu(containerId, player.inventory, index);
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static ItemStack getRecord(ItemStack stack) {
        if (!(stack.getItem() instanceof BoomboxItem))
            return ItemStack.EMPTY;
        CompoundTag compoundTag = stack.getTag();
        return compoundTag != null && compoundTag.contains("Record", NbtConstants.COMPOUND) ? ItemStack.of(compoundTag.getCompound("Record")) : ItemStack.EMPTY;
    }

    public static void setRecord(ItemStack stack, ItemStack record) {
        if (!(stack.getItem() instanceof BoomboxItem))
            return;

        if (record.isEmpty()) {
            stack.removeTagKey("Record");
        } else {
            stack.getOrCreateTag().put("Record", record.save(new CompoundTag()));
        }
    }
}
