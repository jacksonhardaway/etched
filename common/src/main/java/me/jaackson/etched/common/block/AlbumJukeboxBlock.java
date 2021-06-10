package me.jaackson.etched.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author Ocelot
 */
public class AlbumJukeboxBlock extends Block {

    public AlbumJukeboxBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof JukeboxBlockEntity) {
            Item item = ((JukeboxBlockEntity) blockEntity).getRecord().getItem();
            if (item instanceof RecordItem) {
                return ((RecordItem) item).getAnalogOutput();
            }
        }

        return 0;
    }
}
