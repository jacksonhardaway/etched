package gg.moonflower.etched.api.record;

import gg.moonflower.etched.api.sound.download.SoundSourceManager;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.ClientboundPlayMusicPacket;
import gg.moonflower.etched.core.Etched;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.net.Proxy;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class PlayableRecordItem extends Item implements PlayableRecord {

    private static final Component ALBUM = new TranslatableComponent("item." + Etched.MOD_ID + ".etched_music_disc.album").withStyle(ChatFormatting.DARK_GRAY);

    public PlayableRecordItem(Properties properties) {
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
        if (!this.getMusic(stack).isPresent())
            return InteractionResult.PASS;

        if (!level.isClientSide()) {
            ((JukeboxBlock) Blocks.JUKEBOX).setRecord(level, pos, state, stack);
            EtchedMessages.PLAY.sendToNear((ServerLevel) level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 64, new ClientboundPlayMusicPacket(stack.copy(), pos));
            stack.shrink(1);
            Player player = ctx.getPlayer();
            if (player != null)
                player.awardStat(Stats.PLAY_RECORD);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag tooltipFlag) {
        this.getAlbum(stack).ifPresent(track -> {
            list.add(track.getDisplayName().copy().withStyle(ChatFormatting.GRAY));
            Component brand = SoundSourceManager.getBrandText(track.getUrl())
                .map(component -> new TextComponent("  ").append(component.copy()))
                .<Component>map(component -> getTrackCount(stack) > 1 ? component.append(" ").append(ALBUM) : component)
                .orElse(getTrackCount(stack) > 1 ? ALBUM : TextComponent.EMPTY);
            list.add(brand);
        });
    }

    @Override
    public CompletableFuture<AlbumCover> getAlbumCover(ItemStack stack, Proxy proxy, ResourceManager resourceManager) {
        return this.getAlbum(stack).map(data -> SoundSourceManager.resolveAlbumCover(data.getUrl(), null, proxy, resourceManager)).orElseGet(() -> CompletableFuture.completedFuture(AlbumCover.EMPTY));
    }
}
