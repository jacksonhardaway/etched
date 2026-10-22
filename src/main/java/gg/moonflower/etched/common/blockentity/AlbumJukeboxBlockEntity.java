package gg.moonflower.etched.common.blockentity;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.block.AlbumJukeboxBlock;
import gg.moonflower.etched.common.menu.AlbumJukeboxMenu;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.common.menu.SoundSettingsContainer;
import gg.moonflower.etched.common.menu.SoundSettingsMenu;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Ocelot
 */
public class AlbumJukeboxBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, SoundSettingsContainer {

    /**
     * How far away the jukebox can be heard in blocks unless changed.
     */
    public static final int DEFAULT_RADIUS = 64;

    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    private NonNullList<ItemStack> items;
    private int playingIndex;
    private int track;
    private ItemStack playingStack;
    private float volume;
    private int radius;
    private boolean loaded;

    public AlbumJukeboxBlockEntity(BlockPos pos, BlockState state) {
        super(EtchedBlocks.ALBUM_JUKEBOX_BE.get(), pos, state);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        this.playingIndex = -1;
        this.track = 0;
        this.playingStack = ItemStack.EMPTY;
        this.volume = 1.0F;
        this.radius = DEFAULT_RADIUS;
    }

    public static void tickClient(Level level, BlockPos pos, BlockState state, AlbumJukeboxBlockEntity entity) {
        if (!entity.loaded) {
            entity.loaded = true;
            SoundTracker.playAlbum(entity, state, level, pos, false);
        }

        if (entity.isPlaying()) {
            AABB range = new AABB(pos).inflate(3.45);
            List<LivingEntity> livingEntities = level.getEntitiesOfClass(LivingEntity.class, range);
            livingEntities.forEach(living -> living.setRecordPlayingNearby(pos, true));
        }
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
        if (this.level == null) {
            return;
        }
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, this.items, registries);
        }
        this.volume = tag.contains("Volume", Tag.TAG_FLOAT) ? Mth.clamp(tag.getFloat("Volume"), 0.0F, 1.0F) : 1.0F;
        this.radius = tag.contains("Radius", Tag.TAG_INT) ? Mth.clamp(tag.getInt("Radius"), SoundSettingsMenu.MIN_RADIUS, SoundSettingsMenu.MAX_RADIUS) : DEFAULT_RADIUS;
        if (this.loaded) {
            SoundTracker.playAlbum(this, this.getBlockState(), this.level, this.getBlockPos(), false);
            SoundTracker.setBlockSoundSettings(this.getBlockPos(), this.volume, this.radius, DEFAULT_RADIUS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, this.items, registries);
        }
        tag.putFloat("Volume", this.volume);
        tag.putInt("Radius", this.radius);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Mods like Create remove the block entity before the block, so the stop event from onRemove never reaches clients
        if (this.loaded && this.level != null && this.level.isClientSide()) {
            this.loaded = false;
            SoundTracker.stopBlockSound(this.level, this.getBlockPos());
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
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
        if (!stack.isEmpty()) {
            this.updatePlaying();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = super.removeItemNoUpdate(index);
        this.updateState();
        if (!stack.isEmpty()) {
            this.updatePlaying();
        }
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
        return Component.translatable("container." + Etched.MOD_ID + ".album_jukebox");
    }

    @Override
    protected AbstractContainerMenu createMenu(int menuId, Inventory inventory) {
        return new AlbumJukeboxMenu(menuId, inventory, this, this.getBlockPos());
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /**
     * @return The volume of the jukebox from 0 to 1
     */
    public float getVolume() {
        return this.volume;
    }

    @Override
    public void setVolume(float volume) {
        volume = Mth.clamp(volume, 0.0F, 1.0F);
        if (this.volume != volume) {
            this.volume = volume;
            this.setChanged();
            this.updatePlaying();
        }
    }

    /**
     * @return How far away the jukebox can be heard in blocks
     */
    public int getRadius() {
        return this.radius;
    }

    @Override
    public void setRadius(int radius) {
        radius = Mth.clamp(radius, SoundSettingsMenu.MIN_RADIUS, SoundSettingsMenu.MAX_RADIUS);
        if (this.radius != radius) {
            this.radius = radius;
            this.setChanged();
            this.updatePlaying();
        }
    }

    public int getPlayingIndex() {
        return this.playingIndex;
    }

    public int getTrack() {
        return this.track;
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
            int tracks = PlayableRecord.getTrackCount(this.level.registryAccess(), this.playingStack);
            if (this.track >= tracks) {
                this.track = 0;
            }
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
            if (this.playingIndex < 0) {
                this.playingIndex = this.getContainerSize() - 1;
            }
            this.nextPlayingIndex(true);
            this.track = Math.max(0, this.playingIndex < 0 || this.playingIndex >= this.getContainerSize() ? 0 : PlayableRecord.getTrackCount(this.level.registryAccess(), this.getItem(this.playingIndex)) - 1);
            this.playingStack = ItemStack.EMPTY;
        }
    }

    /**
     * Cycles to the next index to begin playing.
     */
    public void next() {
        int tracks = this.playingIndex < 0 || this.playingIndex >= this.getContainerSize() ? 1 : PlayableRecord.getTrackCount(this.level.registryAccess(), this.getItem(this.playingIndex));
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
            if (this.playingIndex == -1) {
                return false;
            }
            this.playingIndex = -1;
            this.track = 0;
            return true;
        }

        int oldIndex = this.playingIndex;
        ItemStack oldStack = this.playingStack.copy();
        this.nextPlayingIndex(reverse);
        if (oldIndex != this.playingIndex || !ItemStack.matches(oldStack, this.playingStack)) {
            if (!reverse) {
                this.track = 0;
                return true;
            }

            if (this.playingIndex < 0 || this.playingIndex >= this.getContainerSize()) {
                this.track = 0;
                return true;
            }

            this.track = Math.max(0, PlayableRecord.getTrackCount(this.level.registryAccess(), this.getItem(this.playingIndex)) - 1);
            return true;
        }
        return false;
    }

    public boolean isPlaying() {
        BlockState state = this.getBlockState();
        return (!state.hasProperty(AlbumJukeboxBlock.POWERED) || !state.getValue(AlbumJukeboxBlock.POWERED)) && !this.isEmpty();
    }
}