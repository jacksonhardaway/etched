package me.jaackson.etched;

import me.jaackson.etched.bridge.NetworkBridge;
import me.jaackson.etched.bridge.RegistryBridge;
import me.jaackson.etched.client.screen.AlbumJukeboxScreen;
import me.jaackson.etched.client.screen.EtchingScreen;
import me.jaackson.etched.common.item.EtchedMusicDiscItem;
import me.jaackson.etched.common.network.ClientboundAddMinecartJukeboxPacket;
import me.jaackson.etched.common.network.ClientboundPlayMinecartJukeboxMusicPacket;
import me.jaackson.etched.common.network.ClientboundPlayMusicPacket;
import me.jaackson.etched.common.network.ServerboundSetEtchingUrlPacket;
import me.jaackson.etched.common.network.handler.EtchedClientPlayHandler;
import me.jaackson.etched.common.network.handler.EtchedServerPlayHandler;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.MinecartRenderer;
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
        NetworkBridge.registerPlayToClient(ClientboundAddMinecartJukeboxPacket.CHANNEL, ClientboundAddMinecartJukeboxPacket.class, ClientboundAddMinecartJukeboxPacket::new, () -> EtchedClientPlayHandler::handleAddMinecartJukeboxPacket);
        NetworkBridge.registerPlayToClient(ClientboundPlayMinecartJukeboxMusicPacket.CHANNEL, ClientboundPlayMinecartJukeboxMusicPacket.class, ClientboundPlayMinecartJukeboxMusicPacket::new, () -> EtchedClientPlayHandler::handlePlayMinecartJukeboxPacket);
        NetworkBridge.registerPlayToServer(ServerboundSetEtchingUrlPacket.CHANNEL, ServerboundSetEtchingUrlPacket.class, ServerboundSetEtchingUrlPacket::new, EtchedServerPlayHandler::handleSetEtcherUrl);
    }

    public static void clientInit() {
        RegistryBridge.registerSprite(new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_disc"), InventoryMenu.BLOCK_ATLAS);
        RegistryBridge.registerSprite(new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_label"), InventoryMenu.BLOCK_ATLAS);
        RegistryBridge.registerItemColor((stack, index) -> index > 0 ? -1 : ((DyeableLeatherItem) stack.getItem()).getColor(stack), EtchedRegistry.BLANK_MUSIC_DISC, EtchedRegistry.MUSIC_LABEL);
        RegistryBridge.registerItemColor((stack, index) -> index == 0 ? EtchedMusicDiscItem.getPrimaryColor(stack) : index == 1 && EtchedMusicDiscItem.getPattern(stack).isColorable() ? EtchedMusicDiscItem.getSecondaryColor(stack) : -1, EtchedRegistry.ETCHED_MUSIC_DISC);
    }

    public static void commonPostInit() {
        EtchedRegistry.registerVillages();
    }

    public static void clientPostInit() {
        RegistryBridge.registerScreenFactory(EtchedRegistry.ETCHING_MENU.get(), EtchingScreen::new);
        RegistryBridge.registerScreenFactory(EtchedRegistry.ALBUM_JUKEBOX_MENU.get(), AlbumJukeboxScreen::new);
        RegistryBridge.registerBlockRenderType(EtchedRegistry.ETCHING_TABLE.get(), RenderType.cutout());
        RegistryBridge.registerItemOverride(EtchedRegistry.ETCHED_MUSIC_DISC.get(), new ResourceLocation(Etched.MOD_ID, "pattern"), (stack, level, livingEntity) -> EtchedMusicDiscItem.getPattern(stack).ordinal());
        RegistryBridge.registerEntityRenderer(EtchedRegistry.JUKEBOX_MINECART_ENTITY.get(), MinecartRenderer::new);
    }
}
