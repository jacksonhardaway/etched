package gg.moonflower.etched.client.screen;

import gg.moonflower.etched.common.menu.BoomboxMenu;
import gg.moonflower.etched.core.Etched;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * @author Ocelot
 */
public class BoomboxScreen extends AbstractContainerScreen<BoomboxMenu> {

    private static final ResourceLocation BOOMBOX_LOCATION = new ResourceLocation(Etched.MOD_ID, "textures/gui/container/boombox.png");

    public BoomboxScreen(BoomboxMenu hopperMenu, Inventory inventory, Component component) {
        super(hopperMenu, inventory, component);
        this.imageHeight = 133;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(BOOMBOX_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }
}
