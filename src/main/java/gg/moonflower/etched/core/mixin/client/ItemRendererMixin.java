package gg.moonflower.etched.core.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.registry.EtchedItems;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Unique
    private static final ModelResourceLocation etched$BOOMBOX_IN_HAND_MODEL = new ModelResourceLocation(new ResourceLocation(Etched.MOD_ID, "boombox_in_hand"), "inventory");

    @Shadow
    @Final
    private ItemModelShaper itemModelShaper;

    @Unique
    private Item etched$capturedItem;

    @Unique
    private Item etched$capturedHandItem;

    @Inject(method = "render", at = @At("HEAD"))
    public void capture(ItemStack itemStack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model, CallbackInfo ci) {
        this.etched$capturedItem = itemStack.getItem();
    }

    @ModifyVariable(method = "render", ordinal = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z", ordinal = 0, shift = At.Shift.BEFORE), argsOnly = true)
    public BakedModel render(BakedModel original) {
        if (this.etched$capturedHandItem == EtchedItems.BOOMBOX.get()) {
            return this.itemModelShaper.getItemModel(this.etched$capturedItem);
        }
        return original;
    }

    @Inject(method = "getModel", at = @At("HEAD"))
    public void capture(ItemStack itemStack, Level level, LivingEntity livingEntity, int i, CallbackInfoReturnable<BakedModel> cir) {
        this.etched$capturedHandItem = itemStack.getItem();
    }

    @ModifyVariable(method = "getModel", ordinal = 0, at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/renderer/ItemModelShaper;getItemModel(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/client/resources/model/BakedModel;", shift = At.Shift.AFTER))
    public BakedModel getModel(BakedModel original) {
        if (this.etched$capturedHandItem == EtchedItems.BOOMBOX.get()) {
            return this.itemModelShaper.getModelManager().getModel(etched$BOOMBOX_IN_HAND_MODEL);
        }
        return original;
    }
}
