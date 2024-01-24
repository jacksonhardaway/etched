package gg.moonflower.etched.core.mixin.grindstone;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"net.minecraft.world.inventory.GrindstoneMenu$2", "net.minecraft.world.inventory.GrindstoneMenu$3"})
public class GrindstoneMenuRepairSlotMixin {

    @Inject(method = "mayPlace(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    public void mayPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
