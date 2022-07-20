package gg.moonflower.etched.core.hook.fabric;

import gg.moonflower.etched.core.mixin.fabric.RecordItemAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.RecordItem;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class RecordItemHookImpl {

    public static SoundEvent getSound(RecordItem item) {
        return ((RecordItemAccessor) item).getSound();
    }
}
