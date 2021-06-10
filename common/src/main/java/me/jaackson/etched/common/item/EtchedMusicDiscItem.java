package me.jaackson.etched.common.item;

import me.jaackson.etched.EtchedRegistry;
import me.jaackson.etched.bridge.NetworkBridge;
import me.jaackson.etched.common.network.ClientboundPlayMusicPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * @author Ocelot
 */
public class EtchedMusicDiscItem extends Item {

    private static final Map<ItemStack, MusicInfo> CACHE = new WeakHashMap<>();

    public EtchedMusicDiscItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag tooltipFlag) {
        getMusic(stack).ifPresent(music -> list.add(new TextComponent(music.getAuthor() + " - " + music.getTitle()).withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.JUKEBOX) || state.getValue(JukeboxBlock.HAS_RECORD))
            return InteractionResult.PASS;

        ItemStack stack = ctx.getItemInHand();
        Optional<MusicInfo> optional = getMusic(stack);
        if (!optional.isPresent())
            return InteractionResult.PASS;

        if (!level.isClientSide()) {
            MusicInfo music = optional.get();
            ((JukeboxBlock) Blocks.JUKEBOX).setRecord(level, pos, state, stack);
            NetworkBridge.sendToNear(ClientboundPlayMusicPacket.CHANNEL, (ServerLevel) level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16, new ClientboundPlayMusicPacket(new TextComponent(music.getAuthor() + " - " + music.getTitle()), music.getUrl(), pos));
            stack.shrink(1);
            Player player = ctx.getPlayer();
            if (player != null)
                player.awardStat(Stats.PLAY_RECORD);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Retrieves the music URL from the specified stack.
     *
     * @param stack The stack to get NBT from
     * @return The optional URL for that item
     */
    public static Optional<MusicInfo> getMusic(ItemStack stack) {
        if (stack.getItem() != EtchedRegistry.ETCHED_MUSIC_DISC.get())
            return Optional.empty();
        CompoundTag nbt = stack.getTag();
        if (nbt == null || !nbt.contains("Music", 10))
            return Optional.empty();

        MusicInfo music = CACHE.computeIfAbsent(stack, key -> {
            MusicInfo info = new MusicInfo();
            info.load(nbt.getCompound("Music"));
            return info.getUrl() != null ? info : MusicInfo.EMPTY;
        });
        return music != MusicInfo.EMPTY ? Optional.of(music) : Optional.empty();
    }

    /**
     * Sets the URL for the specified stack.
     *
     * @param stack     The stack to set NBT for
     * @param musicInfo The music to apply to the disk
     */
    public static void setMusic(ItemStack stack, @Nullable MusicInfo musicInfo) {
        if (stack.getItem() != EtchedRegistry.ETCHED_MUSIC_DISC.get())
            return;
        if (musicInfo == null) {
            if (stack.getTag() != null) {
                stack.getTag().remove("Music");
                if (stack.getTag().isEmpty())
                    stack.setTag(null);
            }
        } else {
            stack.getOrCreateTag().put("Music", musicInfo.save(new CompoundTag()));
        }
    }

    /**
     * <p>Music information stored on an etched music disc.</p>
     *
     * @author Ocelot
     */
    public static class MusicInfo {

        public static final MusicInfo EMPTY = new MusicInfo();

        private String url;
        private String title;
        private String author;

        public MusicInfo() {
            this.url = null;
            this.title = "Custom Music";
            this.author = "Unknown";
        }

        private CompoundTag save(CompoundTag nbt) {
            if (this.url != null)
                nbt.putString("Url", this.url);
            if (this.title != null)
                nbt.putString("Title", this.title);
            if (this.author != null)
                nbt.putString("Author", this.author);
            return nbt;
        }

        private void load(CompoundTag nbt) {
            this.url = nbt.contains("Url", 8) ? nbt.getString("Url") : null;
            this.title = nbt.contains("Title", 8) ? nbt.getString("Title") : "Custom Music";
            this.author = nbt.contains("Author", 8) ? nbt.getString("Author") : "Unknown";
        }

        /**
         * @return The URL of the music
         */
        public String getUrl() {
            return url;
        }

        /**
         * @return The title of the music
         */
        public String getTitle() {
            return title;
        }

        /**
         * @return The player who created the music disk
         */
        public String getAuthor() {
            return author;
        }
    }
}
