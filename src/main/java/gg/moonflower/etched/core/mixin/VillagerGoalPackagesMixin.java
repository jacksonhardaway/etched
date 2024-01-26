package gg.moonflower.etched.core.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import gg.moonflower.etched.common.entity.WorkAtNoteBlock;
import gg.moonflower.etched.core.registry.EtchedVillagers;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.ai.behavior.WorkAtPoi;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerGoalPackages.class)
public class VillagerGoalPackagesMixin {

    @Unique
    private static VillagerProfession etched$capturedProfession;

    @Inject(method = "getWorkPackage", at = @At("HEAD"))
    private static void capture(VillagerProfession profession, float f, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends Behavior<? super Villager>>>> cir) {
        VillagerGoalPackagesMixin.etched$capturedProfession = profession;
    }

    @Inject(method = "getWorkPackage", at = @At("RETURN"))
    private static void clear(VillagerProfession profession, float f, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends Behavior<? super Villager>>>> cir) {
        VillagerGoalPackagesMixin.etched$capturedProfession = null;
    }

    @ModifyVariable(method = "getWorkPackage", index = 2, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/VillagerGoalPackages;getMinimalLookBehavior()Lcom/mojang/datafixers/util/Pair;"))
    private static WorkAtPoi modifyWorkPoi(WorkAtPoi value) {
        if (etched$capturedProfession == EtchedVillagers.BARD.get()) {
            return new WorkAtNoteBlock();
        }
        return value;
    }
}
