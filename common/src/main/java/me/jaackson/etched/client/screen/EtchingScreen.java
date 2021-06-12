package me.jaackson.etched.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.jaackson.etched.Etched;
import me.jaackson.etched.EtchedRegistry;
import me.jaackson.etched.bridge.NetworkBridge;
import me.jaackson.etched.common.item.EtchedMusicDiscItem;
import me.jaackson.etched.common.item.MusicLabelItem;
import me.jaackson.etched.common.menu.EtchingMenu;
import me.jaackson.etched.common.network.ServerboundSetEtcherUrlPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;

import static org.lwjgl.opengl.GL11C.GL_EQUAL;

public class EtchingScreen extends AbstractContainerScreen<EtchingMenu> implements ContainerListener {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Etched.MOD_ID, "textures/gui/container/etching_table.png");

    private ItemStack discStack;
    private ItemStack labelStack;
    private EditBox url;
    private boolean displayLabels;

    public EtchingScreen(EtchingMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageHeight = 180;
        this.inventoryLabelY += 14;

        this.discStack = ItemStack.EMPTY;
        this.labelStack = ItemStack.EMPTY;
    }

    @Override
    protected void init() {
        super.init();
        this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
        this.url = new EditBox(this.font, this.leftPos + 11, this.topPos + 25, 154, 16, new TranslatableComponent("container.etched.etching_table"));
        this.url.setTextColor(-1);
        this.url.setTextColorUneditable(-1);
        this.url.setBordered(false);
        this.url.setMaxLength(32500);
        this.url.setResponder(this::onUrlChanged);
        this.url.setCanLoseFocus(true);
        this.children.add(this.url);
        this.menu.addSlotListener(this);
    }

    @Override
    public void resize(Minecraft minecraft, int i, int j) {
        String string = this.url.getValue();
        this.init(minecraft, i, j);
        this.url.setValue(string);
    }

    @Override
    public void removed() {
        super.removed();
        this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i == 256) {
            this.minecraft.player.closeContainer();
        }

        return this.url.keyPressed(i, j, k) || this.url.canConsumeInput() || super.keyPressed(i, j, k);
    }

    @Override
    public void tick() {
        super.tick();
        this.url.tick();
    }

    @Override
    public void refreshContainer(AbstractContainerMenu abstractContainerMenu, NonNullList<ItemStack> nonNullList) {
        this.slotChanged(abstractContainerMenu, 0, abstractContainerMenu.getSlot(0).getItem());
    }

    @Override
    public void slotChanged(AbstractContainerMenu abstractContainerMenu, int slot, ItemStack stack) {
        if (slot == 0) {
            if (this.discStack.isEmpty() && !stack.isEmpty())
                this.url.setValue("");
            EtchedMusicDiscItem.getMusic(stack).ifPresent(musicInfo -> this.url.setValue(musicInfo.getUrl()));
            this.discStack = stack;
        }

        if (slot == 1) {
            this.labelStack = stack;
        }

        boolean editable = this.discStack.getItem() == EtchedRegistry.ETCHED_MUSIC_DISC.get() || (!this.discStack.isEmpty() && !this.labelStack.isEmpty());
        this.url.setEditable(editable);
        this.url.setVisible(editable);
        this.url.setFocus(editable);
        this.setFocused(editable ? this.url : null);

        this.displayLabels = !this.discStack.isEmpty() && !this.labelStack.isEmpty();
    }

    @Override
    public void setContainerData(AbstractContainerMenu abstractContainerMenu, int index, int value) {
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        super.render(poseStack, mouseX, mouseY, partialTicks);
        this.url.render(poseStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(poseStack, mouseX, mouseY);
    }

    @Override
    protected void renderBg(PoseStack poseStack, float f, int mouseX, int mouseY) {
        this.renderBackground(poseStack);

        this.minecraft.getTextureManager().bind(TEXTURE);
        this.blit(poseStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        this.blit(poseStack, this.leftPos + 9, this.topPos + 21, 0, (this.discStack.getItem() == EtchedRegistry.ETCHED_MUSIC_DISC.get() || (!this.discStack.isEmpty() && !this.labelStack.isEmpty()) ? 180 : 196), 158, 16);

        if (this.displayLabels) {
            for (int index = 0; index < 6; index++) {
                int x = this.leftPos + 46 + (index * 14);
                int y = this.topPos + 65;
                this.minecraft.getTextureManager().bind(TEXTURE);

                int v = 212 + (index == this.menu.getLabelIndex() ? 14 : mouseX >= x && mouseY >= y && mouseX < x + 14 && mouseY < y + 14 ? 28 : 0);
                this.blit(poseStack, x, y, 0, v, 14, 14);
                this.renderLabel(poseStack, x, y, index);
            }
        }
    }

    private void renderLabel(PoseStack poseStack, int x, int y, int index) {
        if (this.labelStack.isEmpty() || this.discStack.isEmpty())
            return;

        EtchedMusicDiscItem.LabelPattern pattern = EtchedMusicDiscItem.LabelPattern.values()[index];
        int labelColor = this.labelStack.getItem() instanceof MusicLabelItem ? ((MusicLabelItem) this.labelStack.getItem()).getColor(this.labelStack) : 0xFFFFFF;

        if (pattern.isColorable())
            RenderSystem.color3f((float) (labelColor >> 16 & 255) / 255.0F, (float) (labelColor >> 8 & 255) / 255.0F, (float) (labelColor & 255) / 255.0F);
        RenderSystem.alphaFunc(GL_EQUAL, 1);
        RenderSystem.enableAlphaTest();
        Minecraft.getInstance().getTextureManager().bind(pattern.getTexture());
        Gui.blit(poseStack, x, y, 1, 1, 14, 14, 16, 16);
        RenderSystem.disableAlphaTest();
        RenderSystem.color4f(1F, 1F, 1F, 1F);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int i) {
        if (this.displayLabels) {
            for (int index = 0; index < 6; index++) {
                int x = this.leftPos + 46 + (index * 14);
                int y = this.topPos + 65;

                if (mouseX >= x && mouseY >= y && mouseX < x + 14 && mouseY < y + 14 && this.menu.getLabelIndex() != index) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, i);
    }

    private void onUrlChanged(String url) {
        this.menu.setUrl(url);
        NetworkBridge.sendToServer(new ServerboundSetEtcherUrlPacket(url));
    }
}
