package gg.moonflower.etched.core.mixin;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.client.sound.EntityRecordSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

@Mixin(RecordItem.class)
public class RecordItemMixin implements PlayableRecord {

    @Override
    public boolean canPlay(ItemStack stack) {
        return true;
    }

    @Override
    public Optional<SoundInstance> createEntitySound(ItemStack stack, Entity entity) {
        if (!(stack.getItem() instanceof RecordItem))
            return Optional.empty();

        if (PlayableRecord.canShowMessage(entity.getX(), entity.getY(), entity.getZ()))
            Minecraft.getInstance().gui.setNowPlaying(((RecordItem) stack.getItem()).getDisplayName());
        return Optional.of(new EntityRecordSoundInstance(((RecordItem) stack.getItem()).getSound(), entity));
    }
}
