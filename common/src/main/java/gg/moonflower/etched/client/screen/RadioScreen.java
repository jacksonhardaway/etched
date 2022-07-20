package gg.moonflower.etched.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.moonflower.etched.common.menu.RadioMenu;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.ServerboundSetUrlPacket;
import gg.moonflower.etched.core.Etched;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
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
        this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
        this.url = new EditBox(this.font, this.leftPos + 10, this.topPos + 21, 154, 16, this.url, new TranslatableComponent("container." + Etched.MOD_ID + ".radio.url"));
        this.url.setTextColor(-1);
        this.url.setTextColorUneditable(-1);
        this.url.setBordered(false);
        this.url.setMaxLength(32500);
        this.url.setVisible(this.canEdit);
        this.url.setCanLoseFocus(false);
        this.addRenderableWidget(this.url);
        this.addRenderableWidget(new Button((this.width - this.imageWidth) / 2, (this.height - this.imageHeight) / 2 + this.imageHeight + 5, this.imageWidth, 20, CommonComponents.GUI_DONE, button -> {
            EtchedMessages.PLAY.sendToServer(new ServerboundSetUrlPacket(this.url.getValue()));
            this.minecraft.setScreen(null);
        }));
    }

    @Override
    public void removed() {
        super.removed();
        this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    @Override
    public void containerTick() {
        this.url.tick();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderBg(PoseStack poseStack, float f, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        this.blit(poseStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        this.blit(poseStack, this.leftPos + 8, this.topPos + 18, 0, this.canEdit ? 39 : 53, 160, 14);
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        this.font.draw(poseStack, this.title, (float) this.titleLabelX, (float) this.titleLabelY, 4210752);
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
        this.url.setFocus(true);
    }
}
