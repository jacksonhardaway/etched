package gg.moonflower.etched.core.mixin.client;

import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.item.BoomboxItem;
import gg.moonflower.etched.core.registry.EtchedTags;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(Parrot.class)
public abstract class ParrotMixin extends Entity {

    @Shadow
    private BlockPos jukebox;
    @Shadow
    private boolean partyParrot;

    @Unique
    private BlockPos etched$musicPos;
    @Unique
    private boolean etched$dancing;

    public ParrotMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "aiStep", at = @At("HEAD"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    public void capture(CallbackInfo ci) {
        this.etched$musicPos = this.jukebox;
        this.etched$dancing = this.partyParrot;
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/ShoulderRidingEntity;aiStep()V"))
    public void addAudioProviders(CallbackInfo ci) {
        if (this.etched$musicPos == null || !this.etched$musicPos.closerToCenterThan(this.position(), 3.46) || !this.level().getBlockState(this.etched$musicPos).is(Blocks.JUKEBOX) && !this.level().getBlockState(this.etched$musicPos).is(EtchedTags.AUDIO_PROVIDER)) {
            this.partyParrot = false;
            this.jukebox = null;
        } else {
            this.partyParrot = this.etched$dancing;
            this.jukebox = this.etched$musicPos;
        }

        if (this.level().isClientSide()) {
            List<Entity> entities = this.level().getEntities(this, this.getBoundingBox().inflate(3.45), entity -> {
                if (!entity.isAlive() || entity.isSpectator()) {
                    return false;
                }
                if (entity == Minecraft.getInstance().player && BoomboxItem.getPlayingHand((LivingEntity) entity) == null) {
                    return false;
                }

                return SoundTracker.getEntitySound(entity.getId()) != null;
            });

            if (!entities.isEmpty()) {
                this.partyParrot = true;
            }
        }
    }
}
