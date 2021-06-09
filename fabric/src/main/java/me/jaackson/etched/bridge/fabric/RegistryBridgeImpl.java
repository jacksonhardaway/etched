package me.jaackson.etched.bridge.fabric;

import me.jaackson.etched.Etched;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

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

    public static <V extends BlockEntity, T extends BlockEntityType<V>> Supplier<T> registerBlockEntity(String name, Supplier<T> object) {
        T register = Registry.register(Registry.BLOCK_ENTITY_TYPE, new ResourceLocation(Etched.MOD_ID, name), object.get());
        return () -> register;
    }

    @SafeVarargs
    @Environment(EnvType.CLIENT)
    public static void registerItemColor(ItemColor color, Supplier<Item>... items) {
        for (Supplier<Item> item : items) {
            ColorProviderRegistry.ITEM.register(color, item.get());
        }
    }

}
