package gg.moonflower.etched.core.hook;

import dev.architectury.injectables.annotations.ExpectPlatform;
import gg.moonflower.pollen.core.Pollen;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.RecordItem;

public class RecordItemHook {

    @ExpectPlatform
    public static SoundEvent getSound(RecordItem item) {
        return Pollen.expect();
    }
}
