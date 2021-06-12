package me.jaackson.etched;

import me.jaackson.etched.bridge.NetworkBridge;
import me.jaackson.etched.bridge.RegistryBridge;
import me.jaackson.etched.client.screen.EtchingScreen;
import me.jaackson.etched.common.item.EtchedMusicDiscItem;
import me.jaackson.etched.common.network.ClientboundPlayMusicPacket;
import me.jaackson.etched.common.network.handler.EtchedClientPlayHandler;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeableLeatherItem;

/**
 * @author Jackson
 * @author Ocelot
 */
public class Etched {

    public static final String MOD_ID = "etched";

    public static void commonInit() {
        EtchedRegistry.register();
        NetworkBridge.registerPlayToClient(ClientboundPlayMusicPacket.CHANNEL, ClientboundPlayMusicPacket.class, ClientboundPlayMusicPacket::new, () -> EtchedClientPlayHandler::handlePlayMusicPacket);
    }

    public static void clientInit() {
        RegistryBridge.registerSprite(new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_disc"), InventoryMenu.BLOCK_ATLAS);
        RegistryBridge.registerSprite(new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_label"), InventoryMenu.BLOCK_ATLAS);
        RegistryBridge.registerItemColor((stack, index) -> index > 0 ? -1 : ((DyeableLeatherItem) stack.getItem()).getColor(stack), EtchedRegistry.BLANK_MUSIC_DISC, EtchedRegistry.MUSIC_LABEL);
        RegistryBridge.registerItemColor((stack, index) -> index == 0 ? EtchedMusicDiscItem.getPrimaryColor(stack) : index == 1 && EtchedMusicDiscItem.getPattern(stack).isColorable() ? EtchedMusicDiscItem.getSecondaryColor(stack) : -1, EtchedRegistry.ETCHED_MUSIC_DISC);
    }

    public static void commonPostInit() {
    }

    public static void clientPostInit() {
        RegistryBridge.registerScreenFactory(EtchedRegistry.ETCHING_MENU.get(), EtchingScreen::new);
        RegistryBridge.registerBlockRenderType(EtchedRegistry.ETCHING_TABLE.get(), RenderType.cutout());
        RegistryBridge.registerItemOverride(EtchedRegistry.ETCHED_MUSIC_DISC.get(), new ResourceLocation(Etched.MOD_ID, "pattern"), (stack, level, livingEntity) -> EtchedMusicDiscItem.getPattern(stack).ordinal());
    }
}
