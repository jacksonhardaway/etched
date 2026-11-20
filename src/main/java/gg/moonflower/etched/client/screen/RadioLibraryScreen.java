package gg.moonflower.etched.client.screen;

import gg.moonflower.etched.client.radio.ClientRadioStations;
import gg.moonflower.etched.client.radio.RadioStationLibrary;
import gg.moonflower.etched.common.radio.RadioStation;
import gg.moonflower.etched.core.Etched;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RadioLibraryScreen extends Screen {

    private static final int ROW_HEIGHT = 24;

    private final RadioScreen parent;
    private final boolean managePresets;
    private final RadioStationLibrary library = ClientRadioStations.get();
    private final List<Button> presetButtons = new ArrayList<>();
    private EditBox query;
    private StationList results;
    private Button primaryButton;
    private Button editButton;
    private Button removeButton;
    private Button renamePresetButton;
    private Button clearPresetButton;
    private Component status;
    private int selectedPreset;
    private int lastClickedPreset = -1;
    private long lastPresetClickTime;
    private int statusY;

    public RadioLibraryScreen(RadioScreen parent, boolean managePresets) {
        super(Component.translatable(managePresets
                ? "screen." + Etched.MOD_ID + ".radio_library.manage_title"
                : "screen." + Etched.MOD_ID + ".radio_library.title"));
        this.parent = parent;
        this.managePresets = managePresets;
        if (managePresets) {
            List<RadioStation> presets = parent.getPresets();
            for (int i = 0; i < presets.size(); i++) {
                if (presets.get(i).isEmpty()) {
                    this.selectedPreset = i;
                    break;
                }
            }
        }
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(this.width - 20, 420);
        int left = (this.width - contentWidth) / 2;
        int queryTop = this.managePresets ? 76 : 24;
        String queryText = this.query != null ? this.query.getValue() : "";

        this.presetButtons.clear();
        if (this.managePresets) {
            int presetWidth = (contentWidth - 8) / 3;
            for (int i = 0; i < this.parent.getPresets().size(); i++) {
                int index = i;
                Button button = this.addRenderableWidget(Button.builder(this.presetLabel(i), ignored -> this.selectPreset(index))
                        .bounds(left + (i % 3) * (presetWidth + 4), 24 + (i / 3) * 24, presetWidth, 20)
                        .build());
                this.presetButtons.add(button);
            }
        }

        int browserWidth = 104;
        this.query = new EditBox(this.font, left, queryTop, contentWidth - browserWidth - 4, 20, this.query,
                Component.translatable("screen.etched.radio_library.search"));
        this.query.setMaxLength(128);
        this.query.setValue(queryText);
        this.query.setHint(Component.translatable("screen.etched.radio_library.search").withStyle(ChatFormatting.DARK_GRAY));
        this.query.setResponder(value -> this.refreshResults(null));
        this.addRenderableWidget(this.query);
        this.addRenderableWidget(Button.builder(Component.translatable("screen.etched.radio_library.browser"), button ->
                        this.minecraft.setScreen(new RadioBrowserScreen(this, station -> this.acceptBrowserStation(
                                new RadioStation(station.name(), station.url())))))
                .bounds(left + contentWidth - browserWidth, queryTop, browserWidth, 20)
                .build());

        int listTop = queryTop + 28;
        int controlsHeight = this.managePresets ? 80 : 56;
        int listHeight = Math.min(ROW_HEIGHT * 4 + 4,
                Math.max(ROW_HEIGHT * 2, this.height - listTop - controlsHeight));
        this.results = this.addRenderableWidget(new StationList(this.minecraft, this.width, listHeight, listTop, contentWidth));
        this.refreshResults(null);

        int gap = 4;
        int thirdWidth = (contentWidth - gap * 2) / 3;
        int firstRow = listTop + listHeight + 4;
        Component primaryLabel = this.managePresets ? this.assignmentLabel() : Component.translatable("screen.etched.radio_library.use");
        this.primaryButton = this.addRenderableWidget(Button.builder(primaryLabel, button -> this.useSelected())
                .bounds(left, firstRow, thirdWidth, 20)
                .build());

        int secondRow = firstRow + 24;
        if (this.managePresets) {
            this.renamePresetButton = this.addRenderableWidget(Button.builder(this.renamePresetLabel(), button -> this.renamePreset(this.selectedPreset))
                    .bounds(left + thirdWidth + gap, firstRow, thirdWidth, 20)
                    .build());
            this.clearPresetButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.etched.radio_library.clear_preset"), button -> this.clearPreset())
                    .bounds(left + (thirdWidth + gap) * 2, firstRow, thirdWidth, 20)
                    .build());
            this.editButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.etched.radio_library.edit_station"), button -> this.editSelected())
                    .bounds(left, secondRow, thirdWidth, 20)
                    .build());
            this.removeButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.etched.radio_library.remove_favorite"), button -> this.removeSelected())
                    .bounds(left + thirdWidth + gap, secondRow, thirdWidth, 20)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable("screen.etched.radio_library.add"), button -> this.addManually())
                    .bounds(left + (thirdWidth + gap) * 2, secondRow, thirdWidth, 20)
                    .build());
            int thirdRow = secondRow + 24;
            this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
                    .bounds(left, thirdRow, contentWidth, 20)
                    .build());
            this.statusY = Math.min(this.height - 10, thirdRow + 24);
        } else {
            this.editButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.etched.radio_library.edit_station"), button -> this.editSelected())
                    .bounds(left + thirdWidth + gap, firstRow, thirdWidth, 20)
                    .build());
            this.removeButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.etched.radio_library.remove_favorite"), button -> this.removeSelected())
                    .bounds(left + (thirdWidth + gap) * 2, firstRow, thirdWidth, 20)
                    .build());
            int halfWidth = (contentWidth - gap) / 2;
            this.addRenderableWidget(Button.builder(Component.translatable("screen.etched.radio_library.add"), button -> this.addManually())
                    .bounds(left, secondRow, halfWidth, 20)
                    .build());
            this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
                    .bounds(left + halfWidth + gap, secondRow, halfWidth, 20)
                    .build());
            this.statusY = Math.min(this.height - 10, secondRow + 24);
        }
        this.updateButtons();
        this.setInitialFocus(this.query);
    }

    private void acceptBrowserStation(RadioStation station) {
        if (this.managePresets) {
            if (this.save(() -> this.library.add(station))) {
                this.parent.setPreset(this.selectedPreset, station);
                this.selectNextEmptyPreset();
                this.refreshResults(station.url());
            }
            this.minecraft.setScreen(this);
        } else {
            this.parent.setStation(station);
            this.minecraft.setScreen(this.parent);
        }
    }

    private void useSelected() {
        RadioStation station = this.results.selectedStation();
        if (station == null) {
            return;
        }
        if (this.managePresets) {
            this.parent.setPreset(this.selectedPreset, station);
            this.selectNextEmptyPreset();
            this.updateButtons();
        } else {
            this.parent.setStation(station);
            this.minecraft.setScreen(this.parent);
        }
    }

    private void addManually() {
        this.minecraft.setScreen(new RadioStationEditScreen(this, RadioStation.EMPTY, true, station -> {
            if (this.save(() -> this.library.add(station))) {
                this.refreshResults(station.url());
                if (this.managePresets) {
                    this.parent.setPreset(this.selectedPreset, station);
                    this.selectNextEmptyPreset();
                }
            }
        }));
    }

    private void editSelected() {
        RadioStation station = this.results.selectedStation();
        if (station == null) {
            return;
        }
        this.minecraft.setScreen(new RadioStationEditScreen(this, station, true, updated -> {
            if (this.save(() -> this.library.update(station.url(), updated))) {
                this.refreshResults(updated.url());
            }
        }));
    }

    private void removeSelected() {
        RadioStation station = this.results.selectedStation();
        if (station != null && this.save(() -> this.library.remove(station.url()))) {
            this.refreshResults(null);
        }
    }

    private void clearPreset() {
        this.parent.setPreset(this.selectedPreset, RadioStation.EMPTY);
        this.updateButtons();
    }

    private void selectPreset(int index) {
        long time = Util.getMillis();
        boolean doubleClick = index == this.lastClickedPreset && time - this.lastPresetClickTime < 250;
        this.selectedPreset = index;
        this.lastClickedPreset = index;
        this.lastPresetClickTime = time;
        this.updateButtons();
        if (doubleClick) {
            this.renamePreset(index);
        }
    }

    private void renamePreset(int index) {
        RadioStation preset = this.parent.getPresets().get(index);
        if (!preset.isEmpty()) {
            this.minecraft.setScreen(new RadioStationEditScreen(this, preset, false,
                    renamed -> this.parent.setPreset(index, renamed)));
        }
    }

    private void selectNextEmptyPreset() {
        List<RadioStation> presets = this.parent.getPresets();
        for (int offset = 1; offset <= presets.size(); offset++) {
            int index = (this.selectedPreset + offset) % presets.size();
            if (presets.get(index).isEmpty()) {
                this.selectedPreset = index;
                return;
            }
        }
    }

    private void refreshResults(@Nullable String selectedUrl) {
        if (this.results == null) {
            return;
        }
        String filter = this.query == null ? "" : this.query.getValue().strip().toLowerCase(Locale.ROOT);
        this.results.clearResults();
        for (RadioStation station : this.library.stations()) {
            if (filter.isEmpty()
                    || station.displayName().toLowerCase(Locale.ROOT).contains(filter)
                    || station.url().toLowerCase(Locale.ROOT).contains(filter)) {
                this.results.addStation(new StationList.Entry(this, station));
            }
        }
        if (selectedUrl != null) {
            this.results.selectUrl(selectedUrl);
        }
        this.updateButtons();
    }

    private void updateButtons() {
        boolean hasSelection = this.results != null && this.results.getSelected() != null;
        if (this.primaryButton != null) {
            this.primaryButton.active = hasSelection;
            if (this.managePresets) {
                this.primaryButton.setMessage(this.assignmentLabel());
            }
        }
        if (this.editButton != null) {
            this.editButton.active = hasSelection;
        }
        if (this.removeButton != null) {
            this.removeButton.active = hasSelection;
        }
        boolean hasPreset = this.managePresets && !this.parent.getPresets().get(this.selectedPreset).isEmpty();
        if (this.renamePresetButton != null) {
            this.renamePresetButton.active = hasPreset;
            this.renamePresetButton.setMessage(this.renamePresetLabel());
        }
        if (this.clearPresetButton != null) {
            this.clearPresetButton.active = hasPreset;
        }
        for (int i = 0; i < this.presetButtons.size(); i++) {
            this.presetButtons.get(i).setMessage(this.presetLabel(i));
        }
    }

    private Component presetLabel(int index) {
        Component label = this.parent.presetLabel(index);
        return index == this.selectedPreset
                ? Component.literal("> ").append(label)
                : label;
    }

    private Component assignmentLabel() {
        boolean replacing = !this.parent.getPresets().get(this.selectedPreset).isEmpty();
        return Component.translatable("screen.etched.radio_library." + (replacing ? "replace" : "assign"), this.selectedPreset + 1);
    }

    private Component renamePresetLabel() {
        return Component.translatable("screen.etched.radio_library.rename_preset", this.selectedPreset + 1);
    }

    private boolean save(IoAction action) {
        try {
            action.run();
            this.status = null;
            return true;
        } catch (FileAlreadyExistsException exception) {
            this.status = Component.translatable("screen.etched.radio_library.already_saved");
            return false;
        } catch (IOException exception) {
            this.status = Component.translatable("container.etched.radio.library_error");
            return false;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, -1);
        if (this.status != null) {
            graphics.drawCenteredString(this.font, this.status, this.width / 2, this.statusY, 0xFF5555);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }

    private static class StationList extends ObjectSelectionList<StationList.Entry> {

        private final int rowWidth;

        private StationList(Minecraft minecraft, int width, int height, int y, int rowWidth) {
            super(minecraft, width, height, y, ROW_HEIGHT);
            this.rowWidth = rowWidth;
        }

        private void addStation(Entry entry) {
            this.addEntry(entry);
        }

        private void clearResults() {
            this.clearEntries();
            this.setScrollAmount(0);
        }

        @Nullable
        private RadioStation selectedStation() {
            Entry selected = this.getSelected();
            return selected != null ? selected.station : null;
        }

        private void selectUrl(String url) {
            for (Entry entry : this.children()) {
                if (entry.station.url().equals(url)) {
                    this.setSelected(entry);
                    return;
                }
            }
        }

        @Override
        public int getRowWidth() {
            return this.rowWidth;
        }

        @Override
        public void setSelected(@Nullable Entry selected) {
            super.setSelected(selected);
            if (selected != null) {
                selected.screen.updateButtons();
            }
        }

        private static class Entry extends ObjectSelectionList.Entry<Entry> {

            private final RadioLibraryScreen screen;
            private final RadioStation station;
            private long lastClickTime;

            private Entry(RadioLibraryScreen screen, RadioStation station) {
                this.screen = screen;
                this.station = station;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovering, float partialTick) {
                Minecraft minecraft = Minecraft.getInstance();
                int textWidth = width - 8;
                graphics.drawString(minecraft.font, minecraft.font.plainSubstrByWidth(this.station.displayName(), textWidth),
                        left + 4, top + 2, -1);
                graphics.drawString(minecraft.font, minecraft.font.plainSubstrByWidth(this.station.url(), textWidth),
                        left + 4, top + 12, 0xFFA0A0A0);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                long time = Util.getMillis();
                if (time - this.lastClickTime < 250) {
                    this.screen.results.setSelected(this);
                    this.screen.useSelected();
                    return true;
                }
                this.lastClickTime = time;
                return super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", this.station.displayName());
            }
        }
    }
}
