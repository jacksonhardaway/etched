package gg.moonflower.etched.common.item;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.PlayableRecordItem;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.client.render.item.AlbumCoverItemRenderer;
import gg.moonflower.etched.common.menu.AlbumCoverMenu;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.registry.EtchedItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class AlbumCoverItem extends PlayableRecordItem implements ContainerItem {

    public static final int MAX_RECORDS = 9;

    public AlbumCoverItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return AlbumCoverItemRenderer.INSTANCE;
            }
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isSecondaryUseActive()) {
            if (dropContents(stack, player)) {
                this.playDropContentsSound(player);
                player.awardStat(Stats.ITEM_USED.get(this));
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
            return InteractionResultHolder.pass(stack);
        }

        if (!Etched.SERVER_CONFIG.useAlbumCoverMenu.get()) {
            return InteractionResultHolder.fail(stack);
        }
        return this.use(this, level, player, hand);
    }

    @Override
    public AbstractContainerMenu constructMenu(int containerId, Inventory inventory, Player player, int index) {
        return new AlbumCoverMenu(containerId, inventory, index);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack albumCover, Slot slot, ClickAction clickAction, Player player) {
        if (Etched.SERVER_CONFIG.useAlbumCoverMenu.get()) {
            return false;
        }
        if (clickAction != ClickAction.SECONDARY) {
            return false;
        }

        ItemStack clickItem = slot.getItem();
        if (clickItem.isEmpty()) {
            removeOne(albumCover).ifPresent(record -> {
                this.playRemoveOneSound(player);
                add(albumCover, slot.safeInsert(record));
            });
        } else if (canAdd(albumCover, clickItem)) {
            this.playInsertSound(player);
            add(albumCover, slot.safeTake(clickItem.getCount(), 1, player));
        }

        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack albumCover, ItemStack clickItem, Slot slot, ClickAction clickAction, Player player, SlotAccess slotAccess) {
        if (Etched.SERVER_CONFIG.useAlbumCoverMenu.get()) {
            return false;
        }
        if (clickAction == ClickAction.SECONDARY && slot.allowModification(player)) {
            if (clickItem.isEmpty()) {
                removeOne(albumCover).ifPresent(removedRecord -> {
                    this.playRemoveOneSound(player);
                    slotAccess.set(removedRecord);
                });
            } else if (canAdd(albumCover, clickItem)) {
                this.playInsertSound(player);
                add(albumCover, clickItem);
            }

            return true;
        }

        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag tooltipFlag) {
        for (ItemStack record : getRecords(stack)) {
            if (record.getItem() instanceof PlayableRecord) {
                record.getItem().appendHoverText(record, level, list, tooltipFlag);
            }
        }
    }

    @Override
    public void onDestroyed(ItemEntity itemEntity) {
        ItemUtils.onContainerDestroyed(itemEntity, getRecords(itemEntity.getItem()).stream());
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

    private static Optional<ItemStack> removeOne(ItemStack albumCover) {
        CompoundTag tag = albumCover.getOrCreateTag();
        if (!tag.contains("Records", Tag.TAG_LIST)) {
            return Optional.empty();
        }

        ListTag recordsNbt = tag.getList("Records", Tag.TAG_COMPOUND);
        if (recordsNbt.isEmpty()) {
            return Optional.empty();
        }

        CompoundTag recordNbt = recordsNbt.getCompound(recordsNbt.size() - 1);
        ItemStack recordStack = ItemStack.of(recordNbt);
        recordsNbt.remove(recordsNbt.size() - 1);

        return Optional.of(recordStack);
    }

    private static boolean dropContents(ItemStack itemStack, Player player) {
        CompoundTag tag = itemStack.getOrCreateTag();
        if (!tag.contains("Records")) {
            return false;
        }

        if (player instanceof ServerPlayer) {
            ListTag listTag = tag.getList("Records", Tag.TAG_COMPOUND);

            for (int i = 0; i < listTag.size(); i++) {
                player.getInventory().placeItemBackInInventory(ItemStack.of(listTag.getCompound(i)));
            }
        }

        itemStack.removeTagKey("Records");
        return true;
    }

    private static void add(ItemStack albumCover, ItemStack record) {
        if (!albumCover.is(EtchedItems.ALBUM_COVER.get()) || !AlbumCoverMenu.isValid(record)) {
            return;
        }

        CompoundTag tag = albumCover.getOrCreateTag();
        if (!tag.contains("Records")) {
            tag.put("Records", new ListTag());
        }

        ListTag recordsNbt = tag.getList("Records", Tag.TAG_COMPOUND);

        ItemStack singleRecord = record.split(1);
        CompoundTag recordTag = new CompoundTag();
        singleRecord.save(recordTag);
        recordsNbt.add(recordTag);

        if (getCoverStack(albumCover).isEmpty()) {
            getRecords(albumCover).stream().filter(stack -> !stack.isEmpty()).findFirst().ifPresent(stack -> setCover(albumCover, stack));
        }
    }

    private static boolean canAdd(ItemStack albumCover, ItemStack record) {
        if (!albumCover.is(EtchedItems.ALBUM_COVER.get()) || !AlbumCoverMenu.isValid(record)) {
            return false;
        }
        return albumCover.getTag() == null || !albumCover.getTag().contains("Records", Tag.TAG_LIST) || albumCover.getTag().getList("Records", Tag.TAG_COMPOUND).size() < MAX_RECORDS;
    }

    @Override
    public Optional<TrackData[]> getMusic(ItemStack stack) {
        List<ItemStack> records = getRecords(stack);
        return records.isEmpty() ? Optional.empty() : Optional.of(records.stream().filter(record -> record.getItem() instanceof PlayableRecord).flatMap(record -> Arrays.stream(((PlayableRecord) record.getItem()).getMusic(record).orElseGet(() -> new TrackData[0]))).toArray(TrackData[]::new));
    }

    @Override
    public Optional<TrackData> getAlbum(ItemStack stack) {
        return Optional.empty();
    }

    @Override
    public int getTrackCount(ItemStack stack) {
        return getRecords(stack).stream().filter(record -> record.getItem() instanceof PlayableRecord).mapToInt(record -> ((PlayableRecord) record.getItem()).getTrackCount(record)).sum();
    }

    @Override
    public boolean canGrindstoneRepair(ItemStack stack) {
        return getCoverStack(stack).isPresent();
    }

    public static Optional<ItemStack> getCoverStack(ItemStack stack) {
        if (stack.getItem() != EtchedItems.ALBUM_COVER.get()) {
            return Optional.empty();
        }

        CompoundTag nbt = stack.getTag();
        if (nbt == null || !nbt.contains("CoverRecord", Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }

        ItemStack cover = ItemStack.of(nbt.getCompound("CoverRecord"));
        return cover.isEmpty() ? Optional.empty() : Optional.of(cover);
    }

    public static List<ItemStack> getRecords(ItemStack stack) {
        if (stack.getItem() != EtchedItems.ALBUM_COVER.get()) {
            return Collections.emptyList();
        }

        CompoundTag nbt = stack.getTag();
        if (nbt == null || !nbt.contains("Records", Tag.TAG_LIST)) {
            return Collections.emptyList();
        }

        ListTag recordsNbt = nbt.getList("Records", Tag.TAG_COMPOUND);
        if (recordsNbt.isEmpty()) {
            return Collections.emptyList();
        }

        List<ItemStack> list = new ArrayList<>(recordsNbt.size());
        for (int i = 0; i < Math.min(MAX_RECORDS, recordsNbt.size()); i++) {
            ItemStack record = ItemStack.of(recordsNbt.getCompound(i));
            if (!record.isEmpty()) {
                list.add(record);
            }
        }

        return list;
    }

    public static void setCover(ItemStack stack, ItemStack record) {
        if (stack.getItem() != EtchedItems.ALBUM_COVER.get()) {
            return;
        }

        if (record.isEmpty()) {
            stack.removeTagKey("CoverRecord");
            return;
        }
        stack.getOrCreateTag().put("CoverRecord", record.save(new CompoundTag()));
    }

    public static void setRecords(ItemStack stack, Collection<ItemStack> records) {
        if (stack.getItem() != EtchedItems.ALBUM_COVER.get() || records.isEmpty()) {
            return;
        }

        CompoundTag nbt = stack.getOrCreateTag();
        ListTag recordsNbt = new ListTag();
        int i = 0;
        for (ItemStack record : records) {
            if (record.isEmpty()) {
                continue;
            }
            if (i >= MAX_RECORDS) {
                break;
            }
            recordsNbt.add(record.save(new CompoundTag()));
            i++;
        }
        nbt.put("Records", recordsNbt);
    }
}
