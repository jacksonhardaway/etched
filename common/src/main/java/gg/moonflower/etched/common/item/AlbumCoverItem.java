package gg.moonflower.etched.common.item;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.PlayableRecordItem;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.common.menu.AlbumCoverMenu;
import gg.moonflower.etched.core.registry.EtchedItems;
import gg.moonflower.pollen.api.util.NbtConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AlbumCoverItem extends PlayableRecordItem implements ContainerItem {

    public static final int MAX_RECORDS = 9;

    public AlbumCoverItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return this.use(this, level, player, hand);
    }

    @Override
    public AbstractContainerMenu constructMenu(int containerId, Inventory inventory, Player player, int index) {
        return new AlbumCoverMenu(containerId, inventory, index);
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

    public static Optional<ItemStack> getCoverStack(ItemStack stack) {
        if (stack.getItem() != EtchedItems.ALBUM_COVER.get())
            return Optional.empty();

        CompoundTag nbt = stack.getTag();
        if (nbt == null || !nbt.contains("CoverRecord", NbtConstants.COMPOUND))
            return Optional.empty();

        ItemStack cover = ItemStack.of(nbt.getCompound("CoverRecord"));
        return cover.isEmpty() ? Optional.empty() : Optional.of(cover);
    }

    public static List<ItemStack> getRecords(ItemStack stack) {
        if (stack.getItem() != EtchedItems.ALBUM_COVER.get())
            return Collections.emptyList();

        CompoundTag nbt = stack.getTag();
        if (nbt == null || !nbt.contains("Records", NbtConstants.LIST))
            return Collections.emptyList();

        ListTag keysNbt = nbt.getList("Records", NbtConstants.COMPOUND);
        if (keysNbt.isEmpty())
            return Collections.emptyList();

        List<ItemStack> list = new ArrayList<>(keysNbt.size());
        for (int i = 0; i < Math.min(MAX_RECORDS, keysNbt.size()); i++) {
            ItemStack key = ItemStack.of(keysNbt.getCompound(i));
            if (!key.isEmpty())
                list.add(key);
        }

        return list;
    }

    public static void setCover(ItemStack stack, ItemStack record) {
        if (stack.getItem() != EtchedItems.ALBUM_COVER.get())
            return;

        if (record.isEmpty()) {
            stack.removeTagKey("CoverRecord");
            return;
        }
        stack.getOrCreateTag().put("CoverRecord", record.save(new CompoundTag()));
    }

    public static void setRecords(ItemStack stack, Collection<ItemStack> keys) {
        if (stack.getItem() != EtchedItems.ALBUM_COVER.get() || keys.isEmpty())
            return;

        CompoundTag nbt = stack.getOrCreateTag();
        ListTag keysNbt = new ListTag();
        int i = 0;
        for (ItemStack key : keys) {
            if (key.isEmpty())
                continue;
            if (i >= MAX_RECORDS)
                break;
            keysNbt.add(key.save(new CompoundTag()));
            i++;
        }
        nbt.put("Records", keysNbt);
    }
}
