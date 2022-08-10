package gg.moonflower.etched.core.mixin.client;

import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.item.BoomboxItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.ShoulderRidingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Parrot.class)
public abstract class ParrotMixin extends ShoulderRidingEntity {

    @Shadow private boolean partyParrot;

    protected ParrotMixin(EntityType<? extends ShoulderRidingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/ShoulderRidingEntity;aiStep()V"))
    public void checkEntitySound(CallbackInfo ci) {
        if (this.level.isClientSide()) {
            List<Entity> entities = this.level.getEntitiesOfClass(Entity.class, new AABB(this.position().x() - 3.46, this.position().y() - 3.46, this.position().z() - 3.46, this.position().x() + 3.46, this.position().y() + 3.46, this.position().z() + 3.46), entity -> {
                if (entity instanceof LivingEntity) {
                    if (BoomboxItem.getPlayingHand((LivingEntity) entity) != null) {
                        return true;
                    }

                    if (entity instanceof Player)
                        return false;
                }

                return SoundTracker.getEntitySound(entity.getId()) != null;
            });
            if (!entities.isEmpty())
                this.partyParrot = true;
        }
    }
}
