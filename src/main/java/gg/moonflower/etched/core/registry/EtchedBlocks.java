package gg.moonflower.etched.core.registry;

import gg.moonflower.etched.common.block.AlbumJukeboxBlock;
import gg.moonflower.etched.common.block.EtchingTableBlock;
import gg.moonflower.etched.common.block.RadioBlock;
import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import gg.moonflower.etched.common.blockentity.RadioBlockEntity;
import gg.moonflower.etched.common.item.PortalRadioItem;
import gg.moonflower.etched.core.Etched;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;
import java.util.function.Supplier;

public class EtchedBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Etched.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Etched.MOD_ID);

    public static final RegistryObject<Block> ETCHING_TABLE = registerWithItem("etching_table", () -> new EtchingTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).strength(2.5F).sound(SoundType.WOOD)), new Item.Properties());
    public static final RegistryObject<Block> ALBUM_JUKEBOX = registerWithItem("album_jukebox", () -> new AlbumJukeboxBlock(BlockBehaviour.Properties.copy(Blocks.JUKEBOX)), new Item.Properties());
    public static final RegistryObject<Block> RADIO = registerWithItem("radio", () -> new RadioBlock(BlockBehaviour.Properties.copy(Blocks.JUKEBOX).noOcclusion()), new Item.Properties());
    public static final RegistryObject<Item> PORTAL_RADIO_ITEM = EtchedItems.REGISTRY.register("portal_radio", () -> new PortalRadioItem(RADIO.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<AlbumJukeboxBlockEntity>> ALBUM_JUKEBOX_BE = BLOCK_ENTITIES.register("album_jukebox", () -> BlockEntityType.Builder.of(AlbumJukeboxBlockEntity::new, ALBUM_JUKEBOX.get()).build(null));
    public static final RegistryObject<BlockEntityType<RadioBlockEntity>> RADIO_BE = BLOCK_ENTITIES.register("radio", () -> BlockEntityType.Builder.of(RadioBlockEntity::new, RADIO.get()).build(null));

    /**
     * Registers a block with a simple item.
     *
     * @param id         The id of the block
     * @param block      The block to register
     * @param properties The properties of the item to register
     * @param <R>        The type of block being registered
     * @return The registered block
     */
    private static <R extends Block> RegistryObject<R> registerWithItem(String id, Supplier<R> block, Item.Properties properties) {
        return registerWithItem(id, block, object -> new BlockItem(object, properties));
    }

    /**
     * Registers a block with an item.
     *
     * @param id          The id of the block
     * @param block       The block to register
     * @param itemFactory The factory to create a new item from the registered block
     * @param <R>         The type of block being registered
     * @return The registered block
     */
    private static <R extends Block> RegistryObject<R> registerWithItem(String id, Supplier<R> block, Function<R, Item> itemFactory) {
        RegistryObject<R> register = BLOCKS.register(id, block);
        EtchedItems.REGISTRY.register(id, () -> itemFactory.apply(register.get()));
        return register;
    }
}
