package gg.moonflower.etched.client.render.entity;

import gg.moonflower.pollen.api.registry.client.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

public class ContextualMinecartRenderer<T extends AbstractMinecart> extends MinecartRenderer<T> {
    public ContextualMinecartRenderer(EntityRendererRegistry.EntityRendererFactory.Context context) {
        super(context.getEntityRenderDispatcher());
    }
}
