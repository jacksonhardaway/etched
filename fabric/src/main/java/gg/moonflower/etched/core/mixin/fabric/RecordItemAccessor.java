package gg.moonflower.etched.core.mixin.fabric;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.RecordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecordItem.class)
public interface RecordItemAccessor {

    @Accessor
    SoundEvent getSound();
}
