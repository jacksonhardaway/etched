package gg.moonflower.etched.core.mixin.forge.client;

import net.minecraftforge.client.gui.ForgeIngameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ForgeIngameGui.class)
public class ForgeIngameGuiMixin {

    @ModifyArg(method = "renderRecordOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;hsvToRgb(FFF)I"), index = 0)
    public float modifyHue(float f) {
        return ((f * 50.0F) % 50.0F) / 50.0F;
    }
}
