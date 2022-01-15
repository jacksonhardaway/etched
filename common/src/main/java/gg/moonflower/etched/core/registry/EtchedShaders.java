package gg.moonflower.etched.core.registry;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.pollen.api.registry.client.ShaderRegistry;
import net.minecraft.resources.ResourceLocation;

public class EtchedShaders {

    public static final ResourceLocation ETCHING_SCREEN_LABEL = new ResourceLocation(Etched.MOD_ID, "etching_screen_label");

    public static void init() {
        ShaderRegistry.register(ETCHING_SCREEN_LABEL, DefaultVertexFormat.POSITION_COLOR_TEX);
    }
}
