package gg.moonflower.etched.core.mixin.client;

import gg.moonflower.etched.common.item.BoomboxItem;
import gg.moonflower.etched.core.Etched;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow
    public abstract Item getItem();

    @Inject(method = "getTooltipLines", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    public void addBoomboxStatus(Player player, TooltipFlag isAdvanced, CallbackInfoReturnable<List<Component>> cir, List<Component> lines) {
        if (this.getItem() instanceof BoomboxItem && BoomboxItem.isPaused((ItemStack) (Object) this))
            lines.add(BoomboxItem.PAUSED);
    }
}
