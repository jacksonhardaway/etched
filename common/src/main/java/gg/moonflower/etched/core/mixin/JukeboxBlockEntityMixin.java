package gg.moonflower.etched.core.mixin;

import net.minecraft.core.BlockPos;
import gg.moonflower.etched.api.record.PlayableRecord;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Ocelot
 */
@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin extends BlockEntity implements WorldlyContainer {

    @Unique
    private static final int[] SLOTS = {0};

    @Unique
    private boolean inserting;

    public JukeboxBlockEntityMixin(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }

    private void startPlaying(ItemStack stack) {
        if (this.level == null)
            return;

        if (!stack.isEmpty())
            stack.useOn(new DirectionalPlaceContext(this.level, this.getBlockPos(), Direction.DOWN, stack, Direction.UP));
    }

    private void stopPlaying() {
        if (this.level == null)
            return;

        BlockState state = this.level.getBlockState(this.getBlockPos());
        if (state.is(Blocks.JUKEBOX) && state.getValue(JukeboxBlock.HAS_RECORD)) {
            this.level.levelEvent(1010, this.getBlockPos(), 0);
            state = state.setValue(JukeboxBlock.HAS_RECORD, false);
            this.level.setBlock(this.getBlockPos(), state, 2);
        }
    }

    @Shadow
    public abstract ItemStack getRecord();

    @Shadow
    public abstract void setRecord(ItemStack stack);

    @Inject(method = "setRecord", at = @At("HEAD"), cancellable = true)
    public void cancelSetRecord(ItemStack stack, CallbackInfo ci) {
        if (this.inserting)
            ci.cancel();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return PlayableRecord.isPlayableRecord(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.getRecord().isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? this.getRecord() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (index != 0)
            return ItemStack.EMPTY;
        ItemStack split = this.getRecord().split(count);
        this.setChanged();
        if (this.getRecord().isEmpty())
            this.stopPlaying();
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return this.removeItem(index, this.getRecord().getCount());
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index == 0) {
            if (!this.getRecord().isEmpty())
                this.stopPlaying();
            this.inserting = true;
            this.startPlaying(stack.copy());
            this.inserting = false;
            this.setRecord(stack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {
        if (!this.getRecord().isEmpty())
            this.setItem(0, ItemStack.EMPTY);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
