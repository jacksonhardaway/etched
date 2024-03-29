package gg.moonflower.etched.core.registry;

import dev.architectury.registry.registries.DeferredRegister;
import gg.moonflower.etched.common.item.*;
import gg.moonflower.etched.core.Etched;
import net.minecraft.core.Registry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class EtchedItems {

    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(Etched.MOD_ID, Registry.ITEM_REGISTRY);

    public static final Supplier<Item> MUSIC_LABEL = REGISTRY.register("music_label", () -> new MusicLabelItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC)));
    public static final Supplier<Item> COMPLEX_MUSIC_LABEL = REGISTRY.register("complex_music_label", () -> new ComplexMusicLabelItem(new Item.Properties()));
    public static final Supplier<Item> BLANK_MUSIC_DISC = REGISTRY.register("blank_music_disc", () -> new BlankMusicDiscItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC)));
    public static final Supplier<Item> ETCHED_MUSIC_DISC = REGISTRY.register("etched_music_disc", () -> new EtchedMusicDiscItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> JUKEBOX_MINECART = REGISTRY.register("jukebox_minecart", () -> new MinecartJukeboxItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION).stacksTo(1)));
    public static final Supplier<Item> BOOMBOX = REGISTRY.register("boombox", () -> new BoomboxItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1)));
    public static final Supplier<Item> ALBUM_COVER = REGISTRY.register("album_cover", () -> new AlbumCoverItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1)));

}
