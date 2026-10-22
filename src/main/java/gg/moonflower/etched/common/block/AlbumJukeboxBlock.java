package gg.moonflower.etched.common.block;

import com.mojang.serialization.MapCodec;
import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import gg.moonflower.etched.core.mixin.client.render.LevelRendererAccessor;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author Ocelot
 */
public class AlbumJukeboxBlock extends BaseEntityBlock {

    public static final MapCodec<AlbumJukeboxBlock> CODEC = simpleCodec(AlbumJukeboxBlock::new);

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HAS_RECORD = BlockStateProperties.HAS_RECORD;

    public AlbumJukeboxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(HAS_RECORD, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof AlbumJukeboxBlockEntity jukebox) {
            player.openMenu(jukebox, buf -> {
                buf.writeBlockPos(pos);
                buf.writeFloat(jukebox.getVolume());
                buf.writeVarInt(jukebox.getRadius());
                buf.writeVarInt(-1);
            });
        }
        // TODO: stats
        return ItemInteractionResult.CONSUME;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()).setValue(POWERED, ctx.getLevel().hasNeighborSignal(ctx.getClickedPos())).setValue(HAS_RECORD, false);
    }

    @Override
    public void neighborChanged(BlockState blockState, Level level, BlockPos pos, Block block, BlockPos blockPos2, boolean bl) {
        if (!level.isClientSide()) {
            boolean bl2 = blockState.getValue(POWERED);
            if (bl2 != level.hasNeighborSignal(pos)) {
                level.setBlock(pos, blockState.cycle(POWERED), 2);
                level.sendBlockUpdated(pos, blockState, level.getBlockState(pos), 3);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Container container) {
                Containers.dropContents(level, pos, container);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            level.levelEvent(1011, pos, 0);

            super.onRemove(state, level, pos, newState, moving);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new AlbumJukeboxBlockEntity(blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, HAS_RECORD);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AlbumJukeboxBlockEntity jukebox)) {
            return;
        }

        if (jukebox.getPlayingIndex() <= -1) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Map<BlockPos, SoundInstance> sounds = ((LevelRendererAccessor) minecraft.levelRenderer).getPlayingJukeboxSongs();
        if (sounds.containsKey(pos) && minecraft.getSoundManager().isActive(sounds.get(pos))) {
            level.addParticle(ParticleTypes.NOTE, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, random.nextInt(25) / 24D, 0, 0);
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        if (!level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, EtchedBlocks.ALBUM_JUKEBOX_BE.get(), AlbumJukeboxBlockEntity::tickClient);
    }
}
