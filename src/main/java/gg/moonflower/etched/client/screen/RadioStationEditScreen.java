package gg.moonflower.etched.client.screen;

import gg.moonflower.etched.common.radio.RadioStation;
import gg.moonflower.etched.core.Etched;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;

public class RadioStationEditScreen extends Screen {

    private final Screen parent;
    private final RadioStation station;
    private final boolean editUrl;
    private final Consumer<RadioStation> onSave;
    private EditBox name;
    private EditBox url;
    private Component status;

    public RadioStationEditScreen(Screen parent, RadioStation station, boolean editUrl, Consumer<RadioStation> onSave) {
        super(Component.translatable("screen." + Etched.MOD_ID + ".radio_station."
                + (editUrl ? (station.isEmpty() ? "add_title" : "edit_title") : "rename_title")));
        this.parent = parent;
        this.station = station;
        this.editUrl = editUrl;
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(this.width - 20, 360);
        int left = (this.width - contentWidth) / 2;
        int top = this.height / 2 - 50;

        this.name = new EditBox(this.font, left, top, contentWidth, 20,
                Component.translatable("screen.etched.radio_station.name"));
        this.name.setMaxLength(RadioStation.MAX_NAME_LENGTH);
        this.name.setValue(this.station.name());
        this.name.setHint(Component.translatable("screen.etched.radio_station.name").withStyle(ChatFormatting.DARK_GRAY));
        this.addRenderableWidget(this.name);

        this.url = new EditBox(this.font, left, top + 28, contentWidth, 20,
                Component.translatable("screen.etched.radio_station.url"));
        this.url.setMaxLength(RadioStation.MAX_URL_LENGTH);
        this.url.setValue(this.station.url());
        this.url.setEditable(this.editUrl);
        this.url.setHint(Component.translatable("screen.etched.radio_station.url").withStyle(ChatFormatting.DARK_GRAY));
        this.addRenderableWidget(this.url);

        int buttonWidth = (contentWidth - 4) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("screen.etched.radio_station.save"), button -> this.save())
                .bounds(left, top + 60, buttonWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
                .bounds(left + contentWidth - buttonWidth, top + 60, buttonWidth, 20)
                .build());
        this.setInitialFocus(this.name);
    }

    private void save() {
        RadioStation updated = new RadioStation(this.name.getValue(), this.url.getValue());
        if (updated.isEmpty()) {
            this.status = Component.translatable("screen.etched.radio_station.url_required");
            return;
        }
        this.onSave.accept(updated);
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW_KEY_ENTER || keyCode == GLFW_KEY_KP_ENTER) {
            this.save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 76, -1);
        if (this.status != null) {
            graphics.drawCenteredString(this.font, this.status, this.width / 2, this.height / 2 + 38, 0xFF5555);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
