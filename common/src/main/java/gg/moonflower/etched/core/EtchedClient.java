package gg.moonflower.etched.core;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import gg.moonflower.etched.client.render.entity.JukeboxMinecartRenderer;
import gg.moonflower.etched.client.render.item.AlbumCoverItemRenderer;
import gg.moonflower.etched.client.render.model.EtchedModelLayers;
import gg.moonflower.etched.client.screen.*;
import gg.moonflower.etched.common.item.*;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandlerImpl;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import gg.moonflower.etched.core.registry.EtchedEntities;
import gg.moonflower.etched.core.registry.EtchedItems;
import gg.moonflower.etched.core.registry.EtchedMenus;
import gg.moonflower.pollen.api.event.registry.v1.RegisterAtlasSpriteEvent;
import gg.moonflower.pollen.api.registry.render.v1.ItemRendererRegistry;
import gg.moonflower.pollen.api.registry.render.v1.ModelRegistry;
import net.minecraft.client.model.MinecartModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;

public class EtchedClient {

    public static void init() {
        RegisterAtlasSpriteEvent.event(InventoryMenu.BLOCK_ATLAS).register((atlas, registry) -> {
            registry.accept(new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_disc"));
            registry.accept(new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_label"));
        });

        ModelRegistry.registerSpecial(new ModelResourceLocation(new ResourceLocation(Etched.MOD_ID, "boombox_in_hand"), "inventory"));
        ModelRegistry.registerFactory((resourceManager, out) -> {
            String folder = "models/item/" + AlbumCoverItemRenderer.FOLDER_NAME + "/";
            for (ResourceLocation animationLocation : resourceManager.listResources(folder, name -> name.getPath().endsWith(".json")).keySet()) {
                out.accept(new ModelResourceLocation(new ResourceLocation(animationLocation.getNamespace(), animationLocation.getPath().substring(12, animationLocation.getPath().length() - 5)), "inventory"));
            }
        });

        ColorHandlerRegistry.registerItemColors((stack, index) -> index == 0 || index == 1 ? MusicLabelItem.getLabelColor(stack) : -1, EtchedItems.MUSIC_LABEL);
        ColorHandlerRegistry.registerItemColors((stack, index) -> index == 0 ? ComplexMusicLabelItem.getPrimaryColor(stack) : index == 1 ? ComplexMusicLabelItem.getSecondaryColor(stack) : -1, EtchedItems.COMPLEX_MUSIC_LABEL);

        ColorHandlerRegistry.registerItemColors((stack, index) -> index > 0 ? -1 : ((BlankMusicDiscItem) stack.getItem()).getColor(stack), EtchedItems.BLANK_MUSIC_DISC);
        ColorHandlerRegistry.registerItemColors((stack, index) -> index == 0 ? EtchedMusicDiscItem.getDiscColor(stack) : EtchedMusicDiscItem.getPattern(stack).isColorable() ? index == 1 ? EtchedMusicDiscItem.getLabelPrimaryColor(stack) : index == 2 ? EtchedMusicDiscItem.getLabelSecondaryColor(stack) : -1 : -1, EtchedItems.ETCHED_MUSIC_DISC);

        EntityModelLayerRegistry.register(EtchedModelLayers.JUKEBOX_MINECART, MinecartModel::createBodyLayer);
        EntityRendererRegistry.register(EtchedEntities.JUKEBOX_MINECART, JukeboxMinecartRenderer::new);
        AlbumCoverItemRenderer.init();
    }

    public static void postInit() {
        MenuRegistry.registerScreenFactory(EtchedMenus.ETCHING_MENU.get(), EtchingScreen::new);
        MenuRegistry.registerScreenFactory(EtchedMenus.ALBUM_JUKEBOX_MENU.get(), AlbumJukeboxScreen::new);
        MenuRegistry.registerScreenFactory(EtchedMenus.BOOMBOX_MENU.get(), BoomboxScreen::new);
        MenuRegistry.registerScreenFactory(EtchedMenus.ALBUM_COVER_MENU.get(), AlbumCoverScreen::new);
        MenuRegistry.registerScreenFactory(EtchedMenus.RADIO_MENU.get(), RadioScreen::new);

        ItemPropertiesRegistry.register(EtchedItems.BOOMBOX.get(), new ResourceLocation(Etched.MOD_ID, "playing"), (stack, level, entity, i) -> {
            if (!(entity instanceof Player))
                return 0;
            InteractionHand hand = BoomboxItem.getPlayingHand(entity);
            return hand != null && stack == entity.getItemInHand(hand) ? 1 : 0;
        });
        ItemPropertiesRegistry.register(EtchedItems.ETCHED_MUSIC_DISC.get(), new ResourceLocation(Etched.MOD_ID, "pattern"), (stack, level, entity, i) -> EtchedMusicDiscItem.getPattern(stack).ordinal() / 10F);

        RenderTypeRegistry.register(RenderType.cutout(), EtchedBlocks.ETCHING_TABLE.get(), EtchedBlocks.RADIO.get());

        ItemRendererRegistry.registerHandModel(EtchedItems.BOOMBOX.get(), new ModelResourceLocation(new ResourceLocation(Etched.MOD_ID, "boombox_in_hand"), "inventory"));
        ItemRendererRegistry.registerRenderer(EtchedItems.ALBUM_COVER.get(), AlbumCoverItemRenderer.INSTANCE);

        EtchedMessages.PLAY.setClientHandler(new EtchedClientPlayPacketHandlerImpl());
    }
}
