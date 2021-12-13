package gg.moonflower.etched.core;

import gg.moonflower.etched.client.screen.AlbumJukeboxScreen;
import gg.moonflower.etched.client.screen.EtchingScreen;
import gg.moonflower.etched.common.item.EtchedMusicDiscItem;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.core.registry.*;
import gg.moonflower.pollen.api.event.events.registry.RegisterAtlasSpriteEvent;
import gg.moonflower.pollen.api.platform.Platform;
import gg.moonflower.pollen.api.registry.ClientRegistries;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * @author Jackson
 * @author Ocelot
 */
public class Etched {

    public static final String MOD_ID = "etched";
    public static final Platform PLATFORM = Platform.builder(Etched.MOD_ID)
            .commonInit(Etched::commonInit)
            .commonPostInit(Etched::commonPostInit)
            .clientInit(Etched::clientInit)
            .clientPostInit(Etched::clientPostInit)
            .build();

    public static void commonInit() {
        EtchedBlocks.BLOCKS.register(Etched.PLATFORM);
        EtchedItems.ITEMS.register(Etched.PLATFORM);
        EtchedMenus.MENUS.register(Etched.PLATFORM);
        EtchedSounds.SOUNDS.register(Etched.PLATFORM);
        EtchedVillagers.POI_TYPES.register(Etched.PLATFORM);
        EtchedVillagers.PROFESSIONS.register(Etched.PLATFORM);

        EtchedMessages.init();

        // TODO: Reimplement villager trades
//        RegistryBridge.registerVillagerTrades(EtchedRegistry.BARD, () -> Util.make(new Int2ObjectOpenHashMap<>(), map -> {
//            map.put(1, new VillagerTrades.ItemListing[]{
//                    new ItemTrade(() -> Items.MUSIC_DISC_13, 8, 1, 16, 2, true),
//                    new ItemTrade(() -> Items.NOTE_BLOCK, 12, 2, 16, 2, true),
//                    new ItemTrade(() -> Items.NOTE_BLOCK, 8, 1, 12, 1),
//                    new ItemTrade(EtchedRegistry.MUSIC_LABEL, 4, 2, 12, 1),
//            });
//
//            map.put(2, new VillagerTrades.ItemListing[]{
//                    new ItemTrade(EtchedRegistry.BLANK_MUSIC_DISC, 28, 2, 12, 5),
//                    new ItemTrade(() -> EtchedRegistry.ETCHING_TABLE.get().asItem(), 32, 1, 12, 5),
//            });
//
//            map.put(3, new VillagerTrades.ItemListing[]{
//                    new ItemTrade(() -> Items.JUKEBOX, 26, 1, 12, 10),
//                    new ItemTrade(() -> Items.MUSIC_DISC_CAT, 24, 1, 16, 20, true)
//            });
//
//            List<VillagerTrades.ItemListing> tier4 = new ArrayList<>();
//            for (Item item : ItemTags.MUSIC_DISCS.getValues())
//                tier4.add(new ItemTrade(() -> item, 48, 1, 8, 30, true));
//            tier4.add(new ItemTrade(() -> Items.JUKEBOX, 20, 1, 16, 30, true));
//            tier4.add(new ItemTrade(EtchedRegistry.JUKEBOX_MINECART, 24, 1, 16, 30, true));
//            tier4.add(new ItemTrade(() -> EtchedRegistry.ALBUM_JUKEBOX.get().asItem(), 30, 1, 12, 15));
//            map.put(4, tier4.toArray(new VillagerTrades.ItemListing[0]));
//
//            map.put(5, new VillagerTrades.ItemListing[]{
//                    new ItemTrade(EtchedRegistry.BLANK_MUSIC_DISC, 14, 1, 16, 40, true),
//                    new ItemTrade(EtchedRegistry.MUSIC_LABEL, 1, 1, 16, 40, true),
//                    new ItemTrade(() -> EtchedRegistry.ETCHING_TABLE.get().asItem(), 22, 1, 16, 40, true)
//            });
//        }));
    }

    public static void clientInit() {
        RegisterAtlasSpriteEvent.event(InventoryMenu.BLOCK_ATLAS).register((atlas, registry) -> {
            registry.accept(new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_disc"));
            registry.accept(new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_label"));
        });

        // TODO: Reimplement item colors
//        ClientRegistries.registerItemColor((stack, index) -> index > 0 ? -1 : ((DyeableLeatherItem) stack.getItem()).getColor(stack), EtchedRegistry.BLANK_MUSIC_DISC, EtchedRegistry.MUSIC_LABEL);
//        ClientRegistries.registerItemColor((stack, index) -> index == 0 ? EtchedMusicDiscItem.getPrimaryColor(stack) : index == 1 && EtchedMusicDiscItem.getPattern(stack).isColorable() ? EtchedMusicDiscItem.getSecondaryColor(stack) : -1, EtchedRegistry.ETCHED_MUSIC_DISC);
    }

    public static void commonPostInit(Platform.ModSetupContext ctx) {
        ctx.enqueueWork(EtchedVillagers::registerVillages);
    }

    public static void clientPostInit(Platform.ModSetupContext ctx) {
        ClientRegistries.registerScreenFactory(EtchedMenus.ETCHING_MENU.get(), EtchingScreen::new);
        ClientRegistries.registerScreenFactory(EtchedMenus.ALBUM_JUKEBOX_MENU.get(), AlbumJukeboxScreen::new);
        ClientRegistries.setBlockRenderType(EtchedBlocks.ETCHING_TABLE.get(), RenderType.cutout());
        ClientRegistries.registerItemOverride(EtchedItems.ETCHED_MUSIC_DISC.get(), new ResourceLocation(Etched.MOD_ID, "pattern"), (stack, level, livingEntity, i) -> EtchedMusicDiscItem.getPattern(stack).ordinal());
        ClientRegistries.registerEntityRenderer(EtchedEntities.JUKEBOX_MINECART.get(), context -> new MinecartRenderer<>(context, ModelLayers.MINECART)); // TODO: custom model layer
    }


}
