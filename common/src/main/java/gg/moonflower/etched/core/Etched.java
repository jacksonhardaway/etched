package gg.moonflower.etched.core;

import gg.moonflower.etched.api.sound.download.SoundSourceManager;
import gg.moonflower.etched.client.render.entity.JukeboxMinecartRenderer;
import gg.moonflower.etched.client.render.item.AlbumCoverItemRenderer;
import gg.moonflower.etched.client.screen.*;
import gg.moonflower.etched.common.item.*;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.sound.download.BandcampSource;
import gg.moonflower.etched.common.sound.download.SoundCloudSource;
import gg.moonflower.etched.core.registry.*;
import gg.moonflower.pollen.api.config.ConfigManager;
import gg.moonflower.pollen.api.config.PollinatedConfigType;
import gg.moonflower.pollen.api.event.events.entity.ModifyTradesEvents;
import gg.moonflower.pollen.api.event.events.registry.client.RegisterAtlasSpriteEvent;
import gg.moonflower.pollen.api.platform.Platform;
import gg.moonflower.pollen.api.registry.client.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Supplier;

/**
 * @author Jackson
 * @author Ocelot
 */
public class Etched {

    public static final String MOD_ID = "etched";
    public static final EtchedConfig.Client CLIENT_CONFIG = ConfigManager.register(Etched.MOD_ID, PollinatedConfigType.CLIENT, EtchedConfig.Client::new);
    public static final Platform PLATFORM = Platform.builder(Etched.MOD_ID)
            .commonInit(Etched::commonInit)
            .commonPostInit(Etched::commonPostInit)
            .clientInit(Etched::clientInit)
            .clientPostInit(Etched::clientPostInit)
            .build();

    public static void commonInit() {
        EtchedBlocks.BLOCKS.register(Etched.PLATFORM);
        EtchedBlocks.BLOCK_ENTITIES.register(Etched.PLATFORM);
        EtchedItems.ITEMS.register(Etched.PLATFORM);
        EtchedEntities.ENTITIES.register(Etched.PLATFORM);
        EtchedMenus.MENUS.register(Etched.PLATFORM);
        EtchedSounds.SOUNDS.register(Etched.PLATFORM);
        EtchedVillagers.POI_TYPES.register(Etched.PLATFORM);
        EtchedVillagers.PROFESSIONS.register(Etched.PLATFORM);
        EtchedRecipes.RECIPES.register(Etched.PLATFORM);

        EtchedMessages.init();

        ModifyTradesEvents.VILLAGER.register(context -> {
            if (context.getProfession() != EtchedVillagers.BARD.get())
                return;

            ModifyTradesEvents.TradeRegistry tier1 = context.getTrades(1);
            tier1.add(Items.MUSIC_DISC_13, 8, 1, 4, 20, true);
            tier1.add(Items.MUSIC_DISC_11, 8, 1, 4, 20, true);
            tier1.add(Items.MUSIC_DISC_CAT, 8, 1, 4, 20, true);
            tier1.add(Items.NOTE_BLOCK, 1, 2, 16, 2, true);
            tier1.add(EtchedItems.MUSIC_LABEL, 4, 2, 16, 1, false);

            ModifyTradesEvents.TradeRegistry tier2 = context.getTrades(2);
            tier2.add(EtchedItems.BLANK_MUSIC_DISC, 28, 2, 12, 15, false);
            tier2.add(EtchedBlocks.ETCHING_TABLE, 32, 1, 8, 15, false);

            ModifyTradesEvents.TradeRegistry tier3 = context.getTrades(3);
            tier3.add(Blocks.CLAY, 6, 1, 16, 2, false);
            tier3.add(Blocks.HAY_BLOCK, 12, 1, 8, 2, false);
            tier3.add(Blocks.WHITE_WOOL, 8, 1, 32, 4, false);
            tier3.add(Blocks.BONE_BLOCK, 24, 1, 8, 4, false);
            tier3.add(Blocks.PACKED_ICE, 36, 1, 4, 8, false);
            tier3.add(Blocks.GOLD_BLOCK, 48, 1, 2, 10, false);

            ModifyTradesEvents.TradeRegistry tier4 = context.getTrades(4);
            tier3.add(Items.JUKEBOX, 26, 1, 4, 30, false);
            tier4.add(EtchedItems.JUKEBOX_MINECART, 28, 1, 4, 30, false);
            tier4.add(EtchedBlocks.ALBUM_JUKEBOX, 30, 1, 4, 30, false);

            ModifyTradesEvents.TradeRegistry tier5 = context.getTrades(5);
            tier5.add(Items.DIAMOND, 8, 1, 8, 40, true);
            for (Item item : ItemTags.MUSIC_DISCS.getValues())
                tier5.add(item, 10, 1, 4, 40, true);
        });
    }

    public static void clientInit() {
        RegisterAtlasSpriteEvent.event(InventoryMenu.BLOCK_ATLAS).register((atlas, registry) -> {
            registry.accept(new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_disc"));
            registry.accept(new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_label"));
        });

        ClientLoading.load(); // stops server from crashing due to client loading
        ModelRegistry.registerFactory((resourceManager, out) -> {
            String folder = "models/item/" + AlbumCoverItemRenderer.FOLDER_NAME + "/";
            for (ResourceLocation animationLocation : resourceManager.listResources(folder, name -> name.endsWith(".json")))
                out.accept(new ModelResourceLocation(new ResourceLocation(animationLocation.getNamespace(), animationLocation.getPath().substring(12, animationLocation.getPath().length() - 5)), "inventory"));
        });

        ColorRegistry.register((stack, index) -> index == 0 || index == 1 ? MusicLabelItem.getLabelColor(stack) : -1, EtchedItems.MUSIC_LABEL);
        ColorRegistry.register((stack, index) -> index == 0 ? ComplexMusicLabelItem.getPrimaryColor(stack) : index == 1 ? ComplexMusicLabelItem.getSecondaryColor(stack) : -1, EtchedItems.COMPLEX_MUSIC_LABEL);

        ColorRegistry.register((stack, index) -> index > 0 ? -1 : ((BlankMusicDiscItem) stack.getItem()).getColor(stack), EtchedItems.BLANK_MUSIC_DISC);
        ColorRegistry.register((stack, index) -> index == 0 ? EtchedMusicDiscItem.getDiscColor(stack) : EtchedMusicDiscItem.getPattern(stack).isColorable() ? index == 1 ? EtchedMusicDiscItem.getLabelPrimaryColor(stack) : index == 2 ? EtchedMusicDiscItem.getLabelSecondaryColor(stack) : -1 : -1, EtchedItems.ETCHED_MUSIC_DISC);

        AlbumCoverItemRenderer.init();
    }

    public static void commonPostInit(Platform.ModSetupContext ctx) {
        SoundSourceManager.registerSource(new SoundCloudSource());
        SoundSourceManager.registerSource(new BandcampSource());
        ctx.enqueueWork(EtchedVillagers::registerVillages);
    }

    public static void clientPostInit(Platform.ModSetupContext ctx) {
        ctx.enqueueWork(() -> {
            ScreenRegistry.register(EtchedMenus.ETCHING_MENU.get(), EtchingScreen::new);
            ScreenRegistry.register(EtchedMenus.ALBUM_JUKEBOX_MENU.get(), AlbumJukeboxScreen::new);
            ScreenRegistry.register(EtchedMenus.BOOMBOX_MENU.get(), BoomboxScreen::new);
            ScreenRegistry.register(EtchedMenus.ALBUM_COVER_MENU.get(), AlbumCoverScreen::new);
            ScreenRegistry.register(EtchedMenus.RADIO_MENU.get(), RadioScreen::new);
            ItemPredicateRegistry.register(EtchedItems.BOOMBOX.get(), new ResourceLocation(Etched.MOD_ID, "playing"), (stack, level, entity) -> {
                if (!(entity instanceof Player))
                    return 0;
                InteractionHand hand = BoomboxItem.getPlayingHand(entity);
                return hand != null && stack == entity.getItemInHand(hand) ? 1 : 0;
            });
            ItemPredicateRegistry.register(EtchedItems.ETCHED_MUSIC_DISC.get(), new ResourceLocation(Etched.MOD_ID, "pattern"), (stack, level, entity) -> Mth.clamp(EtchedMusicDiscItem.getPattern(stack).ordinal() / 10F, 0, 1));
        });
        RenderTypeRegistry.register(EtchedBlocks.ETCHING_TABLE.get(), RenderType.cutout());
        RenderTypeRegistry.register(EtchedBlocks.RADIO.get(), RenderType.cutout());
        EntityRendererRegistry.register(EtchedEntities.JUKEBOX_MINECART, JukeboxMinecartRenderer::new);
        ItemRendererRegistry.registerHandModel(EtchedItems.BOOMBOX.get(), new ModelResourceLocation(new ResourceLocation(Etched.MOD_ID, "boombox_in_hand"), "inventory"));
        ItemRendererRegistry.registerRenderer(EtchedItems.ALBUM_COVER.get(), AlbumCoverItemRenderer.INSTANCE);
    }

    private static class ClientLoading {
        private static void load() {
            ModelRegistry.registerSpecial(new ModelResourceLocation(new ResourceLocation(Etched.MOD_ID, "boombox_in_hand"), "inventory"));
        }
    }
}
