package gg.moonflower.etched.common.item;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.sound.download.SoundSourceManager;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.ClientboundPlayMusicPacket;
import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandlerImpl;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.pollen.api.util.NbtConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * @author Ocelot
 */
public class EtchedMusicDiscItem extends Item implements PlayableRecord {

    private static final Component ALBUM = new TranslatableComponent("item." + Etched.MOD_ID + ".etched_music_disc.album").withStyle(ChatFormatting.BLUE);
    private static final Pattern RESOURCE_LOCATION_PATTERN = Pattern.compile("[a-z0-9_.-]+");

    public EtchedMusicDiscItem(Properties properties) {
        super(properties);
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
     * Checks to see if the specified string is a valid music URL.
     *
     * @param url The text to check
     * @return Whether the data is valid
     */
    public static boolean isValidURL(String url) {
        if (isLocalSound(url))
            return true;
        try {
            String scheme = new URI(url).getScheme();
            return "http".equals(scheme) || "https".equals(scheme);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Checks to see if the specified URL is a resource location sound.
     *
     * @param url The url to check
     * @return Whether that sound can be played as a local sound event
     */
    public static boolean isLocalSound(String url) {
        String[] parts = url.split(":");
        if (parts.length > 2)
            return false;
        for (String part : parts)
            if (!RESOURCE_LOCATION_PATTERN.matcher(part).matches())
                return false;
        return true;
    }

    @Override
    public boolean canPlay(ItemStack stack) {
        return getMusic(stack).isPresent();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public Optional<SoundInstance> createEntitySound(ItemStack stack, Entity entity) {
        return getMusic(stack).map(musicInfo -> EtchedClientPlayPacketHandlerImpl.getEtchedRecord(musicInfo.getUrl(), musicInfo.getDisplayName(), entity));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag tooltipFlag) {
        getMusic(stack).ifPresent(music -> {
            list.add(music.getDisplayName().copy().withStyle(ChatFormatting.GRAY));
            SoundSourceManager.getBrandText(music.getUrl()).ifPresent(list::add);
            if (music.isAlbum())
                list.add(ALBUM);
        });
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
            EtchedMessages.PLAY.sendToNear((ServerLevel) level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 64, new ClientboundPlayMusicPacket(music.getDisplayName(), music.getUrl(), pos));
            stack.shrink(1);
            Player player = ctx.getPlayer();
            if (player != null)
                player.awardStat(Stats.PLAY_RECORD);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }


    /**
     * @author Jackson
     */
    public enum LabelPattern {

        FLAT, CROSS, EYE, PARALLEL, STAR, GOLD;

        private final ResourceLocation texture;

        LabelPattern() {
            this.texture = new ResourceLocation(Etched.MOD_ID, "textures/item/" + this.name().toLowerCase(Locale.ROOT) + "_etched_music_disc_label.png");
        }

        /**
         * @return The location of the label texture
         */
        @Environment(EnvType.CLIENT)
        public ResourceLocation getTexture() {
            return texture;
        }

        /**
         * @return Whether or not this label can be colored
         */
        @Environment(EnvType.CLIENT)
        public boolean isColorable() {
            return this != GOLD;
        }
    }

    /**
     * <p>Music information stored on an etched music disc.</p>
     *
     * @author Ocelot
     */
    public static class MusicInfo {

        private String url;
        private String title;
        private String author;
        private boolean album;

        public MusicInfo() {
            this.url = null;
            this.title = "Custom Music";
            this.author = "Unknown";
            this.album = false;
        }

        private CompoundTag save(CompoundTag nbt) {
            if (this.url != null)
                nbt.putString("Url", this.url);
            if (this.title != null)
                nbt.putString("Title", this.title);
            if (this.author != null)
                nbt.putString("Author", this.author);
            if (this.album)
                nbt.putBoolean("Album", true);
            return nbt;
        }

        private void load(CompoundTag nbt) {
            this.url = nbt.contains("Url", NbtConstants.STRING) ? nbt.getString("Url") : null;
            this.title = nbt.contains("Title", NbtConstants.STRING) ? nbt.getString("Title") : "Custom Music";
            this.author = nbt.contains("Author", NbtConstants.STRING) ? nbt.getString("Author") : "Unknown";
            this.album = nbt.contains("Album", NbtConstants.BYTE) && nbt.getBoolean("Album");
        }

        /**
         * @return The URL of the music
         */
        public String getUrl() {
            return url;
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
         * @return The title of the music
         */
        public String getTitle() {
            return title;
        }

        /**
         * @param title The title of the music
         */
        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * @return The player who created the music disk
         */
        public String getAuthor() {
            return author;
        }

        /**
         * @param author The player who authored the disc
         */
        public void setAuthor(String author) {
            this.author = author;
        }

        /**
         * @return Whether this disc contains an album
         */
        public boolean isAlbum() {
            return album;
        }

        /**
         * @param album Whether to be an album
         */
        public void setAlbum(boolean album) {
            this.album = album;
        }

        /**
         * @return The name to show as the record title
         */
        public Component getDisplayName() {
            return new TextComponent(this.author + " - " + this.title);
        }
    }
}
