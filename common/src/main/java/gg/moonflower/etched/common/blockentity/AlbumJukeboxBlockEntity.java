package gg.moonflower.etched.common.blockentity;

import dev.architectury.injectables.annotations.PlatformOnly;
import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.common.block.AlbumJukeboxBlock;
import gg.moonflower.etched.common.menu.AlbumJukeboxMenu;
import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandlerImpl;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ocelot
 */
public class AlbumJukeboxBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {

    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    private NonNullList<ItemStack> items;
    private int playingIndex;
    private int track;
    private ItemStack playingStack;

    public AlbumJukeboxBlockEntity(BlockPos pos, BlockState state) {
        super(EtchedBlocks.ALBUM_JUKEBOX_BE.get(), pos, state);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        this.playingIndex = -1;
        this.track = 0;
        this.playingStack = ItemStack.EMPTY;
    }

    private void updateState() {
        if (this.level != null) {
            boolean hasItem = false;
            for (ItemStack stack : this.getItems()) {
                if (stack != ItemStack.EMPTY) {
                    hasItem = true;
                    break;
                }
            }

            boolean hasRecord = this.level.getBlockState(this.worldPosition).getValue(AlbumJukeboxBlock.HAS_RECORD);
            if (hasItem != hasRecord) {
                this.level.setBlock(this.worldPosition, this.level.getBlockState(this.worldPosition).setValue(AlbumJukeboxBlock.HAS_RECORD, hasItem), 3);
                this.setChanged();
            }
        }
    }

    private void updatePlaying() {
        if (this.level == null)
            return;
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
    }

    public void onLoad() {
        if (this.level != null && this.level.isClientSide())
            EtchedClientPlayPacketHandlerImpl.playAlbum(this, (ClientLevel) this.level, this.getBlockPos(), false);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);

        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(nbt))
            ContainerHelper.loadAllItems(nbt, this.items);
        if (this.level != null && this.level.isClientSide())
            EtchedClientPlayPacketHandlerImpl.playAlbum(this, (ClientLevel) this.level, this.getBlockPos(), false);
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);

        if (!this.trySaveLootTable(nbt))
            ContainerHelper.saveAllItems(nbt, this.items);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return PlayableRecord.isPlayableRecord(stack);
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
        this.updateState();
        if (!stack.isEmpty())
            this.updatePlaying();
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = super.removeItemNoUpdate(index);
        this.updateState();
        if (!stack.isEmpty())
            this.updatePlaying();
        return stack;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        super.setItem(index, stack);
        this.updateState();
        this.updatePlaying();
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.updateState();
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
        return new AlbumJukeboxMenu(menuId, inventory, this, this.getBlockPos());
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    public int getPlayingIndex() {
        return playingIndex;
    }

    public int getTrack() {
        return track;
    }

    /**
     * Sets the playing disc and track.
     *
     * @param playingIndex The new index to play
     * @param track        The track to play on the disc
     * @return Whether a change was made in the index
     */
    public boolean setPlayingIndex(int playingIndex, int track) {
        this.playingIndex = playingIndex;
        this.track = track;

        if (this.recalculatePlayingIndex(false)) {
            int tracks = PlayableRecord.getStackTrackCount(this.playingStack);
            if (this.track >= tracks)
                this.track = 0;
            return true;
        }

        return false;
    }

    /**
     * Stops playing the current track and resets to the start.
     */
    public void stopPlaying() {
        this.playingIndex = -1;
        this.track = 0;
        this.playingStack = ItemStack.EMPTY;
    }

    /**
     * Cycles to the previous index to begin playing.
     */
    public void previous() {
        if (this.track > 0) {
            this.track--;
        } else {
            this.playingIndex--;
            if (this.playingIndex < 0)
                this.playingIndex = this.getContainerSize() - 1;
            this.nextPlayingIndex(true);
            this.track = Math.max(0, this.playingIndex < 0 || this.playingIndex >= this.getContainerSize() ? 0 : PlayableRecord.getStackTrackCount(this.getItem(this.playingIndex)) - 1);
            this.playingStack = ItemStack.EMPTY;
        }
    }

    /**
     * Cycles to the next index to begin playing.
     */
    public void next() {
        int tracks = this.playingIndex < 0 || this.playingIndex >= this.getContainerSize() ? 1 : PlayableRecord.getStackTrackCount(this.getItem(this.playingIndex));
        if (this.track < tracks - 1) {
            this.track++;
        } else {
            this.playingIndex++;
            this.playingIndex %= this.getContainerSize();
            this.nextPlayingIndex(false);
            this.track = 0;
            this.playingStack = ItemStack.EMPTY;
        }
    }

    /**
     * Starts playing the next valid song in the album.
     */
    public void nextPlayingIndex(boolean reverse) {
        boolean wrap = false;
        this.playingIndex = Mth.clamp(this.playingIndex, 0, this.getContainerSize() - 1);
        while (!PlayableRecord.isPlayableRecord(this.getItem(this.playingIndex))) {
            if (reverse) {
                this.playingIndex--;
                if (this.playingIndex < 0) {
                    this.playingIndex = this.getContainerSize() - 1;
                    if (wrap) {
                        this.playingIndex = -1;
                        this.track = 0;
                        this.playingStack = ItemStack.EMPTY;
                        return;
                    }
                    wrap = true;
                }
            } else {
                this.playingIndex++;
                if (this.playingIndex >= this.getContainerSize()) {
                    this.playingIndex = 0;
                    if (wrap) {
                        this.playingIndex = -1;
                        this.track = 0;
                        this.playingStack = ItemStack.EMPTY;
                        return;
                    }
                    wrap = true;
                }
            }
        }
        this.playingStack = this.getItem(this.playingIndex).copy();
    }

    /**
     * Changes the current playing index to the next valid disc.
     *
     * @return Whether a change was made
     */
    public boolean recalculatePlayingIndex(boolean reverse) {
        if (this.isEmpty()) {
            if (this.playingIndex == -1)
                return false;
            this.playingIndex = -1;
            this.track = 0;
            return true;
        }

        int oldIndex = this.playingIndex;
        ItemStack oldStack = this.playingStack.copy();
        this.nextPlayingIndex(reverse);
        if (oldIndex != this.playingIndex || !ItemStack.matches(oldStack, this.playingStack)) {
            this.track = reverse ? Math.max(0, this.playingIndex < 0 || this.playingIndex >= this.getContainerSize() ? 0 : PlayableRecord.getStackTrackCount(this.getItem(this.playingIndex)) - 1) : 0;
            return true;
        }
        return false;
    }

    public boolean isPlaying() {
        BlockState state = this.getBlockState();
        return (!state.hasProperty(AlbumJukeboxBlock.POWERED) || !state.getValue(AlbumJukeboxBlock.POWERED)) && !this.isEmpty();
    }
}
