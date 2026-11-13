package gg.moonflower.etched.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

public class RadioSettingsScreen extends Screen {

    private final Screen parent;
    private final BiConsumer<Integer, Integer> onApply;
    private int volumePercent;
    private int range;
    private VolumeSlider volumeSlider;
    private RangeSlider rangeSlider;

    public RadioSettingsScreen(Screen parent, int volumePercent, int range, BiConsumer<Integer, Integer> onApply) {
        super(Component.translatable("container.etched.radio.settings.title"));
        this.parent = parent;
        this.volumePercent = volumePercent;
        this.range = range;
        this.onApply = onApply;
    }

    @Override
    protected void init() {
        int left = (this.width - 220) / 2;
        int top = this.height / 2 - 58;

        this.volumeSlider = this.addRenderableWidget(new VolumeSlider(left, top, 220, 20, this.volumePercent, value -> {
            this.volumePercent = value;
            this.applyPreview();
        }));
        this.rangeSlider = this.addRenderableWidget(new RangeSlider(left, top + 28, 220, 20, this.range, value -> {
            this.range = value;
            this.applyPreview();
        }));

        this.addRenderableWidget(Button.builder(Component.translatable("container.etched.radio.settings.reset"), button -> {
            this.volumeSlider.setVolumePercent(50, false);
            this.rangeSlider.setRange(32, false);
            this.volumePercent = 50;
            this.range = 32;
            this.applyPreview();
        }).bounds(left, top + 64, 106, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            this.volumePercent = this.volumeSlider.getVolumePercent();
            this.range = this.rangeSlider.getRange();
            this.onApply.accept(this.volumePercent, this.range);
            this.minecraft.setScreen(this.parent);
        }).bounds(left + 114, top + 64, 106, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 84, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.translatable("container.etched.radio.settings.hint"),
                this.width / 2, this.height / 2 + 40, 0xA0A0A0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private void applyPreview() {
        if (this.volumeSlider != null && this.rangeSlider != null) {
            this.onApply.accept(this.volumePercent, this.range);
        }
    }

    private static class VolumeSlider extends AbstractSliderButton {

        private int volumePercent;
        private final IntConsumer onChange;

        private VolumeSlider(int x, int y, int width, int height, int volumePercent, IntConsumer onChange) {
            super(x, y, width, height, CommonComponents.EMPTY, volumePercent / 100.0D);
            this.volumePercent = volumePercent;
            this.onChange = onChange;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("container.etched.radio.settings.volume", this.volumePercent));
        }

        @Override
        protected void applyValue() {
            int newVolume = Math.max(0, Math.min(100, (int) Math.round(this.value * 20.0D) * 5));
            if (newVolume == this.volumePercent) {
                return;
            }
            this.volumePercent = newVolume;
            this.value = this.volumePercent / 100.0D;
            this.updateMessage();
            this.onChange.accept(this.volumePercent);
        }

        private int getVolumePercent() {
            return this.volumePercent;
        }

        private void setVolumePercent(int volumePercent, boolean notify) {
            this.volumePercent = volumePercent;
            this.value = volumePercent / 100.0D;
            this.updateMessage();
            if (notify) {
                this.onChange.accept(this.volumePercent);
            }
        }
    }

    private static class RangeSlider extends AbstractSliderButton {

        private int range;
        private final IntConsumer onChange;

        private RangeSlider(int x, int y, int width, int height, int range, IntConsumer onChange) {
            super(x, y, width, height, CommonComponents.EMPTY, (range - 4) / 28.0D);
            this.range = range;
            this.onChange = onChange;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("container.etched.radio.settings.range", this.range));
        }

        @Override
        protected void applyValue() {
            int newRange = Math.max(4, Math.min(32, 4 + (int) Math.round(this.value * 28.0D)));
            if (newRange == this.range) {
                return;
            }
            this.range = newRange;
            this.value = (this.range - 4) / 28.0D;
            this.updateMessage();
            this.onChange.accept(this.range);
        }

        private int getRange() {
            return this.range;
        }

        private void setRange(int range, boolean notify) {
            this.range = range;
            this.value = (range - 4) / 28.0D;
            this.updateMessage();
            if (notify) {
                this.onChange.accept(this.range);
            }
        }
    }
}
