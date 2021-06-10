package me.jaackson.etched.common.item;

import me.jaackson.etched.bridge.NetworkBridge;
import me.jaackson.etched.common.network.ClientboundPlayMusicPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;

public class EtchedMusicDiscItem extends Item {

    public EtchedMusicDiscItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.JUKEBOX) || state.getValue(JukeboxBlock.HAS_RECORD))
            return InteractionResult.PASS;

        ItemStack stack = ctx.getItemInHand();
        if (!level.isClientSide()) {
            ((JukeboxBlock) Blocks.JUKEBOX).setRecord(level, pos, state, stack);
            NetworkBridge.sendToNear(ClientboundPlayMusicPacket.CHANNEL, (ServerLevel) level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16, new ClientboundPlayMusicPacket(new TextComponent("Epic Music Broh"), "https://cdn.discordapp.com/attachments/768112600981438534/852342836326301704/giorno_theme.mp3", pos));
            stack.shrink(1);
            Player player = ctx.getPlayer();
            if (player != null)
                player.awardStat(Stats.PLAY_RECORD);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
