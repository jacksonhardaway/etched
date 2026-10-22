package gg.moonflower.etched.core.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.common.block.AlbumJukeboxBlock;
import gg.moonflower.etched.common.menu.SoundSettingsContainer;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/**
 * The discs of an album jukebox mounted on a contraption, stored in the contraption's copy of the block entity data.
 */
public class ContraptionAlbumJukeboxContainer extends SimpleContainer implements SoundSettingsContainer {

    private final AbstractContraptionEntity entity;
    private final BlockPos localPos;

    public ContraptionAlbumJukeboxContainer(AbstractContraptionEntity entity, BlockPos localPos) {
        super(9);
        this.entity = entity;
        this.localPos = localPos;

        CompoundTag data = CreateCompat.getBlockData(entity, localPos);
        if (data != null) {
            ContainerHelper.loadAllItems(data, this.getItems(), entity.registryAccess());
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();

        CompoundTag data = new CompoundTag();
        ContainerHelper.saveAllItems(data, this.getItems(), true, this.entity.registryAccess());
        CreateCompat.updateBlockData(this.entity, this.localPos, data);

        // Keep the disc showing in the jukebox model like a placed jukebox does
        StructureBlockInfo info = this.entity.getContraption() != null ? this.entity.getContraption().getBlocks().get(this.localPos) : null;
        boolean hasRecord = !this.isEmpty();
        if (info != null && info.state().hasProperty(AlbumJukeboxBlock.HAS_RECORD) && info.state().getValue(AlbumJukeboxBlock.HAS_RECORD) != hasRecord) {
            BlockState state = info.state().setValue(AlbumJukeboxBlock.HAS_RECORD, hasRecord);
            this.entity.setBlock(this.localPos, new StructureBlockInfo(info.pos(), state, info.nbt()));
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return PlayableRecord.isPlayableRecord(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return CreateCompat.canUse(this.entity, this.localPos, EtchedBlocks.ALBUM_JUKEBOX.get(), player);
    }

    @Override
    public void setVolume(float volume) {
        CreateCompat.setVolume(this.entity, this.localPos, volume);
    }

    @Override
    public void setRadius(int radius) {
        CreateCompat.setRadius(this.entity, this.localPos, radius);
    }
}
