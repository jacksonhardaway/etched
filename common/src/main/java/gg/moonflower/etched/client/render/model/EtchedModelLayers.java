package gg.moonflower.etched.client.render.model;

import gg.moonflower.etched.core.Etched;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public class EtchedModelLayers {
    public static final ModelLayerLocation JUKEBOX_MINECART = create("jukebox_minecart");

    public static ModelLayerLocation create(String model) {
        return create(model, "main");
    }

    public static ModelLayerLocation create(String model, String layer) {
        return new ModelLayerLocation(new ResourceLocation(Etched.MOD_ID, model), layer);
    }
}
