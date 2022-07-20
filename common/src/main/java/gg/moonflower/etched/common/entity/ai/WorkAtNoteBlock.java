package gg.moonflower.etched.common.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.WorkAtPoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class WorkAtNoteBlock extends WorkAtPoi {

    @Override
    protected void useWorkstation(ServerLevel level, Villager villager) {
        Optional<GlobalPos> optional = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        if (optional.isPresent()) {
            GlobalPos globalPos = optional.get();
            BlockState blockState = level.getBlockState(globalPos.pos());
            if (blockState.is(Blocks.NOTE_BLOCK)) {
                this.playNoteBlock(level, villager, globalPos, blockState);
            }
        }
    }

    private void playNoteBlock(ServerLevel level, Villager villager, GlobalPos globalPos, BlockState state) {
        BlockPos pos = globalPos.pos();
        if (villager.getRandom().nextBoolean()) {
            state = state.cycle(NoteBlock.NOTE);
            level.setBlock(pos, state, 3);
        }

        this.playNote(level, state.getBlock(), pos);
    }

    private void playNote(Level level, Block block, BlockPos pos) {
        if (level.getBlockState(pos.above()).isAir()) {
            level.blockEvent(pos, block, 0, 0);
        }
    }

}

