package gg.moonflower.etched.common.item;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.menu.BoomboxMenu;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.registry.EtchedItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BoomboxItem extends Item implements ContainerItem {

    private static final Map<Integer, ItemStack> PLAYING_RECORDS = new Int2ObjectArrayMap<>();
    private static final Component PAUSE = Component.translatable("item." + Etched.MOD_ID + ".boombox.pause", Component.keybind("key.sneak"), Component.keybind("key.use")).withStyle(ChatFormatting.GRAY);
    private static final Component RECORDS = Component.translatable("item." + Etched.MOD_ID + ".boombox.records");
    public static final Component PAUSED = Component.translatable("item." + Etched.MOD_ID + ".boombox.paused").withStyle(ChatFormatting.YELLOW);

    public BoomboxItem(Properties properties) {
        super(properties);
    }

    public static void onLivingEntityUpdateClient(LivingEntity entity) {
        ItemStack newPlayingRecord = ItemStack.EMPTY;
        ItemStack mainStack = entity.getMainHandItem();
        ItemStack offStack = entity.getOffhandItem();
        if (mainStack.getItem() instanceof BoomboxItem && hasRecord(mainStack) && !isPaused(mainStack)) {
            newPlayingRecord = getRecord(mainStack);
        } else if (offStack.getItem() instanceof BoomboxItem && hasRecord(offStack) && !isPaused(offStack)) {
            newPlayingRecord = getRecord(offStack);
        }

        if (entity instanceof Player && newPlayingRecord.isEmpty() && Minecraft.getInstance().cameraEntity == entity) {
            Inventory inventory = ((Player) entity).getInventory();
            for (ItemStack stack : inventory.items) {
                if (stack.getItem() instanceof BoomboxItem && hasRecord(stack) && !isPaused(stack)) {
                    newPlayingRecord = getRecord(stack);
                }
            }
        }

        updatePlaying(entity, newPlayingRecord);
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

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide()) {
            return false;
        }
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
        if (!Etched.SERVER_CONFIG.useBoomboxMenu.get()) {
            return InteractionResultHolder.fail(stack);
        }
        return this.use(this, level, player, hand);
    }

    @Override
    public AbstractContainerMenu constructMenu(int containerId, Inventory inventory, Player player, int index) {
        return new BoomboxMenu(containerId, inventory, index);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack boombox, Slot slot, ClickAction clickAction, Player player) {
        if (Etched.SERVER_CONFIG.useBoomboxMenu.get()) {
            return false;
        }
        if (clickAction != ClickAction.SECONDARY) {
            return false;
        }

        ItemStack clickItem = slot.getItem();
        if (clickItem.isEmpty()) {
            this.playRemoveOneSound(player);
            removeOne(boombox).ifPresent(key -> setRecord(boombox, slot.safeInsert(key)));
        } else if (canAdd(boombox, clickItem)) {
            this.playInsertSound(player);
            setRecord(boombox, slot.safeTake(clickItem.getCount(), 1, player).split(1));
        }

        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack boombox, ItemStack clickItem, Slot slot, ClickAction clickAction, Player player, SlotAccess slotAccess) {
        if (Etched.SERVER_CONFIG.useBoomboxMenu.get()) {
            return false;
        }
        if (clickAction == ClickAction.SECONDARY && slot.allowModification(player)) {
            if (clickItem.isEmpty()) {
                removeOne(boombox).ifPresent(removedKey -> {
                    this.playRemoveOneSound(player);
                    slotAccess.set(removedKey);
                });
            } else if (canAdd(boombox, clickItem)) {
                this.playInsertSound(player);
                setRecord(boombox, clickItem.split(1));
            }

            return true;
        }

        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(PAUSE);
        if (hasRecord(stack)) {
            ItemStack record = getRecord(stack);
            List<Component> records = new LinkedList<>();
            record.getItem().appendHoverText(record, level, records, isAdvanced);

            if (!records.isEmpty()) {
                tooltipComponents.add(Component.empty());
                tooltipComponents.add(RECORDS);
                tooltipComponents.addAll(records);
            }
        }
    }

    private void playRemoveOneSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    private void playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    private void playDropContentsSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    /**
     * Retrieves the current hand boombox sounds are coming from for the specified entity.
     *
     * @param entity The entity to check
     * @return The hand the entity is using or <code>null</code> if no boombox is playing
     */
    @Nullable
    public static InteractionHand getPlayingHand(LivingEntity entity) {
        if (!PLAYING_RECORDS.containsKey(entity.getId())) {
            return null;
        }
        ItemStack stack = entity.getMainHandItem();
        if (stack.getItem() instanceof BoomboxItem && hasRecord(stack)) {
            return InteractionHand.MAIN_HAND;
        }
        stack = entity.getOffhandItem();
        if (stack.getItem() instanceof BoomboxItem && hasRecord(stack)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    public static boolean isPaused(ItemStack stack) {
        if (!(stack.getItem() instanceof BoomboxItem)) {
            return false;
        }
        CompoundTag compoundTag = stack.getTag();
        return compoundTag != null && compoundTag.getBoolean("Paused");
    }

    public static boolean hasRecord(ItemStack stack) {
        if (!(stack.getItem() instanceof BoomboxItem)) {
            return false;
        }
        CompoundTag compoundTag = stack.getTag();
        return compoundTag != null && compoundTag.contains("Record", Tag.TAG_COMPOUND);
    }

    public static ItemStack getRecord(ItemStack stack) {
        if (!(stack.getItem() instanceof BoomboxItem)) {
            return ItemStack.EMPTY;
        }
        CompoundTag compoundTag = stack.getTag();
        return compoundTag != null && compoundTag.contains("Record", Tag.TAG_COMPOUND) ? ItemStack.of(compoundTag.getCompound("Record")) : ItemStack.EMPTY;
    }

    public static void setRecord(ItemStack stack, ItemStack record) {
        if (!(stack.getItem() instanceof BoomboxItem)) {
            return;
        }

        if (record.isEmpty()) {
            stack.removeTagKey("Record");
        } else {
            stack.getOrCreateTag().put("Record", record.save(new CompoundTag()));
        }
    }

    public static void setPaused(ItemStack stack, boolean paused) {
        if (!(stack.getItem() instanceof BoomboxItem)) {
            return;
        }

        if (!paused) {
            stack.removeTagKey("Paused");
        } else {
            stack.getOrCreateTag().putBoolean("Paused", true);
        }
    }

    private static Optional<ItemStack> removeOne(ItemStack boombox) {
        if (!hasRecord(boombox)) {
            return Optional.empty();
        }

        ItemStack record = getRecord(boombox);
        if (record.isEmpty()) {
            return Optional.empty();
        }

        setRecord(boombox, ItemStack.EMPTY);
        return Optional.of(record);
    }

    private static boolean canAdd(ItemStack boombox, ItemStack record) {
        if (!(boombox.is(EtchedItems.BOOMBOX.get())) || !(record.getItem() instanceof PlayableRecord)) {
            return false;
        }
        return getRecord(boombox).isEmpty();
    }
}
