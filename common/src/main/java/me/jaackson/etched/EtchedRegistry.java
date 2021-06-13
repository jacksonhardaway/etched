package me.jaackson.etched;

import me.jaackson.etched.bridge.Platform;
import me.jaackson.etched.bridge.RegistryBridge;
import me.jaackson.etched.common.block.AlbumJukeboxBlock;
import me.jaackson.etched.common.block.EtchingTableBlock;
import me.jaackson.etched.common.blockentity.AlbumJukeboxBlockEntity;
import me.jaackson.etched.common.entity.MinecartJukebox;
import me.jaackson.etched.common.item.BlankMusicDiscItem;
import me.jaackson.etched.common.item.EtchedMusicDiscItem;
import me.jaackson.etched.common.item.MinecartJukeboxItem;
import me.jaackson.etched.common.item.MusicLabelItem;
import me.jaackson.etched.common.menu.AlbumJukeboxMenu;
import me.jaackson.etched.common.menu.EtchingMenu;
import me.shedaniel.architectury.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;

import java.util.function.Supplier;

public class EtchedRegistry {

    public static final Supplier<SoundEvent> UI_ETCHER_TAKE_RESULT = RegistryBridge.registerSound("ui.etching_table.take_result", () -> new SoundEvent(new ResourceLocation(Etched.MOD_ID, "ui.etching_table.take_result")));

    public static final Supplier<MenuType<EtchingMenu>> ETCHING_MENU = RegistryBridge.registerMenu("etching_table", EtchingMenu::new);
    public static final Supplier<MenuType<AlbumJukeboxMenu>> ALBUM_JUKEBOX_MENU = RegistryBridge.registerMenu("album_jukebox", AlbumJukeboxMenu::new);

    public static final Supplier<Item> MUSIC_LABEL = RegistryBridge.registerItem("music_label", () -> new MusicLabelItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC)));
    public static final Supplier<Item> BLANK_MUSIC_DISC = RegistryBridge.registerItem("blank_music_disc", () -> new BlankMusicDiscItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC)));
    public static final Supplier<Item> ETCHED_MUSIC_DISC = RegistryBridge.registerItem("etched_music_disc", () -> new EtchedMusicDiscItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> JUKEBOX_MINECART = RegistryBridge.registerItem("jukebox_minecart", () -> new MinecartJukeboxItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final Supplier<Block> ETCHING_TABLE = RegistryBridge.registerBlock("etching_table", () -> new EtchingTableBlock(BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS));
    public static final Supplier<Block> ALBUM_JUKEBOX = RegistryBridge.registerBlock("album_jukebox", () -> new AlbumJukeboxBlock(BlockBehaviour.Properties.copy(Blocks.JUKEBOX)), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS));

    public static final Supplier<BlockEntityType<AlbumJukeboxBlockEntity>> ALBUM_JUKEBOX_BE = RegistryBridge.registerBlockEntity("album_jukebox", () -> BlockEntityType.Builder.of(AlbumJukeboxBlockEntity::new, ALBUM_JUKEBOX.get()));

    public static final Supplier<EntityType<MinecartJukebox>> JUKEBOX_MINECART_ENTITY = RegistryBridge.registerEntity("minecart_jukebox", () -> EntityType.Builder.<MinecartJukebox>of(MinecartJukebox::new, MobCategory.MISC).sized(0.98F, 0.7F).clientTrackingRange(8));

    public static final Supplier<PoiType> ETCHING_TABLE_POI = RegistryBridge.registerPOI("etching_table", ETCHING_TABLE, 1, 1);
    public static final Supplier<VillagerProfession> BARD = RegistryBridge.registerProfession("bard", ETCHING_TABLE_POI, UI_ETCHER_TAKE_RESULT);

    @ExpectPlatform
    public static void register() {
        Platform.safeAssertionError();
    }
}
