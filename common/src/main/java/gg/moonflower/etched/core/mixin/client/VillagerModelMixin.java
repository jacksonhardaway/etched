package gg.moonflower.etched.core.mixin.client;

import com.google.common.collect.ImmutableList;
import com.mojang.math.Vector3f;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.hook.extension.VillagerExtension;
import gg.moonflower.etched.core.util.AnimationUtil;
import gg.moonflower.pollen.pinwheel.api.client.animation.AnimatedGeometryEntityModel;
import gg.moonflower.pollen.pinwheel.api.client.geometry.GeometryModel;
import gg.moonflower.pollen.pinwheel.api.common.animation.AnimationData;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(VillagerModel.class)
public class VillagerModelMixin<T extends Entity> {

    private static final AnimatedGeometryEntityModel<Entity> RIG = new AnimatedGeometryEntityModel<>(new ResourceLocation(Etched.MOD_ID, "villager"));
    private static final Set<String> USED_BONES = new HashSet<>();
    private static final Map<ModelPart, ModelPart> ORIGINS = new HashMap<>();
    @Shadow
    protected ModelPart head;

    @Shadow protected ModelPart hat;

    @Shadow @Final protected ModelPart hatRim;

    @Shadow @Final protected ModelPart nose;

    @Shadow @Final protected ModelPart body;

    @Shadow @Final protected ModelPart jacket;

    @Shadow @Final protected ModelPart arms;

    @Shadow @Final protected ModelPart leg0;

    @Shadow @Final protected ModelPart leg1;

    private static ModelPart getOrigin(ModelPart part) {
        return ORIGINS.computeIfAbsent(part, ModelPart::createShallowCopy);
    }

    private static void reset(ModelPart part) {
        part.copyFrom(getOrigin(part));
    }

    @Inject(method = "setupAnim", at = @At("HEAD"))
    public void reset(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof VillagerExtension))
            return;

        reset(this.head);
        reset(this.hat);
        reset(this.hatRim);
        reset(this.nose);
        reset(this.body);
        reset(this.jacket);
        reset(this.arms);
        reset(this.leg0);
        reset(this.leg1);
    }

    @Inject(method = "setupAnim", at = @At("TAIL"))
    public void animate(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof VillagerExtension) || !((VillagerExtension) entity).isDancing())
            return;

        USED_BONES.clear();
        RIG.setAnimations(new ResourceLocation(Etched.MOD_ID, "dance"));
        for (AnimationData animation : RIG.getAnimations()) {
            for (AnimationData.BoneAnimation boneAnimation : animation.getBoneAnimations())
                USED_BONES.add(boneAnimation.getName());
        }

//        RIG.setVariableProvider(context ->
//        {
//            context.add("head_x_rotation", (float) (this.head.xRot * 180.0F / Math.PI));
//            context.add("head_y_rotation", (float) (this.head.yRot * 180.0F / Math.PI));
//            context.add("head_z_rotation", (float) (this.head.zRot * 180.0F / Math.PI));
//            context.add("body_x_rotation", (float) (this.body.xRot * 180.0F / Math.PI));
//            context.add("body_y_rotation", (float) (this.body.yRot * 180.0F / Math.PI));
//            context.add("body_z_rotation", (float) (this.body.zRot * 180.0F / Math.PI));
//            context.add("arms_x_rotation", (float) (this.arms.xRot * 180.0F / Math.PI));
//            context.add("arms_y_rotation", (float) (this.arms.yRot * 180.0F / Math.PI));
//            context.add("arms_z_rotation", (float) (this.arms.zRot * 180.0F / Math.PI));
//            context.add("leg0_x_rotation", (float) (this.leg0.xRot * 180.0F / Math.PI));
//            context.add("leg0_y_rotation", (float) (this.leg0.yRot * 180.0F / Math.PI));
//            context.add("leg0_z_rotation", (float) (this.leg0.zRot * 180.0F / Math.PI));
//            context.add("leg1_x_rotation", (float) (this.leg1.xRot * 180.0F / Math.PI));
//            context.add("leg1_y_rotation", (float) (this.leg1.yRot * 180.0F / Math.PI));
//            context.add("leg1_z_rotation", (float) (this.leg1.zRot * 180.0F / Math.PI));
//        });
        RIG.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        GeometryModel model = RIG.getModel();
        Map<String, ModelPart> partMappings = AnimationUtil.getMappings(((VillagerModel<?>) (Object) this));
        USED_BONES.forEach(s -> AnimationUtil.copyAngles(s, model, partMappings.get(AnimationUtil.getNameForPart(s, ((VillagerModel<?>) (Object) this)))));
    }
}
