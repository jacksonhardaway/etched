package gg.moonflower.etched.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import gg.moonflower.etched.common.item.ComplexMusicLabelItem;
import gg.moonflower.etched.common.item.MusicLabelItem;
import gg.moonflower.etched.common.item.SimpleMusicLabelItem;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.ServerboundEditMusicLabelPacket;
import gg.moonflower.etched.core.Etched;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class EditMusicLabelScreen extends Screen {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Etched.MOD_ID, "textures/gui/edit_music_label.png");
    private static final ResourceLocation LABEL = new ResourceLocation(Etched.MOD_ID, "textures/gui/label.png");
    private static final Component TITLE_COMPONENT = Component.translatable("screen.etched.edit_music_label.title");
    private static final Component AUTHOR_COMPONENT = Component.translatable("screen.etched.edit_music_label.author");

    private final Player player;
    private final InteractionHand hand;
    private final ItemStack labelStack;
    private final int imageWidth = 176;
    private final int imageHeight = 139;

    private Button doneButton;
    private EditBox title;
    private EditBox author;

    public EditMusicLabelScreen(Player player, InteractionHand hand, ItemStack stack) {
        super(TITLE_COMPONENT);
        this.player = player;
        this.hand = hand;
        this.labelStack = stack;
    }

    @Override
    protected void init() {
        int leftPos = (this.width - this.imageWidth) / 2;
        int topPos = (this.height - this.imageHeight) / 2;

        this.doneButton = Button.builder(CommonComponents.GUI_DONE, button -> {
            this.saveChanges();
            this.minecraft.setScreen(null);
        }).bounds(leftPos, topPos + this.imageHeight + 5, this.imageWidth, 20).build();
        this.addRenderableWidget(this.doneButton);

        this.title = new EditBox(this.font, leftPos + 10, topPos + 91, 154, 10, TITLE_COMPONENT);
        this.title.setValue(SimpleMusicLabelItem.getTitle(this.labelStack));
        this.title.setTextColorUneditable(-1);
        this.title.setTextColor(-1);
        this.title.setMaxLength(128);
        this.title.setBordered(false);
        this.title.setCanLoseFocus(true);
        this.title.setFocused(true);
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
        this.addRenderableWidget(this.title);

        this.author.setResponder(string -> {
            if ((this.title.getValue().isEmpty() || string.isEmpty()) && this.doneButton.active) {
                this.doneButton.active = false;
            } else if ((!this.title.getValue().isEmpty() && !string.isEmpty()) && !this.doneButton.active) {
                this.doneButton.active = true;
            }
        });
        this.addRenderableWidget(this.author);
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
        this.title.setFocused(titleFocused);
        this.author.setValue(author);
        this.author.setFocused(authorFocused);
        this.setFocused(focused);
    }

    @Override
    public void tick() {
        this.title.tick();
        this.author.tick();
    }

    protected void renderBg(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int leftPos = (this.width - this.imageWidth) / 2;
        int topPos = (this.height - this.imageHeight) / 2;

        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, this.imageWidth, this.imageHeight);
        graphics.drawString(this.font, TITLE_COMPONENT, leftPos + 7, topPos + 77, 4210752);
        graphics.drawString(this.font, AUTHOR_COMPONENT, leftPos + 7, topPos + 77 + 30, 4210752);

        int primaryLabelColor = 0xFFFFFF;
        int secondaryLabelColor = primaryLabelColor;
        if (this.labelStack.getItem() instanceof MusicLabelItem) {
            primaryLabelColor = MusicLabelItem.getLabelColor(this.labelStack);
            secondaryLabelColor = primaryLabelColor;
        } else if (this.labelStack.getItem() instanceof ComplexMusicLabelItem) {
            primaryLabelColor = ComplexMusicLabelItem.getPrimaryColor(this.labelStack);
            secondaryLabelColor = ComplexMusicLabelItem.getSecondaryColor(this.labelStack);
        }

        RenderSystem.setShaderColor((float) (primaryLabelColor >> 16 & 255) / 255.0F, (float) (primaryLabelColor >> 8 & 255) / 255.0F, (float) (primaryLabelColor & 255) / 255.0F, 1.0F);
        graphics.blit(LABEL, leftPos, topPos, 0, 0, this.imageWidth, 70);

        RenderSystem.setShaderColor((float) (secondaryLabelColor >> 16 & 255) / 255.0F, (float) (secondaryLabelColor >> 8 & 255) / 255.0F, (float) (secondaryLabelColor & 255) / 255.0F, 1.0F);
        graphics.blit(LABEL, leftPos, topPos, 0, 70, this.imageWidth, 70);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBg(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void saveChanges() {
        String author = this.author.getValue().trim();
        String title = this.title.getValue().trim();

        SimpleMusicLabelItem.setTitle(this.labelStack, title);
        SimpleMusicLabelItem.setAuthor(this.labelStack, author);

        int slot = this.hand == InteractionHand.MAIN_HAND ? this.player.getInventory().selected : 40;
        EtchedMessages.PLAY.sendToServer(new ServerboundEditMusicLabelPacket(slot, author, title));
    }
}
