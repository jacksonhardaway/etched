package gg.moonflower.etched.common.blockentity;

import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.block.RadioBlock;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import gg.moonflower.pollen.api.util.NbtConstants;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Ocelot
 */
public class RadioBlockEntity extends BlockEntity implements Clearable {

    private String url;

    public RadioBlockEntity(BlockPos pos, BlockState state) {
        super(EtchedBlocks.RADIO_BE.get(), pos, state);
    }

    @Override
    public void load(CompoundTag nbt) {
        this.url = nbt.contains("Url", NbtConstants.STRING) ? nbt.getString("Url") : null;
        if (this.level != null && this.level.isClientSide())
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

    public boolean isPlaying() {
        BlockState state = this.getBlockState();
        return (!state.hasProperty(RadioBlock.POWERED) || !state.getValue(RadioBlock.POWERED)) && !StringUtil.isNullOrEmpty(this.url);
    }

    public void setUrl(String url) {
        if (!Objects.equals(this.url, url)) {
            this.url = url;
            this.setChanged();
            if (this.level != null)
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
}
