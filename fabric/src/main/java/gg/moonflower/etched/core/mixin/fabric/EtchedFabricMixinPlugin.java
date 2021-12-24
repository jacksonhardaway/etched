package gg.moonflower.etched.core.mixin.fabric;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class EtchedFabricMixinPlugin implements IMixinConfigPlugin {
    private boolean optifineLoaded;

    @Override
    public void onLoad(String mixinPackage) {

        // Since the OptiFine jar never gets loaded this early on Fabric, we must check if OptiFabric is installed.
        // OptiFabric won't let the game run if OptiFine isn't installed, so we can ensure OptiFine will be installed if this is present.
        try {
            Class.forName("me.modmuss50.optifabric.mod.OptifabricSetup");
            this.optifineLoaded = true;
        } catch (ClassNotFoundException ignored) {
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return this.optifineLoaded ? !mixinClassName.equals("gg.moonflower.etched.core.mixin.fabric.client.LevelRendererMixin") : !mixinClassName.equals("gg.moonflower.etched.core.mixin.fabric.client.OptifineLevelRendererMixin");
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
