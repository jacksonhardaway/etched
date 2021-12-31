package gg.moonflower.etched.client.render.entity;

import gg.moonflower.etched.client.render.model.EtchedModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

public class ContextualMinecartRenderer<T extends AbstractMinecart> extends MinecartRenderer<T> {
    public ContextualMinecartRenderer(EntityRendererProvider.Context context) {
        super(context, EtchedModelLayers.JUKEBOX_MINECART);
    }
}
