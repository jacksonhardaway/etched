package gg.moonflower.etched.common.item;

import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.menu.BoomboxMenu;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.pollen.api.event.events.lifecycle.TickEvents;
import gg.moonflower.pollen.api.util.NbtConstants;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.KeybindComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BoomboxItem extends Item implements ContainerItem {

    private static final Map<Integer, ItemStack> PLAYING_RECORDS = new Int2ObjectArrayMap<>();
    private static final Component PAUSE = new TranslatableComponent("item." + Etched.MOD_ID + ".boombox.pause", new KeybindComponent("key.sneak"), new KeybindComponent("key.use")).withStyle(ChatFormatting.GRAY);
    private static final Component RECORDS = new TranslatableComponent("item." + Etched.MOD_ID + ".boombox.records");
    public static final Component PAUSED = new TranslatableComponent("item." + Etched.MOD_ID + ".boombox.paused").withStyle(ChatFormatting.YELLOW);

    public BoomboxItem(Properties properties) {
        super(properties);
    }

    static {
        TickEvents.LIVING_PRE.register(entity -> {
            if (!entity.level.isClientSide())
                return true;

            ItemStack newPlayingRecord = ItemStack.EMPTY;
            {
                ItemStack mainStack = entity.getMainHandItem();
                ItemStack offStack = entity.getOffhandItem();
                if (mainStack.getItem() instanceof BoomboxItem && hasRecord(mainStack) && !isPaused(mainStack)) {
                    newPlayingRecord = getRecord(mainStack);
                } else if (offStack.getItem() instanceof BoomboxItem && hasRecord(offStack) && !isPaused(offStack)) {
                    newPlayingRecord = getRecord(offStack);
                }
            }

            if (entity instanceof Player && newPlayingRecord.isEmpty() && Minecraft.getInstance().cameraEntity == entity) {
                Inventory inventory = ((Player) entity).inventory;
                for (ItemStack stack : inventory.items) {
                    if (stack.getItem() instanceof BoomboxItem && hasRecord(stack) && !isPaused(stack)) {
                        newPlayingRecord = getRecord(stack);
                    }
                }
            }

            updatePlaying(entity, newPlayingRecord);
            return true;
        });
    }

    private static void updatePlaying(Entity entity, ItemStack record) {
        if (!ItemStack.matches(PLAYING_RECORDS.getOrDefault(entity.getId(), ItemStack.EMPTY), record)) {
            SoundTracker.playBoombox(entity.getId(), record);
            if (record.isEmpty()) {
                PLAYING_RECORDS.remove(entity.getId());
            } else {
                PLAYING_RECORDS.put(entity.getId(), record);
            }
        }
    }

    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level.isClientSide())
            return false;
        updatePlaying(entity, hasRecord(stack) && !isPaused(stack) ? getRecord(stack) : ItemStack.EMPTY);
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            setPaused(stack, !isPaused(stack));
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return this.use(this, level, player, hand);
    }

    @Override
    public AbstractContainerMenu constructMenu(int containerId, Inventory inventory, Player player, int index) {
        return new BoomboxMenu(containerId, inventory, index);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(PAUSE);
        if (hasRecord(stack)) {
            ItemStack record = getRecord(stack);
            List<Component> records = new LinkedList<>();
            record.getItem().appendHoverText(record, level, records, isAdvanced);

            if (!records.isEmpty()) {
                tooltipComponents.add(TextComponent.EMPTY);
                tooltipComponents.add(RECORDS);
                tooltipComponents.addAll(records);
            }
        }
    }

    /**
     * Retrieves the current hand boombox sounds are coming from for the specified entity.
     *
     * @param entity The entity to check
     * @return The hand the entity is using or <code>null</code> if no boombox is playing
     */
    @Nullable
    public static InteractionHand getPlayingHand(LivingEntity entity) {
        if (!PLAYING_RECORDS.containsKey(entity.getId()))
            return null;
        ItemStack stack = entity.getMainHandItem();
        if (stack.getItem() instanceof BoomboxItem && hasRecord(stack))
            return InteractionHand.MAIN_HAND;
        stack = entity.getOffhandItem();
        if (stack.getItem() instanceof BoomboxItem && hasRecord(stack))
            return InteractionHand.OFF_HAND;
        return null;
    }

    public static boolean isPaused(ItemStack stack) {
        if (!(stack.getItem() instanceof BoomboxItem))
            return false;
        CompoundTag compoundTag = stack.getTag();
        return compoundTag != null && compoundTag.getBoolean("Paused");
    }

    public static boolean hasRecord(ItemStack stack) {
        if (!(stack.getItem() instanceof BoomboxItem))
            return false;
        CompoundTag compoundTag = stack.getTag();
        return compoundTag != null && compoundTag.contains("Record", NbtConstants.COMPOUND);
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

    public static void setPaused(ItemStack stack, boolean paused) {
        if (!(stack.getItem() instanceof BoomboxItem))
            return;

        if (!paused) {
            stack.removeTagKey("Paused");
        } else {
            stack.getOrCreateTag().putBoolean("Paused", true);
        }
    }
}
