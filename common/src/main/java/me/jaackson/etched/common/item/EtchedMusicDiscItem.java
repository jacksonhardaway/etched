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
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * @author Ocelot
 */
public class EtchedMusicDiscItem extends Item {

    public EtchedMusicDiscItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag tooltipFlag) {
        getMusic(stack).ifPresent(music -> list.add(music.getDisplayName().copy().withStyle(ChatFormatting.GRAY)));
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
            NetworkBridge.sendToNear((ServerLevel) level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 64, new ClientboundPlayMusicPacket(music.getDisplayName(), music.getUrl(), pos));
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
        CompoundTag nbt = stack.getTag();
        if (nbt == null || !nbt.contains("Music", 10))
            return Optional.empty();

        MusicInfo music = new MusicInfo();
        music.load(nbt.getCompound("Music"));
        return music.getUrl() != null && isValidURL(music.getUrl()) ? Optional.of(music) : Optional.empty();
    }

    /**
     * Retrieves the label pattern from the specified stack.
     *
     * @param stack The stack to get the pattern from
     * @return The pattern for that item
     */
    public static LabelPattern getPattern(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt == null || !nbt.contains("Pattern", 99))
            return LabelPattern.FLAT;
        int id = nbt.getByte("Pattern");
        return id < 0 || id >= LabelPattern.values().length ? LabelPattern.FLAT : LabelPattern.values()[id];
    }

    /**
     * Retrieves the color of the physical disc from the specified stack.
     *
     * @param stack The stack to get the color from
     * @return The color for the physical disc
     */
    public static int getPrimaryColor(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt == null || !nbt.contains("PrimaryColor", 99))
            return 0x515151;
        return nbt.getInt("PrimaryColor");
    }

    /**
     * Retrieves the color of the label from the specified stack.
     *
     * @param stack The stack to get the color from
     * @return The color for the label
     */
    public static int getSecondaryColor(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt == null || !nbt.contains("SecondaryColor", 99))
            return 0xFFFFFF;
        return nbt.getInt("SecondaryColor");
    }

    /**
     * Sets the URL for the specified stack.
     *
     * @param stack     The stack to set NBT for
     * @param musicInfo The music to apply to the disk
     */
    public static void setMusic(ItemStack stack, @Nullable MusicInfo musicInfo) {
        if (musicInfo == null) {
            if (stack.getTag() != null)
                stack.getTag().remove("Music");
        } else {
            stack.getOrCreateTag().put("Music", musicInfo.save(new CompoundTag()));
        }
    }

    /**
     * Sets the pattern for the specified stack.
     *
     * @param stack   The stack to set NBT for
     * @param pattern The pattern to apply to the disk or <code>null</code> to remove and default to {@link LabelPattern#FLAT}
     */
    public static void setPattern(ItemStack stack, @Nullable LabelPattern pattern) {
        if (pattern == null) {
            if (stack.getTag() != null)
                stack.getTag().remove("Pattern");
        } else {
            stack.getOrCreateTag().putByte("Pattern", (byte) pattern.ordinal());
        }
    }

    /**
     * Sets the color for the specified stack.
     *
     * @param stack          The stack to set NBT for
     * @param primaryColor   The color to use for the physical disk
     * @param secondaryColor The color to use for the label
     */
    public static void setColor(ItemStack stack, int primaryColor, int secondaryColor) {
        stack.getOrCreateTag().putInt("PrimaryColor", primaryColor);
        stack.getOrCreateTag().putInt("SecondaryColor", secondaryColor);
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
        @Nullable
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

        /**
         * Sets the URL to the music file.
         *
         * @param url The url to use
         */
        public void setUrl(String url) {
            this.url = url;
        }

        /**
         * @param title The title of the music
         */
        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * @param author The player who authored the disc
         */
        public void setAuthor(String author) {
            this.author = author;
        }

        /**
         * @return The name to show as the record title
         */
        public Component getDisplayName() {
            return new TextComponent(this.author + " - " + this.title);
        }
    }

    /**
     * @author Jackson
     */
    public enum LabelPattern {

        FLAT,
        CROSS,
        EYE,
        PARALLEL,
        STAR,
        GOLD;

        /**
         * @return Whether or not this label can be colored
         */
        public boolean isColorable() {
            return this != GOLD;
        }
    }

    /**
     * Checks to see if the speciied string is a valid music URL.
     *
     * @param url The text to check
     * @return Whether or not the data is valid
     */
    public static boolean isValidURL(String url) {
        try {
            String scheme = new URI(url).getScheme();
            return "http".equals(scheme) || "https".equals(scheme);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Checks to see if the specified stack can be played in a jukebox.
     *
     * @param stack The stack to check
     * @return Whether or not that stack can play
     */
    public static boolean isPlayableRecord(ItemStack stack) {
        if (stack.getItem() instanceof RecordItem)
            return true;
        if (stack.getItem() == EtchedRegistry.ETCHED_MUSIC_DISC.get())
            return getMusic(stack).isPresent();
        return false;
    }
}
