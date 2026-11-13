package gg.moonflower.etched.common.blockentity;

import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.block.RadioBlock;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * @author Ocelot
 */
public class RadioBlockEntity extends BlockEntity implements Clearable {

    public static final int DEFAULT_VOLUME_PERCENT = 50;
    public static final int MIN_VOLUME_PERCENT = 0;
    public static final int MAX_VOLUME_PERCENT = 100;
    public static final int DEFAULT_RANGE = 32;
    public static final int MIN_RANGE = 4;
    public static final int MAX_RANGE = 32;

    private String url;
    private int volumePercent = DEFAULT_VOLUME_PERCENT;
    private int range = DEFAULT_RANGE;
    private boolean loaded;
    private boolean lastPlaying;

    public RadioBlockEntity(BlockPos pos, BlockState state) {
        super(EtchedBlocks.RADIO_BE.get(), pos, state);
    }

    public static void tickClient(Level level, BlockPos pos, BlockState state, RadioBlockEntity blockEntity) {
        if (!blockEntity.loaded) {
            blockEntity.loaded = true;
            blockEntity.lastPlaying = blockEntity.isPlaying();
            SoundTracker.playRadio(blockEntity.url, state, level, pos);
        } else {
            boolean playing = blockEntity.isPlaying();
            if (playing != blockEntity.lastPlaying) {
                blockEntity.lastPlaying = playing;
                if (playing) {
                    if (!SoundTracker.setRadioMuted(pos, false)) {
                        SoundTracker.playRadio(blockEntity.url, state, level, pos);
                    }
                } else {
                    SoundTracker.setRadioMuted(pos, true);
                }
            }
        }

        if (blockEntity.isPlaying()) {
            AABB range = new AABB(pos).inflate(3.45);
            List<LivingEntity> livingEntities = level.getEntitiesOfClass(LivingEntity.class, range);
            livingEntities.forEach(living -> living.setRecordPlayingNearby(pos, true));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        String previousUrl = this.url;
        int previousVolume = this.volumePercent;
        int previousRange = this.range;
        this.url = tag.contains("Url", Tag.TAG_STRING) ? tag.getString("Url") : null;
        this.volumePercent = tag.contains("VolumePercent", Tag.TAG_INT)
                ? clampVolume(tag.getInt("VolumePercent"))
                : DEFAULT_VOLUME_PERCENT;
        this.range = tag.contains("Range", Tag.TAG_INT)
                ? clampRange(tag.getInt("Range"))
                : DEFAULT_RANGE;
        if (this.loaded) {
            if (!Objects.equals(previousUrl, this.url)) {
                this.lastPlaying = this.isPlaying();
                SoundTracker.playRadio(this.url, this.getBlockState(), this.level, this.getBlockPos());
            } else if (previousVolume != this.volumePercent || previousRange != this.range) {
                SoundTracker.updateRadioSettings(this.level, this.getBlockPos(), this.volumePercent, this.range);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.url != null) {
            tag.putString("Url", this.url);
        }
        tag.putInt("VolumePercent", this.volumePercent);
        tag.putInt("Range", this.range);
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
    public void clearContent() {
        this.url = null;
        this.lastPlaying = false;
        if (this.level != null && this.level.isClientSide()) {
            SoundTracker.playRadio(this.url, this.getBlockState(), this.level, this.getBlockPos());
        }
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        if (!Objects.equals(this.url, url)) {
            this.url = url;
            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }
    }

    public int getVolumePercent() {
        return this.volumePercent;
    }

    public int getRange() {
        return this.range;
    }

    public void setBroadcastSettings(int volumePercent, int range) {
        int newVolume = clampVolume(volumePercent);
        int newRange = clampRange(range);
        if (this.volumePercent == newVolume && this.range == newRange) {
            return;
        }

        this.volumePercent = newVolume;
        this.range = newRange;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    private static int clampVolume(int value) {
        return Math.max(MIN_VOLUME_PERCENT, Math.min(MAX_VOLUME_PERCENT, value));
    }

    private static int clampRange(int value) {
        return Math.max(MIN_RANGE, Math.min(MAX_RANGE, value));
    }

    public boolean isPlaying() {
        BlockState state = this.getBlockState();
        return (!state.hasProperty(RadioBlock.POWERED) || !state.getValue(RadioBlock.POWERED)) && !StringUtil.isNullOrEmpty(this.url);
    }
}
