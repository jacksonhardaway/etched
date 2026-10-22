package gg.moonflower.etched.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.download.SoundSourceManager;
import gg.moonflower.etched.common.component.DiscAppearanceComponent;
import gg.moonflower.etched.common.component.MusicLabelComponent;
import gg.moonflower.etched.common.menu.EtchingMenu;
import gg.moonflower.etched.common.menu.UrlMenu;
import gg.moonflower.etched.common.network.play.ServerboundEtchTrackDataPacket;
import gg.moonflower.etched.common.network.play.SetUrlPacket;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.registry.EtchedComponents;
import gg.moonflower.etched.core.registry.EtchedItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Jackson
 */
public class EtchingScreen extends AbstractContainerScreen<EtchingMenu> implements ContainerListener, UrlMenu {

    private static final ResourceLocation TEXTURE = Etched.etchedPath("textures/gui/container/etching_table.png");
    private static final Component INVALID_URL = Component.translatable("screen." + Etched.MOD_ID + ".etching_table.error.invalid_url");
    private static final Component CANNOT_CREATE = Component.translatable("screen." + Etched.MOD_ID + ".etching_table.error.cannot_create");
    private static final Component CANNOT_CREATE_MISSING_DISC = Component.translatable("screen." + Etched.MOD_ID + ".etching_table.error.cannot_create.missing_disc").withStyle(ChatFormatting.GRAY);
    private static final Component CANNOT_CREATE_MISSING_LABEL = Component.translatable("screen." + Etched.MOD_ID + ".etching_table.error.cannot_create.missing_label").withStyle(ChatFormatting.GRAY);

    private ItemStack discStack;
    private ItemStack labelStack;
    private MusicLabelComponent musicLabel;
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
        this.musicLabel = MusicLabelComponent.DEFAULT;

        this.invalidReason = "";
    }

    @Override
    protected void init() {
        super.init();
        this.url = new EditBox(this.font, this.leftPos + 11, this.topPos + 25, 154, 16, this.url, Component.translatable("container." + Etched.MOD_ID + ".etching_table.url"));
        this.url.setTextColor(-1);
        this.url.setTextColorUneditable(-1);
        this.url.setBordered(false);
        this.url.setMaxLength(32768);
        this.url.setResponder(value -> {
            if (!Objects.equals(this.oldUrl, value) && this.urlTicks <= 0) {
                PacketDistributor.sendToServer(new SetUrlPacket(""));
            }
            this.urlTicks = 10;
        });
        this.url.setCanLoseFocus(true);
        this.addRenderableWidget(this.url);
        this.addRenderableWidget(MusicSearchScreen.createButton(this, List.of(MusicSearchScreen.SOUND_CLOUD, MusicSearchScreen.AUDIUS), this.leftPos + this.imageWidth + 4, this.topPos + 20));
        this.menu.addSlotListener(this);
    }

    @Override
    public void containerTick() {
        if (this.urlTicks > 0) {
            this.urlTicks--;
            if (this.urlTicks <= 0 && !Objects.equals(this.oldUrl, this.url.getValue())) {
                this.oldUrl = this.url.getValue();
                PacketDistributor.sendToServer(new SetUrlPacket(this.url.getValue()));
                resolveTracks(this.url.getValue());
            }
        }
    }

    /**
     * Resolves the tracks for the URL and sends them to the server, which may not be able to reach the music service itself.
     */
    private static void resolveTracks(String url) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null || !connection.hasChannel(ServerboundEtchTrackDataPacket.TYPE) || !SoundSourceManager.isValidUrl(url)) {
            return;
        }

        SoundSourceManager.resolveTracks(url, null, minecraft.getProxy()).whenCompleteAsync((tracks, error) -> {
            if (minecraft.getConnection() != connection) { // Left the server while resolving
                return;
            }
            if (error != null) {
                Etched.LOGGER.warn("Failed to resolve tracks for {}", url, error);
                PacketDistributor.sendToServer(ServerboundEtchTrackDataPacket.failure(url, Objects.requireNonNullElse(error.getMessage(), error.getClass().getSimpleName())));
            } else {
                PacketDistributor.sendToServer(ServerboundEtchTrackDataPacket.success(url, List.of(tracks)));
            }
        }, minecraft);
    }

    @Override
    public void slotChanged(AbstractContainerMenu menu, int slot, ItemStack stack) {
        if (slot == 0) {
            PlayableRecord.getAlbum(stack).ifPresent(track -> this.url.setValue(track.url()));
            this.discStack = stack;
        }

        if (slot == 1) {
            this.musicLabel = stack.getOrDefault(EtchedComponents.MUSIC_LABEL, MusicLabelComponent.DEFAULT);
            this.labelStack = stack;
        }

        boolean displayLabels = !this.discStack.isEmpty() && !menu.getSlot(1).getItem().isEmpty();
        boolean editable = this.discStack.getItem() == EtchedItems.ETCHED_MUSIC_DISC.get() || displayLabels;
        this.url.setEditable(editable);
        this.url.setVisible(editable);
        this.url.setFocused(editable);
        this.setFocused(editable ? this.url : null);

        this.displayLabels = !this.discStack.isEmpty() && !this.labelStack.isEmpty();
    }

    @Override
    public void dataChanged(AbstractContainerMenu abstractContainerMenu, int index, int value) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);

        boolean isEtched = this.discStack.is(EtchedItems.ETCHED_MUSIC_DISC.get());
        List<FormattedCharSequence> reasonLines = new ArrayList<>();
        if (!isEtched && !this.discStack.isEmpty() && this.labelStack.isEmpty()) {
            reasonLines.add(CANNOT_CREATE.getVisualOrderText());
            reasonLines.add(CANNOT_CREATE_MISSING_LABEL.getVisualOrderText());
        } else if (!isEtched && this.discStack.isEmpty() && !this.labelStack.isEmpty()) {
            reasonLines.add(CANNOT_CREATE.getVisualOrderText());
            reasonLines.add(CANNOT_CREATE_MISSING_DISC.getVisualOrderText());
        } else if ((!this.url.getValue().isEmpty() && !TrackData.isValidURL(this.url.getValue())) || !this.invalidReason.isEmpty()) {
            reasonLines.add(INVALID_URL.getVisualOrderText());
            if (!this.invalidReason.isEmpty()) {
                reasonLines.addAll(this.font.split(Component.literal(this.invalidReason).withStyle(ChatFormatting.GRAY), 200));
            }
        }

        if (!reasonLines.isEmpty() && x >= this.leftPos + 83 && x < this.leftPos + 110 && y >= this.topPos + 44 && y < this.topPos + 61) {
            guiGraphics.renderTooltip(this.font, reasonLines, x, y);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if ((!this.url.getValue().isEmpty() && !TrackData.isValidURL(this.url.getValue())) || !this.invalidReason.isEmpty() || (this.discStack.getItem() != EtchedItems.ETCHED_MUSIC_DISC.get() && ((!this.discStack.isEmpty() && this.labelStack.isEmpty()) || (this.discStack.isEmpty() && !this.labelStack.isEmpty())))) {
            guiGraphics.blit(TEXTURE, this.leftPos + 83, this.topPos + 44, 0, 226, 27, 17);
        }

        guiGraphics.blit(TEXTURE, this.leftPos + 9, this.topPos + 21, 0, (this.discStack.getItem() == EtchedItems.ETCHED_MUSIC_DISC.get() || (!this.discStack.isEmpty() && !this.labelStack.isEmpty()) ? 180 : 196), 158, 16);

        if (this.displayLabels) {
            DiscAppearanceComponent.LabelPattern[] patterns = DiscAppearanceComponent.LabelPattern.values();
            for (int index = 0; index < patterns.length; index++) {
                int x = this.leftPos + 46 + (index * 14);
                int y = this.topPos + 65;
                RenderSystem.setShaderTexture(0, TEXTURE);

                int u = index == this.menu.getLabelIndex() ? 14 : mouseX >= x && mouseY >= y && mouseX < x + 14 && mouseY < y + 14 ? 28 : 0;
                guiGraphics.blit(TEXTURE, x, y, u, 212, 14, 14);
                this.renderLabel(guiGraphics, x, y, patterns[index]);
            }
        }
    }

    private void renderLabel(GuiGraphics guiGraphics, int x, int y, DiscAppearanceComponent.LabelPattern pattern) {
        if (this.labelStack.isEmpty() || this.discStack.isEmpty()) {
            return;
        }

        int primaryLabelColor = this.musicLabel.primaryColor();
        int secondaryLabelColor = this.musicLabel.secondaryColor();

        if (pattern.isColorable()) {
            guiGraphics.setColor((float) (primaryLabelColor >> 16 & 255) / 255.0F, (float) (primaryLabelColor >> 8 & 255) / 255.0F, (float) (primaryLabelColor & 255) / 255.0F, 1.0F);
        }

        Pair<ResourceLocation, ResourceLocation> textures = pattern.getTextures();
        guiGraphics.blit(textures.getFirst(), x, y, 14, 14, 1, 1, 14, 14, 16, 16);
        if (pattern.isComplex()) {
            if (pattern.isColorable()) {
                guiGraphics.setColor((float) (secondaryLabelColor >> 16 & 255) / 255.0F, (float) (secondaryLabelColor >> 8 & 255) / 255.0F, (float) (secondaryLabelColor & 255) / 255.0F, 1.0F);
            }

            guiGraphics.blit(textures.getSecond(), x, y, 14, 14, 1, 1, 14, 14, 16, 16);
        }
        guiGraphics.setColor(1F, 1F, 1F, 1F);
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int i) {
        if (this.displayLabels) {
            for (int index = 0; index < DiscAppearanceComponent.LabelPattern.values().length; index++) {
                int x = this.leftPos + 46 + (index * 14);
                int y = this.topPos + 65;

                if (mouseX >= x && mouseY >= y && mouseX < x + 14 && mouseY < y + 14 && this.menu.getLabelIndex() != index) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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

    @Override
    public void setUrl(String url) {
        this.url.setValue(url);
    }
}
