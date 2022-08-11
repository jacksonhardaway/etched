package gg.moonflower.etched.core.util;

import gg.moonflower.etched.core.mixin.client.VillagerModelMixin;
import gg.moonflower.pollen.pinwheel.api.client.animation.AnimatedModelPart;
import gg.moonflower.pollen.pinwheel.api.client.geometry.GeometryModel;
import gg.moonflower.pollen.pinwheel.api.client.geometry.VanillaModelMapping;
import gg.moonflower.pollen.pinwheel.api.common.geometry.GeometryModelData;
import gg.moonflower.pollen.pinwheel.core.client.geometry.BoneModelPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class AnimationUtil {

    private static final Map<Model, Map<String, ModelPart>> MODEL_PARTS = new HashMap<>();
    private static final Map<String, String> MAPPED_NAMES = new HashMap<>();

    public static void copyAngles(String name, GeometryModel model, ModelPart part) {
        model.getModelPart(name).ifPresent(bonePart -> {
            if (bonePart instanceof BoneModelPart) {
                applyPose(part, (BoneModelPart) bonePart);
            }
        });
    }

    public static void applyPose(ModelPart part, BoneModelPart modelPart) {
        AnimatedModelPart.AnimationPose pose = modelPart.getAnimationPose();

        part.xRot += (float) (pose.getRotation().x() * Math.PI / 180.0F);
        part.yRot += (float) (pose.getRotation().y() * Math.PI / 180.0F);
        part.zRot += (float) (pose.getRotation().z() * Math.PI / 180.0F);
        part.x += pose.getPosition().x();
        part.y -= pose.getPosition().y();
        part.z += pose.getPosition().z();
    }

    public static Map<String, ModelPart> mapRenderers(Model model) {
        Map<String, ModelPart> renderers = new HashMap<>();
        Class<?> i = model.getClass();
        while (i != null && i != Object.class) {
            for (Field field : i.getDeclaredFields()) {
                if (!field.isSynthetic()) {
                    if (ModelPart.class.isAssignableFrom(field.getType())) {
                        try {
                            field.setAccessible(true);
                            renderers.put(field.getName(), (ModelPart) field.get(model));
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            i = i.getSuperclass();
        }
        return renderers;
    }

    public static Map<String, ModelPart> getMappings(Model model) {
        return MODEL_PARTS.computeIfAbsent(model, AnimationUtil::mapRenderers);
    }

    public static String getNameForPart(String name, Model model) {
        return MAPPED_NAMES.computeIfAbsent(name, key -> VanillaModelMapping.get(model.getClass(), key));
    }
}
