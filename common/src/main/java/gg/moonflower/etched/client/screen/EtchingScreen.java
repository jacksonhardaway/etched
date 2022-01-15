package gg.moonflower.etched.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.moonflower.etched.common.item.EtchedMusicDiscItem;
import gg.moonflower.etched.common.item.MusicLabelItem;
import gg.moonflower.etched.common.menu.EtchingMenu;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.ServerboundSetEtchingUrlPacket;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.registry.EtchedItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.opengl.GL11C.GL_EQUAL;

/**
 * @author Jackson
 */
public class EtchingScreen extends AbstractContainerScreen<EtchingMenu> implements ContainerListener {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Etched.MOD_ID, "textures/gui/container/etching_table.png");

    private ItemStack discStack;
    private ItemStack labelStack;
    private EditBox url;
    private int urlTicks;
    private String oldUrl;
    private String invalidReason;
    private boolean displayLabels;

    public EtchingScreen(EtchingMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageHeight = 180;
        this.inventoryLabelY += 14;

        this.discStack = ItemStack.EMPTY;
        this.labelStack = ItemStack.EMPTY;

        this.invalidReason = "";
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
        this.url.setResponder(s -> {
            if (!Objects.equals(this.oldUrl, s) && this.urlTicks <= 0)
                EtchedMessages.PLAY.sendToServer(new ServerboundSetEtchingUrlPacket(""));
            this.urlTicks = 8;
        });
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
    public void tick() {
        super.tick();
        this.url.tick();
        if (this.urlTicks > 0) {
            this.urlTicks--;
            if (this.urlTicks <= 0 && !Objects.equals(this.oldUrl, this.url.getValue())) {
                this.oldUrl = this.url.getValue();
                EtchedMessages.PLAY.sendToServer(new ServerboundSetEtchingUrlPacket(this.url.getValue()));
            }
        }
    }

    @Override
    public void refreshContainer(AbstractContainerMenu abstractContainerMenu, NonNullList<ItemStack> nonNullList) {
        this.slotChanged(abstractContainerMenu, 0, abstractContainerMenu.getSlot(0).getItem());
        this.slotChanged(abstractContainerMenu, 1, abstractContainerMenu.getSlot(1).getItem());
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

        boolean editable = this.discStack.getItem() == EtchedItems.ETCHED_MUSIC_DISC.get() || (!this.discStack.isEmpty() && !this.labelStack.isEmpty());
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
    protected void renderTooltip(PoseStack poseStack, int mouseX, int mouseY) {
        super.renderTooltip(poseStack, mouseX, mouseY);

        boolean isEtched = this.discStack.getItem() == EtchedItems.ETCHED_MUSIC_DISC.get();
        List<FormattedCharSequence> reasonLines = new ArrayList<>();
        if (!isEtched && !this.discStack.isEmpty() && this.labelStack.isEmpty()) {
            reasonLines.add(new TranslatableComponent("screen." + Etched.MOD_ID + ".etching_table.error.missing_label").getVisualOrderText());
        } else if (!isEtched && this.discStack.isEmpty() && !this.labelStack.isEmpty()) {
            reasonLines.add(new TranslatableComponent("screen." + Etched.MOD_ID + ".etching_table.error.missing_disc").getVisualOrderText());
        } else if ((!this.url.getValue().isEmpty() && !EtchedMusicDiscItem.isValidURL(this.url.getValue())) || !this.invalidReason.isEmpty()) {
            reasonLines.add(new TranslatableComponent("screen." + Etched.MOD_ID + ".etching_table.error.invalid_url").getVisualOrderText());
            if (!this.invalidReason.isEmpty())
                reasonLines.addAll(this.font.split(new TextComponent(this.invalidReason).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), 200));
        }

        if (mouseX >= this.leftPos + 83 && mouseX < this.leftPos + 110 && mouseY >= this.topPos + 44 && mouseY < this.topPos + 61) {
            this.renderTooltip(poseStack, reasonLines, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(PoseStack poseStack, float f, int mouseX, int mouseY) {
        this.renderBackground(poseStack);

        this.minecraft.getTextureManager().bind(TEXTURE);
        this.blit(poseStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if ((!this.url.getValue().isEmpty() && !EtchedMusicDiscItem.isValidURL(this.url.getValue())) || !this.invalidReason.isEmpty() || (this.discStack.getItem() != EtchedItems.ETCHED_MUSIC_DISC.get() && ((!this.discStack.isEmpty() && this.labelStack.isEmpty()) || (this.discStack.isEmpty() && !this.labelStack.isEmpty()))))
            this.blit(poseStack, this.leftPos + 83, this.topPos + 44, 0, 226, 27, 17);

        this.blit(poseStack, this.leftPos + 9, this.topPos + 21, 0, (this.discStack.getItem() == EtchedItems.ETCHED_MUSIC_DISC.get() || (!this.discStack.isEmpty() && !this.labelStack.isEmpty()) ? 180 : 196), 158, 16);

        if (this.displayLabels) {
            for (int index = 0; index < 6; index++) {
                int x = this.leftPos + 46 + (index * 14);
                int y = this.topPos + 65;
                this.minecraft.getTextureManager().bind(TEXTURE);

                int u = index == this.menu.getLabelIndex() ? 14 : mouseX >= x && mouseY >= y && mouseX < x + 14 && mouseY < y + 14 ? 28 : 0;
                this.blit(poseStack, x, y, u, 212, 14, 14);
                this.renderLabel(poseStack, x, y, index);
            }
        }
    }

    private void renderLabel(PoseStack poseStack, int x, int y, int index) {
        if (this.labelStack.isEmpty() || this.discStack.isEmpty())
            return;

        EtchedMusicDiscItem.LabelPattern pattern = EtchedMusicDiscItem.LabelPattern.values()[index];
        int primaryLabelColor = MusicLabelItem.getPrimaryColor(this.labelStack);
        int secondaryLabelColor = MusicLabelItem.getSecondaryColor(this.labelStack);

        if (pattern.isColorable())
            RenderSystem.color3f((float) (primaryLabelColor >> 16 & 255) / 255.0F, (float) (primaryLabelColor >> 8 & 255) / 255.0F, (float) (primaryLabelColor & 255) / 255.0F);

        Pair<ResourceLocation, ResourceLocation> textures = pattern.getTextures();
        Minecraft.getInstance().getTextureManager().bind(textures.getLeft());
        Gui.blit(poseStack, x, y, 1, 1, 14, 14, 16, 16);
        if (!pattern.isSimple()) {
            if (pattern.isColorable())
                RenderSystem.color3f((float) (secondaryLabelColor >> 16 & 255) / 255.0F, (float) (secondaryLabelColor >> 8 & 255) / 255.0F, (float) (secondaryLabelColor & 255) / 255.0F);

            Minecraft.getInstance().getTextureManager().bind(textures.getRight());
            Gui.blit(poseStack, x, y, 1, 1, 14, 14, 16, 16);
        }
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

    @Override
    public boolean keyPressed(int i, int j, int k) {
        return this.url.keyPressed(i, j, k) || (this.url.isFocused() && this.url.isVisible() && i != 256) || super.keyPressed(i, j, k);
    }

    public void setReason(String exception) {
        this.invalidReason = exception;
    }
}
