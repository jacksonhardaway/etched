package gg.moonflower.etched.client.screen;

import gg.moonflower.etched.client.radio.ClientRadioStations;
import gg.moonflower.etched.client.radio.RadioStationLibrary;
import gg.moonflower.etched.common.menu.RadioMenu;
import gg.moonflower.etched.common.menu.UrlMenu;
import gg.moonflower.etched.common.network.play.SetRadioPresetPacket;
import gg.moonflower.etched.common.network.play.SetUrlPacket;
import gg.moonflower.etched.common.radio.RadioStation;
import gg.moonflower.etched.core.Etched;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;

/**
 * @author Ocelot
 */
public class RadioScreen extends AbstractContainerScreen<RadioMenu> implements UrlMenu {

    private static final ResourceLocation TEXTURE = Etched.etchedPath("textures/gui/radio.png");

    private final List<RadioStation> presets;
    private EditBox url;
    private Button favoriteButton;
    private RadioStation selectedStation = RadioStation.EMPTY;
    private Component status;

    public RadioScreen(RadioMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageWidth = 232;
        this.imageHeight = 170;
        this.presets = new ArrayList<>(menu.getInitialPresets());
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos -= 24;
        int halfWidth = (this.imageWidth - 4) / 2;
        String urlText = this.url != null ? this.url.getValue() : this.menu.getInitialUrl();
        this.url = new EditBox(this.font, this.leftPos + 10, this.topPos + 21, this.imageWidth - 22, 16, null,
                Component.translatable("container." + Etched.MOD_ID + ".radio.url"));
        this.url.setMaxLength(RadioStation.MAX_URL_LENGTH);
        this.url.setValue(urlText);
        this.url.setTextColor(-1);
        this.url.setTextColorUneditable(-1);
        this.url.setBordered(false);
        this.url.setResponder(value -> this.updateFavoriteButton());
        this.setFocused(this.url);
        this.addRenderableWidget(this.url);

        this.favoriteButton = this.addRenderableWidget(Button.builder(CommonComponents.EMPTY, button -> this.toggleFavorite())
                .bounds(this.leftPos + this.imageWidth - 22, this.topPos + 40, 22, 20)
                .build());

        int presetWidth = (this.imageWidth - 8) / 3;
        for (int i = 0; i < this.presets.size(); i++) {
            int index = i;
            RadioStation preset = this.presets.get(i);
            Button button = this.addRenderableWidget(Button.builder(this.presetLabel(i), ignored -> this.usePreset(index))
                    .bounds(this.leftPos + (i % 3) * (presetWidth + 4), this.topPos + 64 + (i / 3) * 24,
                            presetWidth, 20)
                    .build());
            button.active = !preset.isEmpty();
        }

        this.addRenderableWidget(Button.builder(Component.translatable("container.etched.radio.choose_station"), button ->
                        Minecraft.getInstance().setScreen(new RadioLibraryScreen(this, false)))
                .bounds(this.leftPos, this.topPos + 112, halfWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("container.etched.radio.manage_presets"), button ->
                        Minecraft.getInstance().setScreen(new RadioLibraryScreen(this, true)))
                .bounds(this.leftPos + this.imageWidth - halfWidth, this.topPos + 112, halfWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.applyAndClose())
                .bounds(this.leftPos, this.topPos + 136, this.imageWidth, 20)
                .build());
        this.updateFavoriteButton();
    }

    private void applyAndClose() {
        PacketDistributor.sendToServer(new SetUrlPacket(this.url.getValue()));
        Minecraft.getInstance().player.closeContainer();
    }

    private void toggleFavorite() {
        RadioStation station = this.currentStation();
        if (station.isEmpty()) {
            return;
        }

        RadioStationLibrary library = ClientRadioStations.get();
        try {
            if (library.containsUrl(station.url())) {
                library.remove(station.url());
            } else {
                library.add(new RadioStation(station.displayName(), station.url()));
            }
            this.status = null;
        } catch (IOException exception) {
            this.status = Component.translatable("container.etched.radio.library_error");
        }
        this.updateFavoriteButton();
    }

    private void updateFavoriteButton() {
        if (this.favoriteButton == null || this.url == null) {
            return;
        }
        String currentUrl = this.url.getValue();
        this.favoriteButton.active = !currentUrl.isBlank();
        this.favoriteButton.setMessage(Component.literal(ClientRadioStations.get().containsUrl(currentUrl) ? "★" : "☆"));
    }

    private void usePreset(int index) {
        RadioStation preset = this.presets.get(index);
        if (preset.isEmpty()) {
            return;
        }
        this.setStation(preset);
        PacketDistributor.sendToServer(new SetRadioPresetPacket(index, preset, true));
    }

    Component presetLabel(int index) {
        RadioStation preset = this.presets.get(index);
        return preset.isEmpty()
                ? Component.translatable("container.etched.radio.preset", index + 1)
                : Component.literal(preset.displayName());
    }

    List<RadioStation> getPresets() {
        return List.copyOf(this.presets);
    }

    void setPreset(int index, RadioStation station) {
        if (index < 0 || index >= this.presets.size()) {
            return;
        }
        this.presets.set(index, station);
        PacketDistributor.sendToServer(new SetRadioPresetPacket(index, station, false));
    }

    void setStation(RadioStation station) {
        this.selectedStation = station;
        this.url.setValue(station.url());
        this.updateFavoriteButton();
    }

    RadioStation currentStation() {
        String currentUrl = this.url != null ? this.url.getValue().strip() : this.menu.getInitialUrl();
        if (currentUrl.isEmpty()) {
            return RadioStation.EMPTY;
        }
        if (currentUrl.equals(this.selectedStation.url())) {
            return this.selectedStation;
        }
        for (RadioStation preset : this.presets) {
            if (currentUrl.equals(preset.url())) {
                return preset;
            }
        }
        return ClientRadioStations.get().findByUrl(currentUrl).orElseGet(() -> new RadioStation("", currentUrl));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(this.leftPos, this.topPos + 39, this.leftPos + this.imageWidth,
                this.topPos + this.imageHeight, 0xC0101010);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, this.imageWidth, 39,
                0, 0, 176, 39, 256, 256);
        guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 18, this.imageWidth - 16, 14,
                0, 39, 160, 14, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        RadioStation current = this.currentStation();
        Component name = current.isEmpty()
                ? Component.translatable("container.etched.radio.no_station")
                : Component.literal(current.displayName());
        String currentText = Component.translatable("container.etched.radio.current_station", name).getString();
        guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(currentText, this.imageWidth - 30),
                4, 46, 0xFFFFFF, false);
        if (this.status != null) {
            guiGraphics.drawCenteredString(this.font, this.status, this.imageWidth / 2, 160, 0xFF5555);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.url.keyPressed(keyCode, scanCode, modifiers)
                || (this.url.isFocused() && this.url.isVisible() && keyCode != GLFW_KEY_ESCAPE)
                || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void setUrl(String url) {
        this.setStation(new RadioStation("", url));
    }
}
