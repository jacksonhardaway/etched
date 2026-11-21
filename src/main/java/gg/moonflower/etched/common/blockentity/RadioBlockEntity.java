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
    private static final int STREAM_PRELOAD_RANGE = 32;
    private static final int STREAM_RETIRE_RANGE = 36;
    private static final int STREAM_IDLE_RETIRE_CHECKS = 30;

    private String url;
    private int volumePercent = DEFAULT_VOLUME_PERCENT;
    private int range = DEFAULT_RANGE;
    private boolean loaded;
    private boolean lastPlaying;
    private int streamCheckTicks;
    private int streamIdleChecks;
    private int nearbyEntityTicks;

    public RadioBlockEntity(BlockPos pos, BlockState state) {
        super(EtchedBlocks.RADIO_BE.get(), pos, state);
    }

    public static void tickClient(Level level, BlockPos pos, BlockState state, RadioBlockEntity blockEntity) {
        boolean playing = blockEntity.isPlaying();
        if (!blockEntity.loaded) {
            blockEntity.loaded = true;
            blockEntity.lastPlaying = playing;
            blockEntity.reconcileClientStream(level, pos, state, playing);
        } else {
            if (playing != blockEntity.lastPlaying) {
                blockEntity.lastPlaying = playing;
                blockEntity.reconcileClientStream(level, pos, state, playing);
            }

            // The URL and powered state survive a disconnect, but the client-side sound instance
            // does not. Reconcile once per second so a radio also recovers when its first tick ran
            // while the joining client's chunks or block-entity data were still settling.
            if (++blockEntity.streamCheckTicks >= 20) {
                blockEntity.streamCheckTicks = 0;
                blockEntity.reconcileClientStream(level, pos, state, playing);
            }
        }

        if (playing && ++blockEntity.nearbyEntityTicks >= 10) {
            blockEntity.nearbyEntityTicks = 0;
            AABB range = new AABB(pos).inflate(3.45);
            List<LivingEntity> livingEntities = level.getEntitiesOfClass(LivingEntity.class, range);
            livingEntities.forEach(living -> living.setRecordPlayingNearby(pos, true));
        } else if (!playing) {
            blockEntity.nearbyEntityTicks = 0;
        }
    }

    private void reconcileClientStream(Level level, BlockPos pos, BlockState state, boolean playing) {
        boolean hasStream = SoundTracker.hasRadioStream(pos, this.url);
        if (!playing) {
            this.keepMutedOrRetire(level, pos, hasStream);
            return;
        }

        double distanceSquared = SoundTracker.getRadioListenerDistanceSquared(pos);
        if (!Double.isFinite(distanceSquared)) {
            return;
        }

        if (hasStream) {
            if (distanceSquared > STREAM_RETIRE_RANGE * STREAM_RETIRE_RANGE) {
                this.keepMutedOrRetire(level, pos, true);
                return;
            }

            this.streamIdleChecks = 0;
            SoundTracker.setRadioMuted(level, pos, false);
            SoundTracker.updateRadioSettings(level, pos, this.volumePercent, this.range);
            return;
        }

        this.streamIdleChecks = 0;
        // The configured range controls audibility. The stream itself is prepared from a
        // fixed 32-block radius so it is already available when the player enters that range.
        if (distanceSquared <= STREAM_PRELOAD_RANGE * STREAM_PRELOAD_RANGE) {
            SoundTracker.playRadio(this.url, state, level, pos);
        }
    }

    /**
     * Keeps a prepared stream warm for a short time so a lever toggle or a quick
     * return to the area resumes immediately. Long-idle streams are retired to avoid
     * keeping unused network connections alive indefinitely.
     */
    private void keepMutedOrRetire(Level level, BlockPos pos, boolean hasStream) {
        if (!hasStream) {
            this.streamIdleChecks = 0;
            return;
        }

        SoundTracker.setRadioMuted(level, pos, true);
        if (++this.streamIdleChecks >= STREAM_IDLE_RETIRE_CHECKS) {
            this.streamIdleChecks = 0;
            SoundTracker.retireRadioStream(level, pos);
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
                SoundTracker.retireRadioStream(this.level, this.getBlockPos());
                this.reconcileClientStream(this.level, this.getBlockPos(), this.getBlockState(), this.lastPlaying);
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
