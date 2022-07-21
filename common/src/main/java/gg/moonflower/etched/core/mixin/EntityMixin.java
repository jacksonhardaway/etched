package gg.moonflower.etched.core.mixin;

import gg.moonflower.etched.core.registry.EtchedBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "changeDimension", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;"))
    public void createPortalRadio(ServerLevel server, CallbackInfoReturnable<Entity> cir) {
        if ((Object) this instanceof ItemEntity && server.dimension() == Level.NETHER) {
            ItemEntity itemEntity = (ItemEntity) (Object) this;
            ItemStack oldStack = itemEntity.getItem();
            if (oldStack.getItem() != EtchedBlocks.RADIO.get().asItem())
                return;

            ItemStack newStack = new ItemStack(EtchedBlocks.PORTAL_RADIO_ITEM.get(), oldStack.getCount());
            newStack.setTag(oldStack.getTag());
            itemEntity.setItem(newStack);
        }
    }
}
