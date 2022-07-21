package gg.moonflower.etched.common.block;

import gg.moonflower.etched.common.blockentity.RadioBlockEntity;
import gg.moonflower.etched.common.menu.RadioMenu;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.ClientboundSetUrlPacket;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.mixin.client.LevelRendererAccessor;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.Random;

public class RadioBlock extends BaseEntityBlock {

    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty PORTAL = BooleanProperty.create("portal");
    private static final VoxelShape X_SHAPE = Block.box(5.0D, 0.0D, 2.0D, 11.0D, 8.0D, 14.0D);
    private static final VoxelShape Z_SHAPE = Block.box(2.0D, 0.0D, 5.0D, 14.0D, 8.0D, 11.0D);
    private static final VoxelShape ROTATED_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 8.0D, 13.0D);
    private static final Component CONTAINER_TITLE = new TranslatableComponent("container." + Etched.MOD_ID + ".radio");

    public RadioBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ROTATION, 0).setValue(POWERED, false).setValue(PORTAL, false));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if (level.isClientSide())
            return InteractionResult.SUCCESS;
        ItemStack stack = player.getItemInHand(interactionHand);
        if (stack.getItem() == Items.CAKE && !state.getValue(PORTAL)) {
            if (!player.isCreative())
                stack.shrink(1);
            level.setBlock(pos, state.setValue(PORTAL, true), 3);
            return InteractionResult.SUCCESS;
        }
        player.openMenu(state.getMenuProvider(level, pos)).ifPresent(__ -> {
            String url = "";
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RadioBlockEntity)
                url = ((RadioBlockEntity) be).getUrl();
            EtchedMessages.PLAY.sendTo((ServerPlayer) player, new ClientboundSetUrlPacket(url));
        });
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(ROTATION, Mth.floor((double) ((180.0F + context.getRotation()) * 16.0F / 360.0F) + 0.5) & 15)
            .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
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
            if (blockEntity instanceof RadioBlockEntity) {
                if (((RadioBlockEntity) blockEntity).isPlaying())
                    level.levelEvent(1010, pos, 0);
                Clearable.tryClear(blockEntity);
            }

            super.onRemove(state, level, pos, newState, moving);
        }
    }

    @Override
    public MenuProvider getMenuProvider(BlockState blockState, Level level, BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        return new SimpleMenuProvider((menuId, playerInventory, player) -> new RadioMenu(menuId, playerInventory, ContainerLevelAccess.create(level, blockPos), blockEntity instanceof RadioBlockEntity ? ((RadioBlockEntity) blockEntity)::setUrl : url -> {
        }), CONTAINER_TITLE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext collisionContext) {
        int rotation = state.getValue(ROTATION);
        if (rotation % 8 == 0)
            return Z_SHAPE;
        if (rotation % 8 == 4)
            return X_SHAPE;
        return ROTATED_SHAPE;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState blockState) {
        return true;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 16));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), 16));
    }

    @Override
    public BlockEntity newBlockEntity(BlockGetter level) {
        return new RadioBlockEntity();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION, POWERED, PORTAL);
    }

    @Override
    public boolean isPathfindable(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(state.getValue(PORTAL) ? EtchedBlocks.PORTAL_RADIO_ITEM.get() : EtchedBlocks.RADIO.get());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, Random random) {
        if (!Etched.CLIENT_CONFIG.showNotes.get() || !level.getBlockState(pos.above()).isAir())
            return;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof RadioBlockEntity))
            return;

        RadioBlockEntity radio = ((RadioBlockEntity) blockEntity);
        if (radio.getUrl() == null)
            return;

        Minecraft minecraft = Minecraft.getInstance();
        Map<BlockPos, SoundInstance> sounds = ((LevelRendererAccessor) minecraft.levelRenderer).getPlayingRecords();
        if (sounds.containsKey(pos) && minecraft.getSoundManager().isActive(sounds.get(pos)))
            level.addParticle(ParticleTypes.NOTE, pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D, random.nextInt(25) / 24D, 0, 0);
    }
}
