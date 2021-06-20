package me.jaackson.etched;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.jaackson.etched.bridge.NetworkBridge;
import me.jaackson.etched.bridge.RegistryBridge;
import me.jaackson.etched.client.screen.AlbumJukeboxScreen;
import me.jaackson.etched.client.screen.EtchingScreen;
import me.jaackson.etched.common.item.EtchedMusicDiscItem;
import me.jaackson.etched.common.network.ClientboundAddMinecartJukeboxPacket;
import me.jaackson.etched.common.network.ClientboundInvalidEtchUrlPacket;
import me.jaackson.etched.common.network.ClientboundPlayMinecartJukeboxMusicPacket;
import me.jaackson.etched.common.network.ClientboundPlayMusicPacket;
import me.jaackson.etched.common.network.ServerboundSetEtchingUrlPacket;
import me.jaackson.etched.common.network.handler.EtchedClientPlayHandler;
import me.jaackson.etched.common.network.handler.EtchedServerPlayHandler;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * @author Jackson
 * @author Ocelot
 */
public class Etched {

    public static final String MOD_ID = "etched";

    public static void commonInit() {
        EtchedRegistry.register();
        RegistryBridge.registerVillagerTrades(EtchedRegistry.BARD, () -> Util.make(new Int2ObjectOpenHashMap<>(), map -> {
            map.put(1, new VillagerTrades.ItemListing[]{
                    new ItemTrade(() -> Items.MUSIC_DISC_13, 8, 1, 16, 2, true),
                    new ItemTrade(() -> Items.NOTE_BLOCK, 12, 2, 16, 2, true),
                    new ItemTrade(() -> Items.NOTE_BLOCK, 8, 1, 12, 1),
                    new ItemTrade(EtchedRegistry.MUSIC_LABEL, 4, 2, 12, 1),
            });

            map.put(2, new VillagerTrades.ItemListing[]{
                    new ItemTrade(EtchedRegistry.BLANK_MUSIC_DISC, 28, 2, 12, 5),
                    new ItemTrade(() -> EtchedRegistry.ETCHING_TABLE.get().asItem(), 32, 1, 12, 5),
            });

            map.put(3, new VillagerTrades.ItemListing[]{
                    new ItemTrade(() -> Items.JUKEBOX, 26, 1, 12, 10),
                    new ItemTrade(() -> Items.MUSIC_DISC_CAT, 24, 1, 16, 20, true)
            });

            List<VillagerTrades.ItemListing> tier4 = new ArrayList<>();
            for (Item item : ItemTags.MUSIC_DISCS.getValues())
                tier4.add(new ItemTrade(() -> item, 48, 1, 8, 30, true));
            tier4.add(new ItemTrade(() -> Items.JUKEBOX, 20, 1, 16, 30, true));
            tier4.add(new ItemTrade(EtchedRegistry.JUKEBOX_MINECART, 24, 1, 16, 30, true));
            tier4.add(new ItemTrade(() -> EtchedRegistry.ALBUM_JUKEBOX.get().asItem(), 30, 1, 12, 15));
            map.put(4, tier4.toArray(new VillagerTrades.ItemListing[0]));

            map.put(5, new VillagerTrades.ItemListing[]{
                    new ItemTrade(EtchedRegistry.BLANK_MUSIC_DISC, 14, 1, 16, 40, true),
                    new ItemTrade(EtchedRegistry.MUSIC_LABEL, 1, 1, 16, 40, true),
                    new ItemTrade(() -> EtchedRegistry.ETCHING_TABLE.get().asItem(), 22, 1, 16, 40, true)
            });
        }));
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

    public static void commonNetworkingInit() {
        NetworkBridge.registerPlayToServer(ServerboundSetEtchingUrlPacket.CHANNEL, ServerboundSetEtchingUrlPacket.class, ServerboundSetEtchingUrlPacket::new, EtchedServerPlayHandler::handleSetEtcherUrl);
    }

    public static void clientNetworkingInit() {
        NetworkBridge.registerPlayToClient(ClientboundPlayMusicPacket.CHANNEL, ClientboundPlayMusicPacket.class, ClientboundPlayMusicPacket::new, () -> EtchedClientPlayHandler::handlePlayMusicPacket);
        NetworkBridge.registerPlayToClient(ClientboundAddMinecartJukeboxPacket.CHANNEL, ClientboundAddMinecartJukeboxPacket.class, ClientboundAddMinecartJukeboxPacket::new, () -> EtchedClientPlayHandler::handleAddMinecartJukeboxPacket);
        NetworkBridge.registerPlayToClient(ClientboundPlayMinecartJukeboxMusicPacket.CHANNEL, ClientboundPlayMinecartJukeboxMusicPacket.class, ClientboundPlayMinecartJukeboxMusicPacket::new, () -> EtchedClientPlayHandler::handlePlayMinecartJukeboxPacket);
        NetworkBridge.registerPlayToClient(ClientboundInvalidEtchUrlPacket.CHANNEL, ClientboundInvalidEtchUrlPacket.class, ClientboundInvalidEtchUrlPacket::new, () -> EtchedClientPlayHandler::handleSetInvalidEtch);
    }

    static class ItemTrade implements VillagerTrades.ItemListing {
        private final Supplier<Item> item;
        private final int emeralds;
        private final int itemCount;
        private final int maxUses;
        private final int xpGain;
        private final float priceMultiplier;
        private final boolean sellToVillager;

        public ItemTrade(Supplier<Item> Item, int emeralds, int itemCount, int maxUses, int xpGain) {
            this(Item, emeralds, itemCount, maxUses, xpGain, 0.05F, false);
        }

        public ItemTrade(Supplier<Item> Item, int emeralds, int itemCount, int maxUses, int xpGain, boolean sellToVillager) {
            this(Item, emeralds, itemCount, maxUses, xpGain, 0.05F, sellToVillager);
        }

        public ItemTrade(Supplier<Item> Item, int emeralds, int itemCount, int maxUses, int xpGain, float priceMultiplier, boolean sellToVillager) {
            this.item = Item;
            this.emeralds = emeralds;
            this.itemCount = itemCount;
            this.maxUses = maxUses;
            this.xpGain = xpGain;
            this.priceMultiplier = priceMultiplier;
            this.sellToVillager = sellToVillager;
        }

        public MerchantOffer getOffer(Entity entity, Random random) {
            ItemStack emeralds = new ItemStack(Items.EMERALD, this.emeralds);
            ItemStack item = new ItemStack(this.item.get(), this.itemCount);

            return new MerchantOffer(this.sellToVillager ? item : emeralds, this.sellToVillager ? emeralds : item, this.maxUses, this.xpGain, this.priceMultiplier);
        }
    }
}
