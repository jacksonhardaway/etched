package gg.moonflower.etched.core;

import gg.moonflower.etched.api.sound.download.SoundSourceManager;
import gg.moonflower.etched.client.render.EtchedModelLayers;
import gg.moonflower.etched.client.render.JukeboxMinecartRenderer;
import gg.moonflower.etched.client.render.item.AlbumCoverItemRenderer;
import gg.moonflower.etched.client.screen.*;
import gg.moonflower.etched.common.item.*;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.sound.download.BandcampSource;
import gg.moonflower.etched.common.sound.download.SoundCloudSource;
import gg.moonflower.etched.core.registry.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.MinecartModel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;

@Mod(Etched.MOD_ID)
public class Etched {

    public static final String MOD_ID = "etched";
    public static final EtchedConfig.Client CLIENT_CONFIG;
    public static final EtchedConfig.Server SERVER_CONFIG;
    private static final ForgeConfigSpec clientSpec;
    private static final ForgeConfigSpec serverSpec;

    static {
        Pair<EtchedConfig.Client, ForgeConfigSpec> clientConfig = new ForgeConfigSpec.Builder().configure(EtchedConfig.Client::new);
        clientSpec = clientConfig.getRight();
        CLIENT_CONFIG = clientConfig.getLeft();

        Pair<EtchedConfig.Server, ForgeConfigSpec> serverConfig = new ForgeConfigSpec.Builder().configure(EtchedConfig.Server::new);
        serverSpec = serverConfig.getRight();
        SERVER_CONFIG = serverConfig.getLeft();
    }

    public Etched() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(Etched::init);
        bus.addListener(Etched::clientInit);
        bus.addListener(Etched::registerReloadListeners);
        bus.addListener(Etched::registerItemGroups);
        bus.addListener(Etched::registerCustomModels);
        bus.addListener(Etched::registerEntityRenders);
        bus.addListener(Etched::registerEntityLayers);
        bus.addListener(Etched::registerItemColors);

        EtchedBlocks.BLOCKS.register(bus);
        EtchedBlocks.BLOCK_ENTITIES.register(bus);

        EtchedItems.REGISTRY.register(bus);
        EtchedEntities.REGISTRY.register(bus);
        EtchedMenus.REGISTRY.register(bus);
        EtchedSounds.REGISTRY.register(bus);
        EtchedRecipes.REGISTRY.register(bus);

        EtchedVillagers.POI_REGISTRY.register(bus);
        EtchedVillagers.PROFESSION_REGISTRY.register(bus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, clientSpec);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, serverSpec);
    }

    private static void init(FMLCommonSetupEvent event) {
        EtchedMessages.init();

        SoundSourceManager.registerSource(new SoundCloudSource());
        SoundSourceManager.registerSource(new BandcampSource());
    }

    private static void clientInit(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(EtchedItems.BOOMBOX.get(), new ResourceLocation(Etched.MOD_ID, "playing"), (stack, level, entity, i) -> {
                if (!(entity instanceof Player)) {
                    return 0;
                }
                InteractionHand hand = BoomboxItem.getPlayingHand(entity);
                return hand != null && stack == entity.getItemInHand(hand) ? 1 : 0;
            });
            ItemProperties.register(EtchedItems.ETCHED_MUSIC_DISC.get(), new ResourceLocation(Etched.MOD_ID, "pattern"), (stack, level, entity, i) -> EtchedMusicDiscItem.getPattern(stack).ordinal() / 10F);

//            ItemBlockRenderTypes.setRenderLayer(EtchedBlocks.ETCHING_TABLE.get(), ChunkRenderTypeSet.of(RenderType.cutout()));
//            ItemBlockRenderTypes.setRenderLayer(EtchedBlocks.RADIO.get(), ChunkRenderTypeSet.of(RenderType.cutout()));

//            ItemRendererRegistry.registerHandModel(EtchedItems.BOOMBOX.get(), new ModelResourceLocation(new ResourceLocation(Etched.MOD_ID, "boombox_in_hand"), "inventory"));
//            ItemRendererRegistry.registerRenderer(EtchedItems.ALBUM_COVER.get(), AlbumCoverItemRenderer.INSTANCE);

            MenuScreens.register(EtchedMenus.ETCHING_MENU.get(), EtchingScreen::new);
            MenuScreens.register(EtchedMenus.ALBUM_JUKEBOX_MENU.get(), AlbumJukeboxScreen::new);
            MenuScreens.register(EtchedMenus.BOOMBOX_MENU.get(), BoomboxScreen::new);
            MenuScreens.register(EtchedMenus.ALBUM_COVER_MENU.get(), AlbumCoverScreen::new);
            MenuScreens.register(EtchedMenus.RADIO_MENU.get(), RadioScreen::new);
        });
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(AlbumCoverItemRenderer.INSTANCE);
    }

    private static void registerItemGroups(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> tab = event.getTabKey();
        if (tab == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(EtchedItems.MUSIC_LABEL);
            event.accept(EtchedItems.BLANK_MUSIC_DISC);
            event.accept(EtchedItems.BOOMBOX);
            event.accept(EtchedItems.ALBUM_COVER);
        } else if (tab == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(EtchedItems.JUKEBOX_MINECART);
        } else if (tab == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(EtchedBlocks.ETCHING_TABLE);
            event.accept(EtchedBlocks.ALBUM_JUKEBOX);
            event.accept(EtchedBlocks.RADIO);
        }
    }

    private static void registerCustomModels(ModelEvent.RegisterAdditional event) {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        String folder = "models/item/" + AlbumCoverItemRenderer.FOLDER_NAME;
        event.register(new ModelResourceLocation(new ResourceLocation(Etched.MOD_ID, "boombox_in_hand"), "inventory"));
        for (ResourceLocation location : resourceManager.listResources(folder, name -> name.getPath().endsWith(".json")).keySet()) {
            event.register(new ModelResourceLocation(new ResourceLocation(location.getNamespace(), location.getPath().substring(12, location.getPath().length() - 5)), "inventory"));
        }
    }

    private static void registerEntityRenders(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EtchedEntities.JUKEBOX_MINECART.get(), JukeboxMinecartRenderer::new);
    }

    private static void registerEntityLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(EtchedModelLayers.JUKEBOX_MINECART, MinecartModel::createBodyLayer);
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, index) -> index == 0 || index == 1 ? MusicLabelItem.getLabelColor(stack) : -1, EtchedItems.MUSIC_LABEL.get());
        event.register((stack, index) -> index == 0 ? ComplexMusicLabelItem.getPrimaryColor(stack) : index == 1 ? ComplexMusicLabelItem.getSecondaryColor(stack) : -1, EtchedItems.COMPLEX_MUSIC_LABEL.get());

        event.register((stack, index) -> index > 0 ? -1 : ((BlankMusicDiscItem) stack.getItem()).getColor(stack), EtchedItems.BLANK_MUSIC_DISC.get());
        event.register((stack, index) -> {
            if (index == 0) {
                return EtchedMusicDiscItem.getDiscColor(stack);
            }
            if (EtchedMusicDiscItem.getPattern(stack).isColorable()) {
                if (index == 1) {
                    return EtchedMusicDiscItem.getLabelPrimaryColor(stack);
                }
                if (index == 2) {
                    return EtchedMusicDiscItem.getLabelSecondaryColor(stack);
                }
            }
            return -1;
        }, EtchedItems.ETCHED_MUSIC_DISC.get());
    }
}
