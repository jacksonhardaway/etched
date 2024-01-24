package gg.moonflower.etched.core.mixin.grindstone;

import gg.moonflower.etched.common.item.AlbumCoverItem;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin extends AbstractContainerMenu {

    @Shadow
    @Final
    Container repairSlots;

    @Shadow
    @Final
    private Container resultSlots;

    private GrindstoneMenuMixin(@Nullable MenuType<?> menuType, int i) {
        super(menuType, i);
    }

    @Unique
    private boolean isValid(ItemStack stack) {
        return stack.isDamageableItem() || stack.is(Items.ENCHANTED_BOOK) || stack.isEnchanted();
    }

    @Unique
    private static boolean etched$matches(Container container) {
        ItemStack stack = ItemStack.EMPTY;
        for (int j = 0; j < 2; ++j) {
            ItemStack itemStack = container.getItem(j);
            if (!itemStack.isEmpty()) {
                if (!stack.isEmpty()) {
                    return false;
                }
                stack = itemStack;
            }
        }

        return AlbumCoverItem.getCoverStack(stack).isPresent();
    }

    @Unique
    private static ItemStack etched$assemble(Container container) {
        ItemStack result = container.getItem(0).isEmpty() ? container.getItem(1) : container.getItem(0).copy();
        result.setCount(1);
        AlbumCoverItem.setCover(result, ItemStack.EMPTY);
        return result;
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    public void modifyAllowed(CallbackInfo ci) {
        // Reset result slot
        this.resultSlots.setItem(0, ItemStack.EMPTY);

        if (etched$matches(this.repairSlots)) {
            this.resultSlots.setItem(0, etched$assemble(this.repairSlots));
            ci.cancel();
            this.broadcastChanges();
            return;
        }

        // The recipe was invalid, so the slots need to be checked for the vanilla recipe
        for (int i = 0; i < this.repairSlots.getContainerSize(); i++) {
            ItemStack stack = this.repairSlots.getItem(i);
            if (!stack.isEmpty() && !this.isValid(stack)) {
                ci.cancel();
                return;
            }
        }
    }
}
