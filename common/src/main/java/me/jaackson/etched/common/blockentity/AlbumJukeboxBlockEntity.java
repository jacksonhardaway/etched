package me.jaackson.etched.common.blockentity;

import me.jaackson.etched.Etched;
import me.jaackson.etched.EtchedRegistry;
import me.jaackson.etched.common.item.EtchedMusicDiscItem;
import me.jaackson.etched.common.network.handler.EtchedClientPlayHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ocelot
 */
public class AlbumJukeboxBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {

    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    private NonNullList<ItemStack> items;
    @Environment(EnvType.CLIENT)
    private int playingIndex = -1;

    public AlbumJukeboxBlockEntity() {
        super(EtchedRegistry.ALBUM_JUKEBOX_BE.get());
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
    }

    private void updatePlaying() {
        if (this.level == null)
            return;

        BlockPos pos = this.getBlockPos();
        BlockState state = this.level.getBlockState(pos);
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack stack = this.getItem(i);
            if (EtchedMusicDiscItem.isPlayableRecord(stack)) {
                state = state.setValue(JukeboxBlock.HAS_RECORD, true);
                this.level.setBlock(pos, state, 2);
                return;
            }
        }

        if (state.getValue(JukeboxBlock.HAS_RECORD)) {
            this.level.levelEvent(1010, this.getBlockPos(), 0);
            state = state.setValue(JukeboxBlock.HAS_RECORD, false);
            this.level.setBlock(this.getBlockPos(), state, 2);
        }
    }

    @Override
    public void load(BlockState state, CompoundTag hbt) {
        super.load(state, hbt);

        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(hbt))
            ContainerHelper.loadAllItems(hbt, this.items);
        if (this.level != null && this.level.isClientSide())
            EtchedClientPlayHandler.playAlbum(this, (ClientLevel) this.level, this.getBlockPos(), false);
    }

    @Override
    public CompoundTag save(CompoundTag hbt) {
        super.save(hbt);

        if (!this.trySaveLootTable(hbt))
            ContainerHelper.saveAllItems(hbt, this.items);
        return hbt;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.save(new CompoundTag());
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return new ClientboundBlockEntityDataPacket(this.getBlockPos(), 0, this.getUpdateTag());
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return EtchedMusicDiscItem.isPlayableRecord(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public int getContainerSize() {
        return 9;
    }

    @Override
    public ItemStack removeItem(int index, int amount) {
        ItemStack stack = super.removeItem(index, amount);
        if (!stack.isEmpty())
            this.updatePlaying();
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = super.removeItemNoUpdate(index);
        if (!stack.isEmpty())
            this.updatePlaying();
        return stack;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        super.setItem(index, stack);
        this.updatePlaying();
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.updatePlaying();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> nonNullList) {
        this.items = nonNullList;
    }

    @Override
    protected Component getDefaultName() {
        return new TranslatableComponent("container." + Etched.MOD_ID + ".album_jukebox");
    }

    @Override
    protected AbstractContainerMenu createMenu(int menuId, Inventory inventory) {
        return new DispenserMenu(menuId, inventory, this);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Environment(EnvType.CLIENT)
    public int getPlayingIndex() {
        return playingIndex;
    }

    /**
     * Cycles to the next index to begin playing.
     */
    @Environment(EnvType.CLIENT)
    public void next() {
        this.playingIndex++;
        this.playingIndex %= this.getContainerSize();
    }

    /**
     * Starts playing the next valid song in the album.
     */
    @Environment(EnvType.CLIENT)
    public void nextPlayingIndex() {
        boolean wrap = false;
        if (this.playingIndex < 0)
            this.playingIndex = 0;
        while (!EtchedMusicDiscItem.isPlayableRecord(this.getItem(this.playingIndex))) {
            this.playingIndex++;
            if (this.playingIndex >= this.getContainerSize()) {
                this.playingIndex = 0;
                if (wrap) {
                    this.playingIndex = -1;
                    return;
                }
                wrap = true;
            }
        }
    }

    /**
     * Changes the current playing index to the next valid disc.
     *
     * @return Whether or not a change was made
     */
    @Environment(EnvType.CLIENT)
    public boolean recalculatePlayingIndex() {
        if (this.isEmpty()) {
            if (this.playingIndex == -1)
                return false;
            this.playingIndex = -1;
            return true;
        }

        int oldIndex = this.playingIndex;
        this.nextPlayingIndex();
        return oldIndex != this.playingIndex;
    }
}
