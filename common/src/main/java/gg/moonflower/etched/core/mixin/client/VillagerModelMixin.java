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
import net.minecraft.client.model.geom.PartPose;
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
public abstract class VillagerModelMixin<T extends Entity> {

    private static final AnimatedGeometryEntityModel<Entity> RIG = new AnimatedGeometryEntityModel<>(new ResourceLocation(Etched.MOD_ID, "villager"));
    private static final Set<String> USED_BONES = new HashSet<>();
    private static final Map<String, PartPose> ORIGINS = new HashMap<>();

    @Shadow public abstract ModelPart root();

    @Shadow @Final private ModelPart root;

    private PartPose getOrigin(String name) {
        return ORIGINS.computeIfAbsent(name, s -> this.root().getChild(name).storePose());
    }

    private void reset(ModelPart part, String name) {
        part.loadPose(getOrigin(name));
    }

    @Inject(method = "setupAnim", at = @At("HEAD"))
    public void reset(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof VillagerExtension))
            return;

        reset(this.root.getChild("head"), "head");
        reset(this.root.getChild("body"), "body");
        reset(this.root.getChild("arms"), "arms");
        reset(this.root.getChild("left_leg"), "left_leg");
        reset(this.root.getChild("right_leg"), "right_leg");
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

        RIG.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        GeometryModel model = RIG.getModel();
        USED_BONES.forEach(s -> AnimationUtil.copyAngles(s, model, this.root().getChild(s)));
    }
}
