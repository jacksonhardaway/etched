package gg.moonflower.etched.client.screen;

import gg.moonflower.etched.client.search.RadioBrowserClient;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;

/**
 * Searches Radio Browser and copies the selected stream URL into the radio screen.
 */
public class RadioBrowserScreen extends Screen {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Component TITLE = Component.translatable("screen." + Etched.MOD_ID + ".radio_browser.title");
    private static final Component QUERY = Component.translatable("screen." + Etched.MOD_ID + ".radio_browser.query");
    private static final Component SEARCH = Component.translatable("screen." + Etched.MOD_ID + ".radio_browser.search");
    private static final Component SELECT = Component.translatable("screen." + Etched.MOD_ID + ".radio_browser.select");
    private static final Component SEARCHING = Component.translatable("screen." + Etched.MOD_ID + ".radio_browser.searching").withStyle(ChatFormatting.GRAY);
    private static final Component NO_RESULTS = Component.translatable("screen." + Etched.MOD_ID + ".radio_browser.no_results").withStyle(ChatFormatting.GRAY);
    private static final Component ENTER_QUERY = Component.translatable("screen." + Etched.MOD_ID + ".radio_browser.enter_query").withStyle(ChatFormatting.GRAY);
    private static final int ROW_HEIGHT = 24;

    private final RadioScreen parent;
    private final List<RadioBrowserClient.Station> stations = new ArrayList<>();
    private EditBox query;
    private Button searchButton;
    private Button selectButton;
    private ResultList results;
    private int searchId;
    private CompletableFuture<?> searchTask;
    private RadioBrowserClient.RequestControl searchControl;
    @Nullable
    private Component status;

    public RadioBrowserScreen(RadioScreen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(this.width - 20, 360);
        int left = (this.width - contentWidth) / 2;
        int searchWidth = 84;
        String queryText = this.query != null ? this.query.getValue() : "";

        this.query = new EditBox(this.font, left, 24, contentWidth - searchWidth - 4, 20, this.query, QUERY);
        this.query.setMaxLength(128);
        this.query.setValue(queryText);
        this.query.setHint(QUERY.copy().withStyle(ChatFormatting.DARK_GRAY));
        this.addRenderableWidget(this.query);

        this.searchButton = this.addRenderableWidget(Button.builder(SEARCH, button -> this.search())
                .bounds(left + contentWidth - searchWidth, 24, searchWidth, 20)
                .build());

        this.results = this.addRenderableWidget(new ResultList(this.minecraft, this.width, this.height - 86, 52, contentWidth));
        this.stations.forEach(station -> this.results.addResult(new ResultList.Entry(this, station)));

        int buttonWidth = (contentWidth - 4) / 2;
        this.selectButton = this.addRenderableWidget(Button.builder(SELECT, button -> this.selectResult())
                .bounds(left, this.height - 28, buttonWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
                .bounds(left + contentWidth - buttonWidth, this.height - 28, buttonWidth, 20)
                .build());
        this.updateSelectButton();
        this.setInitialFocus(this.query);
    }

    private void search() {
        String text = this.query.getValue().strip();
        if (text.length() < 2) {
            this.cancelSearch();
            this.stations.clear();
            this.results.clearResults();
            this.status = ENTER_QUERY;
            this.updateSelectButton();
            return;
        }

        this.cancelSearch();
        int id = ++this.searchId;
        this.stations.clear();
        this.results.clearResults();
        this.status = SEARCHING;
        this.searchButton.active = false;
        this.updateSelectButton();

        Proxy proxy = Minecraft.getInstance().getProxy();
        RadioBrowserClient.RequestControl control = new RadioBrowserClient.RequestControl();
        this.searchControl = control;
        this.searchTask = CompletableFuture.supplyAsync(() -> {
            try {
                return RadioBrowserClient.search(text, proxy, control);
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        }, Util.nonCriticalIoPool()).whenCompleteAsync((found, error) -> {
            if (this.searchControl == control) {
                this.searchControl = null;
            }
            if (id != this.searchId || this.minecraft.screen != this) {
                return;
            }

            this.searchButton.active = true;
            if (error != null) {
                Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                LOGGER.error("Failed to search Radio Browser for '{}'", text, cause);
                this.status = Component.translatable(
                        "screen." + Etched.MOD_ID + ".radio_browser.error",
                        Objects.requireNonNullElse(cause.getMessage(), cause.getClass().getSimpleName())
                ).withStyle(ChatFormatting.RED);
                return;
            }

            this.stations.addAll(found);
            found.forEach(station -> this.results.addResult(new ResultList.Entry(this, station)));
            this.status = found.isEmpty() ? NO_RESULTS : null;
        }, this.minecraft);
    }

    private void updateSelectButton() {
        this.selectButton.active = this.results.getSelected() != null;
    }

    private void selectResult() {
        ResultList.Entry entry = this.results.getSelected();
        if (entry == null) {
            return;
        }

        Proxy proxy = Minecraft.getInstance().getProxy();
        CompletableFuture.runAsync(() -> RadioBrowserClient.registerClick(entry.station, proxy), Util.nonCriticalIoPool());
        this.minecraft.setScreen(this.parent);
        this.parent.setUrl(entry.station.url());
    }

    private void cancelSearch() {
        this.searchId++;
        if (this.searchControl != null) {
            this.searchControl.cancel();
            this.searchControl = null;
        }
        if (this.searchTask != null) {
            this.searchTask.cancel(false);
            this.searchTask = null;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.query.isFocused() && (keyCode == GLFW_KEY_ENTER || keyCode == GLFW_KEY_KP_ENTER)) {
            this.search();
            return true;
        }
        if (this.results.isFocused() && this.results.getSelected() != null && (keyCode == GLFW_KEY_ENTER || keyCode == GLFW_KEY_KP_ENTER)) {
            this.selectResult();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, -1);
        if (this.status != null) {
            guiGraphics.drawCenteredString(this.font, this.status, this.width / 2, this.height / 2, -1);
        }
    }

    @Override
    public void onClose() {
        this.cancelSearch();
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void removed() {
        this.cancelSearch();
    }

    private static class ResultList extends ObjectSelectionList<ResultList.Entry> {

        private final int rowWidth;

        private ResultList(Minecraft minecraft, int width, int height, int y, int rowWidth) {
            super(minecraft, width, height, y, ROW_HEIGHT);
            this.rowWidth = rowWidth;
        }

        private void addResult(Entry entry) {
            this.addEntry(entry);
        }

        private void clearResults() {
            this.clearEntries();
            this.setScrollAmount(0);
        }

        @Override
        public int getRowWidth() {
            return this.rowWidth;
        }

        @Override
        public void setSelected(@Nullable Entry selected) {
            super.setSelected(selected);
            if (selected != null) {
                selected.screen.updateSelectButton();
            }
        }

        private static class Entry extends ObjectSelectionList.Entry<Entry> {

            private final RadioBrowserScreen screen;
            private final RadioBrowserClient.Station station;
            private long lastClickTime;

            private Entry(RadioBrowserScreen screen, RadioBrowserClient.Station station) {
                this.screen = screen;
                this.station = station;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                Minecraft minecraft = Minecraft.getInstance();
                int textWidth = width - 8;
                guiGraphics.drawString(minecraft.font, minecraft.font.plainSubstrByWidth(this.station.name(), textWidth), left + 4, top + 2, -1);
                guiGraphics.drawString(minecraft.font, minecraft.font.plainSubstrByWidth(this.station.details(), textWidth), left + 4, top + 12, 0xFFA0A0A0);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                long time = Util.getMillis();
                if (time - this.lastClickTime < 250) {
                    this.screen.results.setSelected(this);
                    this.screen.selectResult();
                    return true;
                }
                this.lastClickTime = time;
                return super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", this.station.name());
            }
        }
    }
}
