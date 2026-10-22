package gg.moonflower.etched.common.blockentity;

import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.block.RadioBlock;
import gg.moonflower.etched.common.menu.SoundSettingsMenu;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
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

    /**
     * How far away the radio can be heard in blocks unless changed.
     */
    public static final int DEFAULT_RADIUS = 32;

    private String url;
    private float volume;
    private int radius;
    private boolean loaded;
    private String playingUrl;

    public RadioBlockEntity(BlockPos pos, BlockState state) {
        super(EtchedBlocks.RADIO_BE.get(), pos, state);
        this.volume = 1.0F;
        this.radius = DEFAULT_RADIUS;
    }

    public static void tickClient(Level level, BlockPos pos, BlockState state, RadioBlockEntity blockEntity) {
        if (!blockEntity.loaded) {
            blockEntity.loaded = true;
            blockEntity.playingUrl = blockEntity.getPlayingUrl();
            SoundTracker.playRadio(blockEntity.url, state, level, pos);
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
        this.url = tag.contains("Url", Tag.TAG_STRING) ? tag.getString("Url") : null;
        this.volume = tag.contains("Volume", Tag.TAG_FLOAT) ? Mth.clamp(tag.getFloat("Volume"), 0.0F, 1.0F) : 1.0F;
        this.radius = tag.contains("Radius", Tag.TAG_INT) ? Mth.clamp(tag.getInt("Radius"), SoundSettingsMenu.MIN_RADIUS, SoundSettingsMenu.MAX_RADIUS) : DEFAULT_RADIUS;
        if (this.loaded) {
            // Only a new station or turning the radio on or off restarts the stream, the volume can change while playing
            String playingUrl = this.getPlayingUrl();
            if (!Objects.equals(this.playingUrl, playingUrl)) {
                this.playingUrl = playingUrl;
                SoundTracker.playRadio(this.url, this.getBlockState(), this.level, this.getBlockPos());
            }
            SoundTracker.setBlockSoundSettings(this.getBlockPos(), this.volume, this.radius, DEFAULT_RADIUS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.url != null) {
            tag.putString("Url", this.url);
        }
        tag.putFloat("Volume", this.volume);
        tag.putInt("Radius", this.radius);
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
        if (this.level != null && this.level.isClientSide()) {
            this.playingUrl = null;
            SoundTracker.playRadio(this.url, this.getBlockState(), this.level, this.getBlockPos());
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Mods like Create remove the block entity before the block, so the stop event from onRemove never reaches clients
        if (this.loaded && this.level != null && this.level.isClientSide()) {
            this.loaded = false;
            SoundTracker.playRadio(null, this.getBlockState(), this.level, this.getBlockPos());
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

    /**
     * @return The volume of the radio from 0 to 1
     */
    public float getVolume() {
        return this.volume;
    }

    public void setVolume(float volume) {
        volume = Mth.clamp(volume, 0.0F, 1.0F);
        if (this.volume != volume) {
            this.volume = volume;
            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }
    }

    /**
     * @return How far away the radio can be heard in blocks
     */
    public int getRadius() {
        return this.radius;
    }

    public void setRadius(int radius) {
        radius = Mth.clamp(radius, SoundSettingsMenu.MIN_RADIUS, SoundSettingsMenu.MAX_RADIUS);
        if (this.radius != radius) {
            this.radius = radius;
            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }
    }

    private @Nullable String getPlayingUrl() {
        return this.isPlaying() ? this.url : null;
    }

    public boolean isPlaying() {
        BlockState state = this.getBlockState();
        return (!state.hasProperty(RadioBlock.POWERED) || !state.getValue(RadioBlock.POWERED)) && !StringUtil.isNullOrEmpty(this.url);
    }
}
