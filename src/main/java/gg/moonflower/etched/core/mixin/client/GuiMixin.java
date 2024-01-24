package gg.moonflower.etched.core.mixin.client;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Gui.class)
public class GuiMixin {

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;hsvToRgb(FFF)I"), index = 0)
    public float modifyHue(float f) {
        return ((f * 50.0F) % 50.0F) / 50.0F;
    }
}
