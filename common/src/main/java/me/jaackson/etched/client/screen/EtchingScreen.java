package me.jaackson.etched.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import me.jaackson.etched.Etched;
import me.jaackson.etched.bridge.NetworkBridge;
import me.jaackson.etched.common.item.EtchedMusicDiscItem;
import me.jaackson.etched.common.menu.EtchingMenu;
import me.jaackson.etched.common.network.ServerboundSetEtcherUrlPacket;
import net.minecraft.client.Minecraft;
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

public class EtchingScreen extends AbstractContainerScreen<EtchingMenu> implements ContainerListener {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Etched.MOD_ID, "textures/gui/container/etching_table.png");

    private EditBox url;
    private boolean displayLabels;

    public EtchingScreen(EtchingMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageHeight = 180;
        this.inventoryLabelY += 14;
    }


    @Override
    protected void init() {
        super.init();
        this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
        this.url = new EditBox(this.font, this.leftPos + 11, this.topPos + 26, 154, 16, new TranslatableComponent("container.repair"));
        this.url.setTextColor(-1);
        this.url.setTextColorUneditable(-1);
        this.url.setBordered(false);
//        this.url.setMaxLength(35); TODO: change to higher number for longer urls
        this.url.setResponder(this::onUrlChanged);
        this.url.setCanLoseFocus(false);
        this.children.add(this.url);
        this.setInitialFocus(this.url);
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
        this.menu.addSlotListener(this);
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
//            Optional<EtchedMusicDiscItem.MusicInfo> info = EtchedMusicDiscItem.getMusic(stack);
//            this.url.setValue(stack.isEmpty() ? "" : info.isPresent() ? info.get().getUrl() : "");
            this.url.setEditable(!stack.isEmpty());
            this.setFocused(this.url);
        }

        if (slot == 1) {
            this.displayLabels = !stack.isEmpty();
        }
    }

    @Override
    public void setContainerData(AbstractContainerMenu abstractContainerMenu, int i, int j) {
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
        this.blit(poseStack, this.leftPos + 9, this.topPos + 21, 0, (this.menu.getSlot(0).hasItem() ? 180 : 196), 158, 16);

        if (this.displayLabels) {
            for (int index = 0; index < 6; index++) {
                int x = this.leftPos + 46 + (index * 14);
                int y = this.topPos + 65;
                this.minecraft.getTextureManager().bind(TEXTURE);

                int v = 212 + (index == this.menu.getLabelIndex() ? 14 : mouseX >= x && mouseY >= y && mouseX < x + 14 && mouseY < y + 14 ? 28 : 0);
                this.blit(poseStack, x, y, 0, v, 14, 14);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int i) {
        if (this.displayLabels) {
            for (int index = 0; index < 6; index++) {
                int x = this.leftPos + 46 + (index * 14);
                int y = this.topPos + 65;

                if (mouseX >= x && mouseY >= y && mouseX < x + 14 && mouseY < y + 14 && this.menu.getLabelIndex() != index) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_SELECT_PATTERN, 1.0F));
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, i);
    }

    private void onUrlChanged(String url) {
        this.url.setTextColor(EtchedMusicDiscItem.isValidURL(url) ? 16777215 : 16733525);
        this.menu.setUrl(url);
        NetworkBridge.sendToServer(new ServerboundSetEtcherUrlPacket(url));
    }
}
