package gg.moonflower.etched.core.hook.forge;

import gg.moonflower.etched.core.mixin.forge.RecordItemAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.RecordItem;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public class RecordItemHookImpl {
    public static SoundEvent getSound(RecordItem item) {
        return ((RecordItemAccessor) item).getSoundSupplier().get();
    }
}
