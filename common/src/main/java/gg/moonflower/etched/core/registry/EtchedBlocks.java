package gg.moonflower.etched.core.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import gg.moonflower.etched.common.block.AlbumJukeboxBlock;
import gg.moonflower.etched.common.block.EtchingTableBlock;
import gg.moonflower.etched.common.block.RadioBlock;
import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import gg.moonflower.etched.common.blockentity.RadioBlockEntity;
import gg.moonflower.etched.common.item.PortalRadioItem;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.pollen.api.registry.wrapper.v1.PollinatedBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;

public class EtchedBlocks {

    public static final PollinatedBlockRegistry BLOCKS = PollinatedBlockRegistry.create(EtchedItems.REGISTRY);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Etched.MOD_ID, Registry.BLOCK_ENTITY_TYPE_REGISTRY);

    public static final RegistrySupplier<Block> ETCHING_TABLE = BLOCKS.registerWithItem("etching_table", () -> new EtchingTableBlock(BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS));
    public static final RegistrySupplier<Block> ALBUM_JUKEBOX = BLOCKS.registerWithItem("album_jukebox", () -> new AlbumJukeboxBlock(BlockBehaviour.Properties.copy(Blocks.JUKEBOX)), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS));
    public static final RegistrySupplier<Block> RADIO = BLOCKS.registerWithItem("radio", () -> new RadioBlock(BlockBehaviour.Properties.copy(Blocks.JUKEBOX).noOcclusion()), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS));
    public static final RegistrySupplier<Item> PORTAL_RADIO_ITEM = EtchedItems.REGISTRY.register("portal_radio", () -> new PortalRadioItem(RADIO.get(), new Item.Properties()));

    public static final RegistrySupplier<BlockEntityType<AlbumJukeboxBlockEntity>> ALBUM_JUKEBOX_BE = BLOCK_ENTITIES.register("album_jukebox", () -> BlockEntityType.Builder.of(AlbumJukeboxBlockEntity::new, ALBUM_JUKEBOX.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<RadioBlockEntity>> RADIO_BE = BLOCK_ENTITIES.register("radio", () -> BlockEntityType.Builder.of(RadioBlockEntity::new, RADIO.get()).build(null));

}
