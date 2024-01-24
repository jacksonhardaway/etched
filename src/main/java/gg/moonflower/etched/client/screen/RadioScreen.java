package gg.moonflower.etched.client.screen;

import gg.moonflower.etched.common.menu.RadioMenu;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.ServerboundSetUrlPacket;
import gg.moonflower.etched.core.Etched;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * @author Ocelot
 */
public class RadioScreen extends AbstractContainerScreen<RadioMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Etched.MOD_ID, "textures/gui/radio.png");

    private boolean canEdit;
    private EditBox url;

    public RadioScreen(RadioMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageHeight = 39;
    }

    @Override
    protected void init() {
        super.init();
        this.url = new EditBox(this.font, this.leftPos + 10, this.topPos + 21, 154, 16, this.url, Component.translatable("container." + Etched.MOD_ID + ".radio.url"));
        this.url.setTextColor(-1);
        this.url.setTextColorUneditable(-1);
        this.url.setBordered(false);
        this.url.setMaxLength(32500);
        this.url.setVisible(this.canEdit);
        this.url.setCanLoseFocus(false);
        this.addRenderableWidget(this.url);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            EtchedMessages.PLAY.sendToServer(new ServerboundSetUrlPacket(this.url.getValue()));
            this.minecraft.setScreen(null);
        }).bounds((this.width - this.imageWidth) / 2, (this.height - this.imageHeight) / 2 + this.imageHeight + 5, this.imageWidth, 20).build());
    }

    @Override
    public void containerTick() {
        this.url.tick();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 18, 0, this.canEdit ? 39 : 53, 160, 14);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        return this.url.keyPressed(i, j, k) || (this.url.isFocused() && this.url.isVisible() && i != 256) || super.keyPressed(i, j, k);
    }

    public void receiveUrl(String url) {
        this.canEdit = true;
        this.url.setVisible(true);
        this.url.setValue(url);
        this.setFocused(this.url);
        this.url.setFocused(true);
    }
}
