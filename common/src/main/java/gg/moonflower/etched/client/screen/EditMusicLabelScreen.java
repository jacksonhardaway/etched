package gg.moonflower.etched.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.moonflower.etched.common.item.ComplexMusicLabelItem;
import gg.moonflower.etched.common.item.MusicLabelItem;
import gg.moonflower.etched.common.item.SimpleMusicLabelItem;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.ServerboundEditMusicLabelPacket;
import gg.moonflower.etched.core.Etched;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class EditMusicLabelScreen extends Screen {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Etched.MOD_ID, "textures/gui/edit_music_label.png");
    private static final ResourceLocation LABEL = new ResourceLocation(Etched.MOD_ID, "textures/gui/label.png");
    private static final TranslatableComponent TITLE_COMPONENT = new TranslatableComponent("screen.etched.edit_music_label.title");
    private static final TranslatableComponent AUTHOR_COMPONENT = new TranslatableComponent("screen.etched.edit_music_label.author");

    private final Player player;
    private final InteractionHand hand;
    private final ItemStack labelStack;
    private final int imageWidth = 176;
    private final int imageHeight = 139;

    private Button doneButton;
    private EditBox title;
    private EditBox author;

    public EditMusicLabelScreen(Player player, InteractionHand hand, ItemStack stack) {
        super(NarratorChatListener.NO_TITLE);
        this.player = player;
        this.hand = hand;
        this.labelStack = stack;
    }

    @Override
    protected void init() {
        int leftPos = (this.width - this.imageWidth) / 2;
        int topPos = (this.height - this.imageHeight) / 2;

        this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
        this.doneButton = new Button(leftPos, topPos + this.imageHeight + 5, this.imageWidth, 20, CommonComponents.GUI_DONE, button -> {
            this.saveChanges();
            this.minecraft.setScreen(null);
        });
        this.addButton(this.doneButton);

        this.title = new EditBox(this.font, leftPos + 10, topPos + 91, 154, 10, TITLE_COMPONENT);
        this.title.setValue(SimpleMusicLabelItem.getTitle(this.labelStack));
        this.title.setTextColorUneditable(-1);
        this.title.setTextColor(-1);
        this.title.setMaxLength(128);
        this.title.setBordered(false);
        this.title.setCanLoseFocus(true);
        this.title.setFocus(true);
        this.setFocused(this.title);

        this.author = new EditBox(this.font, leftPos + 10, topPos + 121, 154, 10, AUTHOR_COMPONENT);
        this.author.setValue(SimpleMusicLabelItem.getAuthor(this.labelStack));
        this.author.setTextColorUneditable(-1);
        this.author.setTextColor(-1);
        this.author.setMaxLength(128);
        this.author.setBordered(false);
        this.author.setCanLoseFocus(true);

        this.title.setResponder(string -> {
            if ((this.author.getValue().isEmpty() || string.isEmpty()) && this.doneButton.active) {
                this.doneButton.active = false;
            } else if ((!this.author.getValue().isEmpty() && !string.isEmpty()) && !this.doneButton.active) {
                this.doneButton.active = true;
            }
        });
        this.addButton(this.title);

        this.author.setResponder(string -> {
            if ((this.title.getValue().isEmpty() || string.isEmpty()) && this.doneButton.active) {
                this.doneButton.active = false;
            } else if ((!this.title.getValue().isEmpty() && !string.isEmpty()) && !this.doneButton.active) {
                this.doneButton.active = true;
            }
        });
        this.addButton(this.author);
    }

    @Override
    public void resize(Minecraft minecraft, int i, int j) {
        String title = this.title.getValue();
        String author = this.author.getValue();

        boolean titleFocused = this.title.isFocused();
        boolean authorFocused = this.author.isFocused();
        GuiEventListener focused = this.getFocused();

        this.init(minecraft, i, j);
        this.title.setValue(title);
        this.title.setFocus(titleFocused);
        this.author.setValue(author);
        this.author.setFocus(authorFocused);
        this.setFocused(focused);
    }

    @Override
    public void removed() {
        super.removed();
        this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    @Override
    public void tick() {
        this.title.tick();
        this.author.tick();
    }

    protected void renderBg(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        int leftPos = (this.width - this.imageWidth) / 2;
        int topPos = (this.height - this.imageHeight) / 2;

        Minecraft.getInstance().getTextureManager().bind(TEXTURE);
        this.blit(poseStack, leftPos, topPos, 0, 0, this.imageWidth, this.imageHeight);
        this.font.draw(poseStack, TITLE_COMPONENT, leftPos + 7, topPos + 77, 4210752);
        this.font.draw(poseStack, AUTHOR_COMPONENT, leftPos + 7, topPos + 77 + 30, 4210752);

        int primaryLabelColor = 0xFFFFFF;
        int secondaryLabelColor = primaryLabelColor;
        if (this.labelStack.getItem() instanceof MusicLabelItem)  {
            primaryLabelColor = MusicLabelItem.getLabelColor(this.labelStack);
            secondaryLabelColor = primaryLabelColor;
        } else if  (this.labelStack.getItem() instanceof ComplexMusicLabelItem) {
            primaryLabelColor = ComplexMusicLabelItem.getPrimaryColor(this.labelStack);
            secondaryLabelColor = ComplexMusicLabelItem.getSecondaryColor(this.labelStack);
        }

        Minecraft.getInstance().getTextureManager().bind(LABEL);
        RenderSystem.color4f((float) (primaryLabelColor >> 16 & 255) / 255.0F, (float) (primaryLabelColor >> 8 & 255) / 255.0F, (float) (primaryLabelColor & 255) / 255.0F, 1.0F);
        this.blit(poseStack, leftPos, topPos, 0, 0, this.imageWidth, 70);

        RenderSystem.color4f((float) (secondaryLabelColor >> 16 & 255) / 255.0F, (float) (secondaryLabelColor >> 8 & 255) / 255.0F, (float) (secondaryLabelColor & 255) / 255.0F, 1.0F);
        this.blit(poseStack, leftPos, topPos, 0, 70, this.imageWidth, 70);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBg(poseStack, mouseX, mouseY, partialTick);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private void saveChanges() {
        String author = this.author.getValue().trim();
        String title = this.title.getValue().trim();

        SimpleMusicLabelItem.setTitle(this.labelStack, title);
        SimpleMusicLabelItem.setAuthor(this.labelStack, author);

        int slot = this.hand == InteractionHand.MAIN_HAND ? this.player.inventory.selected : 40;
        EtchedMessages.PLAY.sendToServer(new ServerboundEditMusicLabelPacket(slot, author, title));
    }
}
