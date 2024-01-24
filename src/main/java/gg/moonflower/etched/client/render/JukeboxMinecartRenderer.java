package gg.moonflower.etched.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

public class JukeboxMinecartRenderer<T extends AbstractMinecart> extends MinecartRenderer<T> {

    public JukeboxMinecartRenderer(EntityRendererProvider.Context context) {
        super(context, EtchedModelLayers.JUKEBOX_MINECART);
    }
}
