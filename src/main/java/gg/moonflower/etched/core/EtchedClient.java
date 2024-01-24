package gg.moonflower.etched.core;

public class EtchedClient {

    public static void init() {
//        RegisterAtlasSpriteEvent.event(InventoryMenu.BLOCK_ATLAS).register((atlas, registry) -> {
//            registry.accept(new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_disc"));
//            registry.accept(new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_label"));
//        });

//        ModelRegistry.registerSpecial(new ModelResourceLocation(new ResourceLocation(Etched.MOD_ID, "boombox_in_hand"), "inventory"));
//        ModelRegistry.registerFactory((resourceManager, out) -> {
//            String folder = "models/item/" + AlbumCoverItemRenderer.FOLDER_NAME + "/";
//            for (ResourceLocation animationLocation : resourceManager.listResources(folder, name -> name.getPath().endsWith(".json")).keySet()) {
//                out.accept(new ModelResourceLocation(new ResourceLocation(animationLocation.getNamespace(), animationLocation.getPath().substring(12, animationLocation.getPath().length() - 5)), "inventory"));
//            }
//        });

//        ColorHandlerRegistry.registerItemColors((stack, index) -> index == 0 || index == 1 ? MusicLabelItem.getLabelColor(stack) : -1, EtchedItems.MUSIC_LABEL);
//        ColorHandlerRegistry.registerItemColors((stack, index) -> index == 0 ? ComplexMusicLabelItem.getPrimaryColor(stack) : index == 1 ? ComplexMusicLabelItem.getSecondaryColor(stack) : -1, EtchedItems.COMPLEX_MUSIC_LABEL);
//
//        ColorHandlerRegistry.registerItemColors((stack, index) -> index > 0 ? -1 : ((BlankMusicDiscItem) stack.getItem()).getColor(stack), EtchedItems.BLANK_MUSIC_DISC);
//        ColorHandlerRegistry.registerItemColors((stack, index) -> index == 0 ? EtchedMusicDiscItem.getDiscColor(stack) : EtchedMusicDiscItem.getPattern(stack).isColorable() ? index == 1 ? EtchedMusicDiscItem.getLabelPrimaryColor(stack) : index == 2 ? EtchedMusicDiscItem.getLabelSecondaryColor(stack) : -1 : -1, EtchedItems.ETCHED_MUSIC_DISC);

//        EntityModelLayerRegistry.register(EtchedModelLayers.JUKEBOX_MINECART, MinecartModel::createBodyLayer);
//        EntityRendererRegistry.register(EtchedEntities.JUKEBOX_MINECART, JukeboxMinecartRenderer::new);
//        AlbumCoverItemRenderer.init();
    }

    public static void postInit() {
//        MenuScreens.register(EtchedMenus.ETCHING_MENU.get(), EtchingScreen::new);
//        MenuScreens.register(EtchedMenus.ALBUM_JUKEBOX_MENU.get(), AlbumJukeboxScreen::new);
//        MenuScreens.register(EtchedMenus.BOOMBOX_MENU.get(), BoomboxScreen::new);
//        MenuScreens.register(EtchedMenus.ALBUM_COVER_MENU.get(), AlbumCoverScreen::new);
//        MenuScreens.register(EtchedMenus.RADIO_MENU.get(), RadioScreen::new);

//        ItemPropertiesRegistry.register(EtchedItems.BOOMBOX.get(), new ResourceLocation(Etched.MOD_ID, "playing"), (stack, level, entity, i) -> {
//            if (!(entity instanceof Player))
//                return 0;
//            InteractionHand hand = BoomboxItem.getPlayingHand(entity);
//            return hand != null && stack == entity.getItemInHand(hand) ? 1 : 0;
//        });
//        ItemPropertiesRegistry.register(EtchedItems.ETCHED_MUSIC_DISC.get(), new ResourceLocation(Etched.MOD_ID, "pattern"), (stack, level, entity, i) -> EtchedMusicDiscItem.getPattern(stack).ordinal() / 10F);

//        RenderTypeRegistry.register(RenderType.cutout(), EtchedBlocks.ETCHING_TABLE.get(), EtchedBlocks.RADIO.get());

//        ItemRendererRegistry.registerHandModel(EtchedItems.BOOMBOX.get(), new ModelResourceLocation(new ResourceLocation(Etched.MOD_ID, "boombox_in_hand"), "inventory"));
//        ItemRendererRegistry.registerRenderer(EtchedItems.ALBUM_COVER.get(), AlbumCoverItemRenderer.INSTANCE);

//        EtchedMessages.PLAY.setClientHandler(new EtchedClientPlayPacketHandler());
    }
}
