package gg.moonflower.etched.core.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface GuiAccessor {

    @Accessor
    Component getOverlayMessageString();

    @Accessor
    void setOverlayMessageTime(int overlayMessageTime);
}
