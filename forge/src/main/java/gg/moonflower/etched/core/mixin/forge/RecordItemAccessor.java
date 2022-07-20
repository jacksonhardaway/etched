package gg.moonflower.etched.core.mixin.forge;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.RecordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;

@Mixin(RecordItem.class)
public interface RecordItemAccessor {

    @Accessor
    Supplier<SoundEvent> getSoundSupplier();
}
