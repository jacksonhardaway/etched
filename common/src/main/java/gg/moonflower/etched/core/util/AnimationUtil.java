package gg.moonflower.etched.core.util;

import gg.moonflower.pinwheel.api.geometry.GeometryModel;
import gg.moonflower.pinwheel.api.geometry.bone.AnimatedBone;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class AnimationUtil {

    public static void copyAngles(String name, GeometryModel model, ModelPart part) {
        model.getBoneOptional(name).ifPresent(bonePart -> applyPose(part, bonePart));
    }

    public static void applyPose(ModelPart part, AnimatedBone modelPart) {
        AnimatedBone.AnimationPose pose = modelPart.getAnimationPose();

        part.xRot += (float) (pose.rotation().x() * Math.PI / 180.0F);
        part.yRot += (float) (pose.rotation().y() * Math.PI / 180.0F);
        part.zRot += (float) (pose.rotation().z() * Math.PI / 180.0F);
        part.x += pose.position().x();
        part.y -= pose.position().y();
        part.z += pose.position().z();
    }
}
