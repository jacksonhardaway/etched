package gg.moonflower.etched.core.mixin.client;

import gg.moonflower.etched.client.render.entity.VillagerAnimation;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.hook.VillagerExtension;
import gg.moonflower.etched.core.util.AnimationUtil;
import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangRuntime;
import gg.moonflower.pinwheel.api.animation.AnimationData;
import gg.moonflower.pinwheel.api.animation.PlayingAnimation;
import gg.moonflower.pinwheel.api.geometry.GeometryModel;
import gg.moonflower.pollen.api.render.geometry.v1.GeometryModelManager;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(VillagerModel.class)
public abstract class VillagerModelMixin<T extends Entity> {

    @Unique
    private static final ResourceLocation MODEL_LOCATION = new ResourceLocation(Etched.MOD_ID, "villager");
    @Unique
    private static final MolangEnvironment ENVIRONMENT = MolangRuntime.runtime().create();
    @Unique
    private static final PlayingAnimation ANIMATION = new VillagerAnimation();
    @Unique
    private static final Set<String> USED_BONES = new HashSet<>();
    @Unique
    private static final Map<String, PartPose> ORIGINS = new HashMap<>();

    @Shadow
    public abstract ModelPart root();

    @Shadow
    @Final
    private ModelPart root;

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
        if (!(entity instanceof VillagerExtension ext) || !ext.isDancing()) {
            return;
        }

        GeometryModel model = GeometryModelManager.getModel(MODEL_LOCATION);
        if (model == GeometryModel.EMPTY) {
            return;
        }

        ANIMATION.setAnimationTime(ageInTicks / 20.0F);
        model.applyAnimations(ENVIRONMENT, List.of(ANIMATION));

        USED_BONES.clear();
        for (AnimationData.BoneAnimation boneAnimation : ANIMATION.getAnimation().boneAnimations()) {
            USED_BONES.add(boneAnimation.name());
        }

        USED_BONES.forEach(s -> AnimationUtil.copyAngles(s, model, this.root().getChild(s)));
        USED_BONES.clear();
    }
}
