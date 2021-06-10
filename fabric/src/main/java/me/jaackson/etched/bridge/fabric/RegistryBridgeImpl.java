package me.jaackson.etched.bridge.fabric;

import me.jaackson.etched.Etched;
import me.jaackson.etched.bridge.RegistryBridge;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * @author Jackson
 */
public class RegistryBridgeImpl {

    public static <T extends SoundEvent> Supplier<T> registerSound(String name, T object) {
        T register = Registry.register(Registry.SOUND_EVENT, new ResourceLocation(Etched.MOD_ID, name), object);
        return () -> register;
    }

    public static <T extends Item> Supplier<T> registerItem(String name, T object) {
        T register = Registry.register(Registry.ITEM, new ResourceLocation(Etched.MOD_ID, name), object);
        return () -> register;
    }

    public static <T extends Block> Supplier<T> registerBlock(String name, T object) {
        T register = Registry.register(Registry.BLOCK, new ResourceLocation(Etched.MOD_ID, name), object);
        return () -> register;
    }

    public static <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenu(String name, RegistryBridge.MenuFactory<T> object) {
        MenuType<T> register = ScreenHandlerRegistry.registerSimple(new ResourceLocation(Etched.MOD_ID, name), object::create);
        return () -> register;
    }

    @SafeVarargs
    @Environment(EnvType.CLIENT)
    public static void registerItemColor(ItemColor color, Supplier<Item>... items) {
        for (Supplier<Item> item : items) {
            ColorProviderRegistry.ITEM.register(color, item.get());
        }
    }

    @Environment(EnvType.CLIENT)
    public static void registerItemOverride(Item item, ResourceLocation resourceLocation, ItemPropertyFunction itemPropertyFunction) {
        FabricModelPredicateProviderRegistry.register(item, resourceLocation, itemPropertyFunction);
    }
}
