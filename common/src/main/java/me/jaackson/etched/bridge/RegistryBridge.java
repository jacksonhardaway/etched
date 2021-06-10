package me.jaackson.etched.bridge;

import me.shedaniel.architectury.annotations.ExpectPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Jackson
 */
public final class RegistryBridge {

    @ExpectPlatform
    public static <T extends SoundEvent> Supplier<T> registerSound(String name, T object) {
        return Platform.safeAssertionError();
    }

    @ExpectPlatform
    public static <T extends Item> Supplier<T> registerItem(String name, T object) {
        return Platform.safeAssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> Supplier<T> registerBlock(String name, T object) {
        return Platform.safeAssertionError();
    }

    @ExpectPlatform
    public static <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenu(String name, RegistryBridge.MenuFactory<T> object) {
        return Platform.safeAssertionError();
    }

    @SafeVarargs
    @ExpectPlatform
    @Environment(EnvType.CLIENT)
    public static void registerItemColor(ItemColor color, Supplier<Item>... items) {
        Platform.safeAssertionError();
    }

    @ExpectPlatform
    @Environment(EnvType.CLIENT)
    public static void registerItemOverride(Item item, ResourceLocation resourceLocation, ItemPropertyFunction itemPropertyFunction) {
        Platform.safeAssertionError();
    }

    public static Supplier<Block> registerBlock(String name, Block block, Item.Properties properties) {
        return registerBlock(name, block, blockSupplier -> new BlockItem(block, properties));
    }

    public static Supplier<Block> registerBlock(String name, Block block, Function<Supplier<Block>, Item> item) {
        Supplier<Block> register = registerBlock(name, block);
        registerItem(name, item.apply(register));
        return register;
    }

    public interface MenuFactory<T extends AbstractContainerMenu> {
        T create(int id, Inventory inventory);
    }
}
