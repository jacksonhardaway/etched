package gg.moonflower.etched.core.fabric;

import gg.moonflower.etched.common.entity.MinecartJukebox;
import gg.moonflower.etched.core.EtchedClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.player.ClientPickBlockGatherCallback;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class EtchedFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EtchedClient.init();
        EtchedClient.postInit();

        ClientPickBlockGatherCallback.EVENT.register((player, result) -> {
            if (result.getType() == HitResult.Type.ENTITY && player.getAbilities().instabuild) {
                Entity entity = ((EntityHitResult) result).getEntity();
                if (entity instanceof MinecartJukebox minecart) {
                    return new ItemStack(minecart.getDropItem());
                }
            }
            return ItemStack.EMPTY;
        });
    }
}
