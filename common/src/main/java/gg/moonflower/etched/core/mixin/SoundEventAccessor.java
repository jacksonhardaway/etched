package gg.moonflower.etched.core.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundEvent.class)
public interface SoundEventAccessor {

    @Accessor
    ResourceLocation getLocation();
}
