package gg.moonflower.etched.core.registry;

import gg.moonflower.etched.common.entity.MinecartJukebox;
import gg.moonflower.etched.core.Etched;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class EtchedEntities {

    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Etched.MOD_ID);

    public static final Supplier<EntityType<MinecartJukebox>> JUKEBOX_MINECART = REGISTRY.register("jukebox_minecart", () -> EntityType.Builder.<MinecartJukebox>of(MinecartJukebox::new, MobCategory.MISC).sized(0.98F, 0.7F).clientTrackingRange(8).build("minecart_jukebox"));
}
