package gg.moonflower.etched.common.item;

import gg.moonflower.etched.common.block.RadioBlock;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

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
    public void fillItemCategory(CreativeModeTab creativeModeTab, NonNullList<ItemStack> nonNullList) {
    }

    @Override
    public void registerBlocks(Map<Block, Item> map, Item item) {
    }

    @Override
    public String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }
}
