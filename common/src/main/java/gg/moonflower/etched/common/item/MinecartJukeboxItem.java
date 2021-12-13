package gg.moonflower.etched.common.item;

import gg.moonflower.etched.common.entity.MinecartJukebox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

/**
 * @author Ocelot
 */
public class MinecartJukeboxItem extends Item {

    private static final DispenseItemBehavior DISPENSE_ITEM_BEHAVIOR = new DefaultDispenseItemBehavior() {
        private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

        @Override
        public ItemStack execute(BlockSource source, ItemStack stack) {
            Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
            Level level = source.getLevel();
            double d = source.x() + (double) direction.getStepX() * 1.125D;
            double e = Math.floor(source.y()) + (double) direction.getStepY();
            double f = source.z() + (double) direction.getStepZ() * 1.125D;
            BlockPos blockPos = source.getPos().relative(direction);
            BlockState blockState = level.getBlockState(blockPos);
            RailShape railShape = blockState.getBlock() instanceof BaseRailBlock ? blockState.getValue(((BaseRailBlock) blockState.getBlock()).getShapeProperty()) : RailShape.NORTH_SOUTH;
            double k;
            if (blockState.is(BlockTags.RAILS)) {
                if (railShape.isAscending()) {
                    k = 0.6D;
                } else {
                    k = 0.1D;
                }
            } else {
                if (!blockState.isAir() || !level.getBlockState(blockPos.below()).is(BlockTags.RAILS))
                    return this.defaultDispenseItemBehavior.dispense(source, stack);

                BlockState blockState2 = level.getBlockState(blockPos.below());
                RailShape railShape2 = blockState2.getBlock() instanceof BaseRailBlock ? blockState2.getValue(((BaseRailBlock) blockState2.getBlock()).getShapeProperty()) : RailShape.NORTH_SOUTH;
                if (direction != Direction.DOWN && railShape2.isAscending()) {
                    k = -0.4D;
                } else {
                    k = -0.9D;
                }
            }

            MinecartJukebox jukeboxMinecart = new MinecartJukebox(level, d, e + k, f);
            if (stack.hasCustomHoverName())
                jukeboxMinecart.setCustomName(stack.getHoverName());

            level.addFreshEntity(jukeboxMinecart);
            stack.shrink(1);
            return stack;
        }

        protected void playSound(BlockSource blockSource) {
            blockSource.getLevel().levelEvent(1000, blockSource.getPos(), 0);
        }
    };

    public MinecartJukeboxItem(Item.Properties properties) {
        super(properties);
        DispenserBlock.registerBehavior(this, DISPENSE_ITEM_BEHAVIOR);
    }

    @Override
    public InteractionResult useOn(UseOnContext useOnContext) {
        Level level = useOnContext.getLevel();
        BlockPos blockPos = useOnContext.getClickedPos();
        BlockState blockState = level.getBlockState(blockPos);
        if (!blockState.is(BlockTags.RAILS))
            return InteractionResult.FAIL;

        ItemStack stack = useOnContext.getItemInHand();
        if (!level.isClientSide()) {
            RailShape railShape = blockState.getBlock() instanceof BaseRailBlock ? blockState.getValue(((BaseRailBlock) blockState.getBlock()).getShapeProperty()) : RailShape.NORTH_SOUTH;
            double d = 0.0D;
            if (railShape.isAscending())
                d = 0.5D;

            MinecartJukebox jukeboxMinecart = new MinecartJukebox(level, blockPos.getX() + 0.5D, blockPos.getY() + 0.0625D + d, blockPos.getZ() + 0.5D);
            if (stack.hasCustomHoverName())
                jukeboxMinecart.setCustomName(stack.getHoverName());

            level.addFreshEntity(jukeboxMinecart);
        }

        stack.shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
