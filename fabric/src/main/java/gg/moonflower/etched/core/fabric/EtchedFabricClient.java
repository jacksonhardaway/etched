package gg.moonflower.etched.core.fabric;

import gg.moonflower.etched.common.entity.MinecartJukebox;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.player.ClientPickBlockGatherCallback;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * @author Jackson
 */
public class EtchedFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPickBlockGatherCallback.EVENT.register((player, result) -> {
            if (result.getType() == HitResult.Type.ENTITY && player.abilities.instabuild) {
                Entity entity = ((EntityHitResult) result).getEntity();
                if (entity instanceof MinecartJukebox)
                    return ((MinecartJukebox) entity).getCartItem();
            }
            return ItemStack.EMPTY;
        });
    }
}
