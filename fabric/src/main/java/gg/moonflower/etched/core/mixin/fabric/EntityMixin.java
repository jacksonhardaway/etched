package gg.moonflower.etched.core.mixin.fabric;

import gg.moonflower.etched.core.hook.EntityHook;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "changeDimension", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;"))
    public void createPortalRadio(ServerLevel server, CallbackInfoReturnable<Entity> cir) {
        if ((Object) this instanceof ItemEntity) {
            EntityHook.warpRadio(server, (ItemEntity) (Object) this);
        }
    }
}
