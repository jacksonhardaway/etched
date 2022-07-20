package gg.moonflower.etched.core.registry;

import gg.moonflower.etched.common.block.AlbumJukeboxBlock;
import gg.moonflower.etched.common.block.EtchingTableBlock;
import gg.moonflower.etched.common.block.RadioBlock;
import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import gg.moonflower.etched.common.blockentity.RadioBlockEntity;
import gg.moonflower.etched.common.item.PortalRadioItem;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.pollen.api.registry.PollinatedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;

import java.util.function.Function;
import java.util.function.Supplier;

public class EtchedBlocks {

    public static final PollinatedRegistry<Block> BLOCKS = PollinatedRegistry.create(Registry.BLOCK, Etched.MOD_ID);
    public static final PollinatedRegistry<BlockEntityType<?>> BLOCK_ENTITIES = PollinatedRegistry.create(Registry.BLOCK_ENTITY_TYPE, Etched.MOD_ID);

    public static final Supplier<Block> ETCHING_TABLE = registerBlock("etching_table", () -> new EtchingTableBlock(BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS));
    public static final Supplier<Block> ALBUM_JUKEBOX = registerBlock("album_jukebox", () -> new AlbumJukeboxBlock(BlockBehaviour.Properties.copy(Blocks.JUKEBOX)), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS));
    public static final Supplier<Block> RADIO = registerBlock("radio", () -> new RadioBlock(BlockBehaviour.Properties.copy(Blocks.JUKEBOX)), block -> {
        EtchedItems.ITEMS.register("portal_radio", () -> new PortalRadioItem(block, new Item.Properties()));
        return new BlockItem(block, new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS));
    });

    public static final Supplier<BlockEntityType<AlbumJukeboxBlockEntity>> ALBUM_JUKEBOX_BE = BLOCK_ENTITIES.register("album_jukebox", () -> BlockEntityType.Builder.of(AlbumJukeboxBlockEntity::new, ALBUM_JUKEBOX.get()).build(null));
    public static final Supplier<BlockEntityType<RadioBlockEntity>> RADIO_BE = BLOCK_ENTITIES.register("radio", () -> BlockEntityType.Builder.of(RadioBlockEntity::new, RADIO.get()).build(null));

    private static Supplier<Block> registerBlock(String id, Supplier<Block> block, Item.Properties properties) {
        return registerBlock(id, block, b -> new BlockItem(b, properties));
    }

    private static Supplier<Block> registerBlock(String id, Supplier<Block> block, Function<Block, Item> itemFactory) {
        Supplier<Block> register = BLOCKS.register(id, block);
        EtchedItems.ITEMS.register(id, () -> itemFactory.apply(register.get()));
        return register;
    }
}
