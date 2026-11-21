package gg.moonflower.etched.common.blockentity;

import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.block.RadioBlock;
import gg.moonflower.etched.common.radio.RadioStation;
import gg.moonflower.etched.common.radio.RadioStationSerialization;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Ocelot
 */
public class RadioBlockEntity extends BlockEntity implements Clearable {

    public static final int PRESET_COUNT = 6;

    private String url;
    private final RadioStation[] presets = new RadioStation[PRESET_COUNT];
    private boolean loaded;

    public RadioBlockEntity(BlockPos pos, BlockState state) {
        super(EtchedBlocks.RADIO_BE.get(), pos, state);
        Arrays.fill(this.presets, RadioStation.EMPTY);
    }

    public static void tickClient(Level level, BlockPos pos, BlockState state, RadioBlockEntity blockEntity) {
        if (!blockEntity.loaded) {
            blockEntity.loaded = true;
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
        this.url = new RadioStation("", tag.contains("Url", Tag.TAG_STRING) ? tag.getString("Url") : "").url();
        Arrays.fill(this.presets, RadioStation.EMPTY);
        ListTag presetsTag = tag.getList("Presets", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(PRESET_COUNT, presetsTag.size()); i++) {
            this.presets[i] = RadioStationSerialization.load(presetsTag.getCompound(i));
        }
        if (this.loaded) {
            SoundTracker.playRadio(this.url, this.getBlockState(), this.level, this.getBlockPos());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.url != null) {
            tag.putString("Url", this.url);
        }
        ListTag presetsTag = new ListTag();
        for (RadioStation preset : this.presets) {
            presetsTag.add(RadioStationSerialization.save(preset));
        }
        tag.put("Presets", presetsTag);
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
            SoundTracker.playRadio(this.url, this.getBlockState(), this.level, this.getBlockPos());
        }
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        String nextUrl = new RadioStation("", url).url();
        if (!Objects.equals(this.url, nextUrl)) {
            this.url = nextUrl;
            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }
    }

    public List<RadioStation> getPresets() {
        return List.of(this.presets);
    }

    public void setPreset(int index, RadioStation station) {
        if (index < 0 || index >= PRESET_COUNT) {
            return;
        }

        RadioStation next = station != null ? station : RadioStation.EMPTY;
        if (Objects.equals(this.presets[index], next)) {
            return;
        }

        this.presets[index] = next;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void usePreset(int index) {
        if (index >= 0 && index < PRESET_COUNT && !this.presets[index].isEmpty()) {
            this.setUrl(this.presets[index].url());
        }
    }

    public boolean isPlaying() {
        BlockState state = this.getBlockState();
        return (!state.hasProperty(RadioBlock.POWERED) || !state.getValue(RadioBlock.POWERED)) && !StringUtil.isNullOrEmpty(this.url);
    }
}
