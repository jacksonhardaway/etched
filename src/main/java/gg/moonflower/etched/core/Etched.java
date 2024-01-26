package gg.moonflower.etched.core;

import gg.moonflower.etched.api.sound.download.SoundSourceManager;
import gg.moonflower.etched.client.screen.*;
import gg.moonflower.etched.common.item.AlbumCoverItem;
import gg.moonflower.etched.common.item.BoomboxItem;
import gg.moonflower.etched.common.item.EtchedMusicDiscItem;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.sound.download.BandcampSource;
import gg.moonflower.etched.common.sound.download.SoundCloudSource;
import gg.moonflower.etched.core.registry.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.GrindstoneEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
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

        MinecraftForge.EVENT_BUS.addListener(Etched::onGrindstoneChange);
        MinecraftForge.EVENT_BUS.addListener(Etched::onItemChangedDimension);
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

    private static void onGrindstoneChange(GrindstoneEvent.OnPlaceItem event) {
        ItemStack top = event.getTopItem();
        ItemStack bottom = event.getBottomItem();

        if (top.isEmpty() == bottom.isEmpty()) {
            return;
        }

        ItemStack stack = top.isEmpty() ? bottom : top;
        if (AlbumCoverItem.getCoverStack(stack).isPresent()) {
            ItemStack result = stack.copy();
            result.setCount(1);
            AlbumCoverItem.setCover(result, ItemStack.EMPTY);
            event.setOutput(result);
        }
    }

    private static void onItemChangedDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ItemEntity entity) {
            if (event.getDimension() == Level.NETHER) {
                ItemStack oldStack = entity.getItem();
                if (oldStack.getItem() != EtchedBlocks.RADIO.get().asItem()) {
                    return;
                }

                ItemStack newStack = new ItemStack(EtchedBlocks.PORTAL_RADIO_ITEM.get(), oldStack.getCount());
                newStack.setTag(oldStack.getTag());
                entity.setItem(newStack);
            }
        }
    }
}
