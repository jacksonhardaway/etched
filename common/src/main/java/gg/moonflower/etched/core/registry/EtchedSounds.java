package gg.moonflower.etched.core.registry;

import gg.moonflower.etched.core.Etched;
import gg.moonflower.pollen.api.registry.PollinatedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

public class EtchedSounds {

    public static final PollinatedRegistry<SoundEvent> SOUNDS = PollinatedRegistry.create(Registry.SOUND_EVENT, Etched.MOD_ID);

    public static final Supplier<SoundEvent> UI_ETCHER_TAKE_RESULT = registerSound("ui.etching_table.take_result");

    private static Supplier<SoundEvent> registerSound(String id) {
        return SOUNDS.register(id, () -> new SoundEvent(new ResourceLocation(Etched.MOD_ID, id)));
    }
}
