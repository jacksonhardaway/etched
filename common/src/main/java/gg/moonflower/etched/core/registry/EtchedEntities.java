package gg.moonflower.etched.core.registry;

import dev.architectury.registry.registries.DeferredRegister;
import gg.moonflower.etched.common.entity.MinecartJukebox;
import gg.moonflower.etched.core.Etched;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

public class EtchedEntities {

    public static final DeferredRegister<EntityType<?>> REGUSTRY = DeferredRegister.create(Etched.MOD_ID, Registry.ENTITY_TYPE_REGISTRY);

    public static final Supplier<EntityType<MinecartJukebox>> JUKEBOX_MINECART = REGUSTRY.register("jukebox_minecart", () -> EntityType.Builder.<MinecartJukebox>of(MinecartJukebox::new, MobCategory.MISC).sized(0.98F, 0.7F).clientTrackingRange(8).build("minecart_jukebox"));
}
