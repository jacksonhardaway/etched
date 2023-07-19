package gg.moonflower.etched.core.registry;

import dev.architectury.registry.registries.DeferredRegister;
import gg.moonflower.etched.core.Etched;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

public class EtchedSounds {

    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(Etched.MOD_ID, Registry.SOUND_EVENT_REGISTRY);

    public static final Supplier<SoundEvent> UI_ETCHER_TAKE_RESULT = registerSound("ui.etching_table.take_result");

    private static Supplier<SoundEvent> registerSound(String id) {
        return REGISTRY.register(id, () -> new SoundEvent(new ResourceLocation(Etched.MOD_ID, id)));
    }
}
