package gg.moonflower.etched.common.blockentity;

import dev.architectury.injectables.annotations.PlatformOnly;
import gg.moonflower.etched.common.block.RadioBlock;
import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandlerImpl;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import gg.moonflower.pollen.api.util.NbtConstants;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ocelot
 */
public class RadioBlockEntity extends BlockEntity {

    private String url;

    public RadioBlockEntity() {
        super(EtchedBlocks.RADIO_BE.get());
    }

    @Override
    public void load(BlockState state, CompoundTag nbt) {
        super.load(state, nbt);
        this.url = nbt.contains("Url", NbtConstants.STRING) ? nbt.getString("Url") : null;

        if (this.level != null && this.level.isClientSide() && this.url != null)
            EtchedClientPlayPacketHandlerImpl.playRadio(this.url, (ClientLevel) this.level, this.getBlockPos());
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        super.save(nbt);
        if (this.url != null)
            nbt.putString("Url", this.url);
        return nbt;
    }

    @PlatformOnly(PlatformOnly.FORGE)
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        this.load(this.getBlockState(), pkt.getTag());
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

    public void setUrl(String url) {
        this.url = url;
        this.setChanged();
    }

    public boolean isPlaying() {
        BlockState state = this.getBlockState();
        return (!state.hasProperty(RadioBlock.POWERED) || !state.getValue(RadioBlock.POWERED)) && !StringUtil.isNullOrEmpty(this.url);
    }
}
