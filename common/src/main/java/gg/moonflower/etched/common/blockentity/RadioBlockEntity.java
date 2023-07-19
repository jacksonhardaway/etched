package gg.moonflower.etched.common.blockentity;

import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.block.RadioBlock;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
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

    private String url;
    private boolean loaded;

    public RadioBlockEntity(BlockPos pos, BlockState state) {
        super(EtchedBlocks.RADIO_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioBlockEntity blockEntity) {
        if (level == null || !level.isClientSide())
            return;

        if (!blockEntity.loaded) {
            blockEntity.loaded = true;
            SoundTracker.playRadio(blockEntity.url, state, (ClientLevel) level, pos);
        }

        if (blockEntity.isPlaying()) {
            AABB range = new AABB(pos).inflate(3.45);
            List<LivingEntity> livingEntities = level.getEntitiesOfClass(LivingEntity.class, range);
            livingEntities.forEach(living -> living.setRecordPlayingNearby(pos, true));
        }
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.url = nbt.contains("Url", Tag.TAG_STRING) ? nbt.getString("Url") : null;
        if (this.loaded)
            SoundTracker.playRadio(this.url, this.getBlockState(), (ClientLevel) this.level, this.getBlockPos());
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        if (this.url != null)
            nbt.putString("Url", this.url);
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
    public void clearContent() {
        this.url = null;
        if (this.level != null && this.level.isClientSide())
            SoundTracker.playRadio(this.url, this.getBlockState(), (ClientLevel) this.level, this.getBlockPos());
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (!Objects.equals(this.url, url)) {
            this.url = url;
            this.setChanged();
            if (this.level != null)
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public boolean isPlaying() {
        BlockState state = this.getBlockState();
        return (!state.hasProperty(RadioBlock.POWERED) || !state.getValue(RadioBlock.POWERED)) && !StringUtil.isNullOrEmpty(this.url);
    }
}
