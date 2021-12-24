package gg.moonflower.etched.core.mixin;

import gg.moonflower.etched.api.common.item.PlayableRecordItem;
import gg.moonflower.etched.client.sound.JukeboxMinecartSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

@Mixin(RecordItem.class)
public class RecordItemMixin implements PlayableRecordItem {

    @Override
    public boolean canPlay(ItemStack stack) {
        return true;
    }

    @Override
    public Optional<SoundInstance> createEntitySound(ItemStack stack, Entity entity) {
        if (!(stack.getItem() instanceof RecordItem))
            return Optional.empty();

        if (PlayableRecordItem.canShowMessage(entity.getX(), entity.getY(), entity.getZ()))
            Minecraft.getInstance().gui.setNowPlaying(((RecordItem) stack.getItem()).getDisplayName());
        return Optional.of(new JukeboxMinecartSoundInstance(((RecordItem) stack.getItem()).getSound(), entity));
    }
}
