package gg.moonflower.etched.core.mixin.client;

import gg.moonflower.etched.common.item.BoomboxItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<T extends LivingEntity> {

    @Shadow
    public ModelPart leftArm;

    @Shadow
    public ModelPart rightArm;

    // TODO: fix arm swing when holding a boombox

    @Inject(method = "poseRightArm", at = @At("HEAD"), cancellable = true)
    public void poseRightArm(T livingEntity, CallbackInfo ci) {
        if (livingEntity instanceof Player) {
            Player player = (Player) livingEntity;
            InteractionHand playingHand = BoomboxItem.getPlayingHand(livingEntity);
            if ((livingEntity.getMainArm() == HumanoidArm.RIGHT && playingHand == InteractionHand.MAIN_HAND) ||
                    (livingEntity.getMainArm() == HumanoidArm.LEFT && playingHand == InteractionHand.OFF_HAND)) {
                this.rightArm.xRot = (float) Math.PI;
                this.rightArm.yRot = 0.0F;
                this.rightArm.zRot = -0.610865F;
                ci.cancel();
            }
        }
    }

    @Inject(method = "poseLeftArm", at = @At("HEAD"), cancellable = true)
    public void poseLeftArm(T livingEntity, CallbackInfo ci) {
        if (livingEntity instanceof Player) {
            Player player = (Player) livingEntity;
            InteractionHand playingHand = BoomboxItem.getPlayingHand(livingEntity);
            if ((livingEntity.getMainArm() == HumanoidArm.LEFT && playingHand == InteractionHand.MAIN_HAND) ||
                    (livingEntity.getMainArm() == HumanoidArm.RIGHT && playingHand == InteractionHand.OFF_HAND)) {
                this.leftArm.xRot = (float) Math.PI;
                this.leftArm.yRot = 0.0F;
                this.leftArm.zRot = 0.610865F;
                ci.cancel();
            }
        }
    }

    @Inject(method = "setupAttackAnimation", at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/geom/ModelPart;xRot:F", ordinal = 2), locals = LocalCapture.CAPTURE_FAILEXCEPTION, cancellable = true)
    public void setupAttackAnimation(T livingEntity, float f, CallbackInfo ci, HumanoidArm arm, ModelPart part) {
        InteractionHand playingHand = BoomboxItem.getPlayingHand(livingEntity);
        boolean leftArm = ((livingEntity.getMainArm() == HumanoidArm.LEFT && playingHand == InteractionHand.MAIN_HAND) || (livingEntity.getMainArm() == HumanoidArm.RIGHT && playingHand == InteractionHand.OFF_HAND)) && arm == HumanoidArm.LEFT;
        boolean rightArm = ((livingEntity.getMainArm() == HumanoidArm.RIGHT && playingHand == InteractionHand.MAIN_HAND) || (livingEntity.getMainArm() == HumanoidArm.LEFT && playingHand == InteractionHand.OFF_HAND)) && arm == HumanoidArm.RIGHT;
        if (leftArm || rightArm)
            ci.cancel();
    }

}
