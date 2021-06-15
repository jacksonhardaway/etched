package me.jaackson.etched.bridge.forge;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.jaackson.etched.Etched;
import me.jaackson.etched.bridge.RegistryBridge;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Jackson
 */
public class RegistryBridgeImpl {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Etched.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Etched.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Etched.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, Etched.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, Etched.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.CONTAINERS, Etched.MOD_ID);
    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS = DeferredRegister.create(ForgeRegistries.PROFESSIONS, Etched.MOD_ID);
    public static final DeferredRegister<PoiType> POINT_OF_INTEREST_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, Etched.MOD_ID);

    public static <T extends SoundEvent> Supplier<T> registerSound(String name, Supplier<T> object) {
        return SOUND_EVENTS.register(name, object);
    }

    public static <T extends Item> Supplier<T> registerItem(String name, Supplier<T> object) {
        return ITEMS.register(name, object);
    }

    public static <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> object) {
        return BLOCKS.register(name, object);
    }

    public static <B extends BlockEntity, T extends BlockEntityType.Builder<B>> Supplier<BlockEntityType<B>> registerBlockEntity(String name, Supplier<T> object) {
        return BLOCK_ENTITIES.register(name, () -> object.get().build(null));
    }

    public static <E extends Entity, T extends EntityType.Builder<E>> Supplier<EntityType<E>> registerEntity(String name, Supplier<T> object) {
        return ENTITIES.register(name, () -> object.get().build(Etched.MOD_ID + ":" + name));
    }

    public static <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenu(String name, RegistryBridge.MenuFactory<T> object) {
        return MENU_TYPES.register(name, () -> new MenuType<>(object::create));
    }

    public static Supplier<VillagerProfession> registerProfession(String name, Supplier<PoiType> poiType, @Nullable Supplier<SoundEvent> workSound) {
        return VILLAGER_PROFESSIONS.register(name, () -> new VillagerProfession(Etched.MOD_ID + ":" + name, poiType.get(), ImmutableSet.of(), ImmutableSet.of(), workSound != null ? workSound.get() : null));
    }

    public static Supplier<PoiType> registerPOI(String name, Supplier<Block> block, int maxTickets, int validRange) {
        return POINT_OF_INTEREST_TYPES.register(name, () -> new PoiType(Etched.MOD_ID + ":" + name, PoiType.getBlockStates(block.get()), maxTickets, validRange));
    }

    public static void registerVillagerTrades(Supplier<VillagerProfession> prof, Supplier<Int2ObjectMap<VillagerTrades.ItemListing[]>> listings) {
        MinecraftForge.EVENT_BUS.<VillagerTradesEvent>addListener(e -> {
            if (e.getType() == prof.get())
                listings.get().forEach((tier, listing) -> e.getTrades().put(tier.intValue(), Arrays.asList(listing)));
        });
    }

    @SafeVarargs
    @OnlyIn(Dist.CLIENT)
    public static void registerItemColor(ItemColor color, Supplier<Item>... items) {
        FMLJavaModLoadingContext.get().getModEventBus().<ColorHandlerEvent.Item>addListener(e -> {
            for (Supplier<Item> item : items) {
                e.getItemColors().register(color, item.get());
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerItemOverride(Item item, ResourceLocation resourceLocation, ItemPropertyFunction itemPropertyFunction) {
        ItemProperties.register(item, resourceLocation, itemPropertyFunction);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerBlockRenderType(Block block, RenderType type) {
        ItemBlockRenderTypes.setRenderLayer(block, type);
    }

    @OnlyIn(Dist.CLIENT)
    public static <T extends Entity> void registerEntityRenderer(EntityType<T> entityType, Function<EntityRenderDispatcher, EntityRenderer<T>> factory) {
        RenderingRegistry.registerEntityRenderingHandler(entityType, factory::apply);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerSprite(ResourceLocation sprite, ResourceLocation atlas) {
        FMLJavaModLoadingContext.get().getModEventBus().<TextureStitchEvent.Pre>addListener(e -> {
            TextureAtlas texture = e.getMap();
            if (atlas.equals(texture.location())) {
                e.addSprite(sprite);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void registerScreenFactory(MenuType<M> type, RegistryBridge.ScreenFactory<M, S> object) {
        MenuScreens.register(type, object::create);
    }
}
