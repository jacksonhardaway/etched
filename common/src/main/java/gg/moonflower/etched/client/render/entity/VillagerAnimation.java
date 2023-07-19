package gg.moonflower.etched.client.render.entity;

import gg.moonflower.etched.core.Etched;
import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.pinwheel.api.animation.AnimationData;
import gg.moonflower.pinwheel.api.animation.PlayingAnimation;
import gg.moonflower.pollen.api.render.animation.v1.AnimationManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class VillagerAnimation implements PlayingAnimation {

    private static final ResourceLocation LOCATION = new ResourceLocation(Etched.MOD_ID, "dance");

    private float time;

    @Override
    public AnimationData getAnimation() {
        return AnimationManager.getAnimation(LOCATION);
    }

    @Override
    public float getAnimationTime() {
        return this.time;
    }

    @Override
    public float getWeightFactor() {
        return 1.0F;
    }

    @Override
    public float getWeight(MolangEnvironment molangEnvironment) {
        return 1.0f;
    }

    @Override
    public void setAnimationTime(float time) {
        this.time = time;
    }

    @Override
    public void setWeight(float weight) {
    }
}
