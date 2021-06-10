package me.jaackson.etched;

import me.jaackson.etched.bridge.Platform;
import me.jaackson.etched.bridge.RegistryBridge;
import me.jaackson.etched.common.block.AlbumJukeboxBlock;
import me.jaackson.etched.common.block.EtcherBlock;
import me.jaackson.etched.common.item.BlankMusicDiscItem;
import me.jaackson.etched.common.item.EtchedMusicDiscItem;
import me.jaackson.etched.common.item.MusicLabelItem;
import me.shedaniel.architectury.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;

import java.util.function.Supplier;

public class EtchedRegistry {

    public static final Supplier<SoundEvent> UI_ETCHER_TAKE_RESULT = RegistryBridge.registerSound("ui.etcher.take_result", new SoundEvent(new ResourceLocation(Etched.MOD_ID, "ui.etcher.take_result")));

    public static final Supplier<Item> MUSIC_LABEL = RegistryBridge.registerItem("music_label", new MusicLabelItem(new Item.Properties().stacksTo(1).tab(CreativeModeTab.TAB_MISC)));
    public static final Supplier<Item> BLANK_MUSIC_DISC = RegistryBridge.registerItem("blank_music_disc", new BlankMusicDiscItem(new Item.Properties().stacksTo(1).tab(CreativeModeTab.TAB_MISC)));
    public static final Supplier<Item> ETCHED_MUSIC_DISC = RegistryBridge.registerItem("etched_music_disc", new EtchedMusicDiscItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<Block> ETCHER = RegistryBridge.registerBlock("etcher", new EtcherBlock(BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL)), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS));
    public static final Supplier<Block> ALBUM_JUKEBOX = RegistryBridge.registerBlock("album_jukebox", new AlbumJukeboxBlock(BlockBehaviour.Properties.copy(Blocks.JUKEBOX)), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS));

    @ExpectPlatform
    public static void register() {
        Platform.safeAssertionError();
    }
}
