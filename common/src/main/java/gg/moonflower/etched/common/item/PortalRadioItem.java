package gg.moonflower.etched.common.item;

import gg.moonflower.etched.common.block.RadioBlock;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PortalRadioItem extends BlockItem {

    public PortalRadioItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Nullable
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState blockState = this.getBlock().getStateForPlacement(context);
        if (blockState == null)
            return null;
        blockState = blockState.setValue(RadioBlock.PORTAL, true);
        return this.canPlace(context, blockState) ? blockState : null;
    }

    @Override
    public String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }
}
