package gg.moonflower.etched.core.mixin.client;

import com.mojang.math.Vector3f;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.hook.extension.VillagerExtension;
import gg.moonflower.etched.core.util.AnimationUtil;
import gg.moonflower.pollen.pinwheel.api.client.animation.AnimatedGeometryEntityModel;
import gg.moonflower.pollen.pinwheel.api.client.animation.AnimatedModelPart;
import gg.moonflower.pollen.pinwheel.api.client.geometry.GeometryModel;
import gg.moonflower.pollen.pinwheel.api.client.geometry.GeometryModelRenderer;
import gg.moonflower.pollen.pinwheel.api.client.geometry.VanillaModelMapping;
import gg.moonflower.pollen.pinwheel.api.common.animation.AnimationData;
import gg.moonflower.pollen.pinwheel.api.common.geometry.GeometryModelData;
import gg.moonflower.pollen.pinwheel.core.client.geometry.BoneModelPart;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import org.apache.commons.lang3.time.StopWatch;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(VillagerModel.class)
public class VillagerModelMixin<T extends Entity> {

//    protected ModelPart head;
//    protected ModelPart hat;
//    protected final ModelPart hatRim;
//    protected final ModelPart body;
//    protected final ModelPart jacket;
//    protected final ModelPart arms;
//    protected final ModelPart leg0;
//    protected final ModelPart leg1;
//    protected final ModelPart nose;


    private static final AnimatedGeometryEntityModel<Entity> RIG = new AnimatedGeometryEntityModel<>(new ResourceLocation(Etched.MOD_ID, "villager"));
    private static final Set<String> USED_BONES = new HashSet<>();
    private static final Map<ModelPart, Vector3f> ORIGINS = new HashMap<>();
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

    private static Vector3f getOriginVec(ModelPart modelPart) {
        return ORIGINS.computeIfAbsent(modelPart, part -> new Vector3f(part.x, part.y, part.z));
    }

    @Inject(method = "setupAnim", at = @At("HEAD"))
    public void reset(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        Vector3f headVec = getOriginVec(this.head);
        this.head.x = headVec.x();
        this.head.y = headVec.y();
        this.head.z = headVec.z();

        Vector3f hatVec = getOriginVec(this.hat);
        this.hat.x = hatVec.x();
        this.hat.y = hatVec.y();
        this.hat.z = hatVec.z();

        Vector3f hatRimVec = getOriginVec(this.hatRim);
        this.hatRim.x = hatRimVec.x();
        this.hatRim.y = hatRimVec.y();
        this.hatRim.z = hatRimVec.z();

        Vector3f bodyVec = getOriginVec(this.body);
        this.body.x = bodyVec.x();
        this.body.y = bodyVec.y();
        this.body.z = bodyVec.z();

        Vector3f jacketVec = getOriginVec(this.jacket);
        this.jacket.x = jacketVec.x();
        this.jacket.y = jacketVec.y();
        this.jacket.z = jacketVec.z();

        Vector3f armsVec = getOriginVec(this.arms);
        this.arms.x = armsVec.x();
        this.arms.y = armsVec.y();
        this.arms.z = armsVec.z();

        Vector3f leg0Vec = getOriginVec(this.leg0);
        this.leg0.x = leg0Vec.x();
        this.leg0.y = leg0Vec.y();
        this.leg0.z = leg0Vec.z();

        Vector3f leg1Vec = getOriginVec(this.leg1);
        this.leg1.x = leg1Vec.x();
        this.leg1.y = leg1Vec.y();
        this.leg1.z = leg1Vec.z();

        Vector3f noseVec = getOriginVec(this.nose);
        this.nose.x = noseVec.x();
        this.nose.y = noseVec.y();
        this.nose.z = noseVec.z();
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
        Map<String, ModelPart> partMappings = AnimationUtil.getMappings(((VillagerModel<?>) (Object) this));
        USED_BONES.forEach(s -> AnimationUtil.copyAngles(s, model, partMappings.get(AnimationUtil.getNameForPart(s, ((VillagerModel<?>) (Object) this)))));
    }
}
