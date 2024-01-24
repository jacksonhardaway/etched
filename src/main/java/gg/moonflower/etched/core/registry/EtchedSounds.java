package gg.moonflower.etched.core.registry;

import gg.moonflower.etched.core.Etched;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class EtchedSounds {

    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Etched.MOD_ID);

    public static final Supplier<SoundEvent> UI_ETCHER_TAKE_RESULT = registerSound("ui.etching_table.take_result");

    private static Supplier<SoundEvent> registerSound(String id) {
        return REGISTRY.register(id, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Etched.MOD_ID, id)));
    }
}
