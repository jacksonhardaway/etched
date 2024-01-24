package gg.moonflower.etched.core.mixin.client;

import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ForgeGui.class)
public class ForgeIngameGuiMixin {

    @ModifyArg(method = "renderRecordOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;hsvToRgb(FFF)I"), index = 0)
    public float modifyHue(float hue) {
        return ((hue * 50.0F) % 50.0F) / 50.0F;
    }
}
