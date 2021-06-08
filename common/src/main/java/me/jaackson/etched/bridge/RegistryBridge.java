package me.jaackson.etched.bridge;

import me.shedaniel.architectury.annotations.ExpectPlatform;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

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
    public static <V extends BlockEntity, T extends BlockEntityType<V>> Supplier<T> registerBlockEntity(String name, T object) {
        return Platform.safeAssertionError();
    }
}
