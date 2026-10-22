package gg.moonflower.etched.client.screen;

import gg.moonflower.etched.client.search.MusicSearchResult;
import gg.moonflower.etched.client.search.AudiusSearchSource;
import gg.moonflower.etched.client.search.MusicSearchSource;
import gg.moonflower.etched.client.search.RadioBrowserSearchSource;
import gg.moonflower.etched.client.search.SoundCloudSearchSource;
import gg.moonflower.etched.common.menu.UrlMenu;
import gg.moonflower.etched.core.Etched;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;

/**
 * Lets the player search for music and puts the chosen URL into the screen that opened it.
 */
public class MusicSearchScreen extends Screen {

    public static final MusicSearchSource RADIO_BROWSER = new RadioBrowserSearchSource();
    public static final MusicSearchSource SOUND_CLOUD = new SoundCloudSearchSource();
    public static final MusicSearchSource AUDIUS = new AudiusSearchSource();

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation SEARCH_SPRITE = ResourceLocation.withDefaultNamespace("icon/search");
    private static final Component TITLE = Component.translatable("screen." + Etched.MOD_ID + ".music_search");
    private static final Component QUERY = Component.translatable("screen." + Etched.MOD_ID + ".music_search.query");
    private static final Component SELECT = Component.translatable("screen." + Etched.MOD_ID + ".music_search.select");
    private static final Component SEARCHING = Component.translatable("screen." + Etched.MOD_ID + ".music_search.searching").withStyle(ChatFormatting.GRAY);
    private static final Component NO_RESULTS = Component.translatable("screen." + Etched.MOD_ID + ".music_search.no_results").withStyle(ChatFormatting.GRAY);
    private static final int SEARCH_DELAY = 10; // Ticks to wait after typing before searching
    private static final int ROW_HEIGHT = 24;

    private final Screen parent;
    private final UrlMenu target;
    private final List<MusicSearchSource> sources;
    private int sourceIndex;

    private EditBox query;
    private Button sourceButton;
    private Button selectButton;
    private ResultList results;
    private String lastQuery;
    private int searchTicks;
    private int searchId;
    @Nullable
    private Component status;

    public <T extends Screen & UrlMenu> MusicSearchScreen(T parent, List<MusicSearchSource> sources) {
        super(TITLE);
        this.parent = parent;
        this.target = parent;
        this.sources = sources;
        this.lastQuery = "";
    }

    /**
     * Creates the button that opens this screen.
     *
     * @param parent  The screen to put the URL into
     * @param sources The sources to search, with the default source first
     * @return A new button
     */
    public static <T extends Screen & UrlMenu> Button createButton(T parent, List<MusicSearchSource> sources, int x, int y) {
        Component message = Component.translatable("screen." + Etched.MOD_ID + ".music_search.open");
        SpriteIconButton button = SpriteIconButton.builder(message, b -> Minecraft.getInstance().setScreen(new MusicSearchScreen(parent, sources)), true).sprite(SEARCH_SPRITE, 12, 12).size(20, 20).build();
        button.setPosition(x, y);
        button.setTooltip(Tooltip.create(message));
        return button;
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(this.width - 20, 320);
        int left = (this.width - contentWidth) / 2;
        int sourceWidth = 100;

        this.query = new EditBox(this.font, left, 24, contentWidth - sourceWidth - 4, 20, this.query, QUERY);
        this.query.setMaxLength(256);
        this.query.setHint(QUERY.copy().withStyle(ChatFormatting.DARK_GRAY));
        this.query.setResponder(value -> this.searchTicks = SEARCH_DELAY);
        this.addRenderableWidget(this.query);

        this.sourceButton = this.addRenderableWidget(Button.builder(this.getSource().getName(), button -> {
            this.sourceIndex = (this.sourceIndex + 1) % this.sources.size();
            button.setMessage(this.getSource().getName());
            this.lastQuery = "";
            this.search();
        }).bounds(left + contentWidth - sourceWidth, 24, sourceWidth, 20).build());
        this.sourceButton.active = this.sources.size() > 1;

        List<ResultList.Entry> oldEntries = this.results != null ? List.copyOf(this.results.children()) : List.of();
        ResultList.Entry oldSelected = this.results != null ? this.results.getSelected() : null;
        this.results = this.addRenderableWidget(new ResultList(this.minecraft, this.width, this.height - 50 - 36, 50, contentWidth));
        oldEntries.forEach(this.results::addResult);
        this.results.setSelected(oldSelected);

        int buttonWidth = (contentWidth - 4) / 2;
        this.selectButton = this.addRenderableWidget(Button.builder(SELECT, button -> this.selectResult()).bounds(left, this.height - 28, buttonWidth, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).bounds(left + contentWidth - buttonWidth, this.height - 28, buttonWidth, 20).build());
        this.updateSelectButton();

        this.setInitialFocus(this.query);
    }

    private MusicSearchSource getSource() {
        return this.sources.get(this.sourceIndex);
    }

    private void search() {
        String text = this.query.getValue().strip();
        if (text.equals(this.lastQuery)) {
            return;
        }
        this.lastQuery = text;
        this.searchTicks = 0;

        int id = ++this.searchId;
        this.results.clearResults();
        this.updateSelectButton();
        if (text.isEmpty()) {
            this.status = null;
            return;
        }

        this.status = SEARCHING;
        MusicSearchSource source = this.getSource();
        CompletableFuture.supplyAsync(() -> {
            try {
                return source.search(text, Minecraft.getInstance().getProxy());
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, Util.nonCriticalIoPool()).whenCompleteAsync((found, error) -> {
            if (id != this.searchId) { // A newer search has started
                return;
            }
            if (error != null) {
                Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                LOGGER.error("Failed to search {} for '{}'", source.getName().getString(), text, cause);
                this.status = Component.translatable("screen." + Etched.MOD_ID + ".music_search.error", Objects.requireNonNullElse(cause.getMessage(), cause.getClass().getSimpleName())).withStyle(ChatFormatting.RED);
                return;
            }

            found.forEach(result -> this.results.addResult(new ResultList.Entry(this, result)));
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
        this.minecraft.setScreen(this.parent);
        this.target.setUrl(entry.result.url());
    }

    @Override
    public void tick() {
        if (this.searchTicks > 0 && --this.searchTicks == 0) {
            this.search();
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
            guiGraphics.drawCenteredString(this.font, this.status, this.width / 2, 50 + (this.height - 50 - 36) / 2 - this.font.lineHeight / 2, -1);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
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

            private final MusicSearchScreen screen;
            private final MusicSearchResult result;
            private long lastClickTime;

            private Entry(MusicSearchScreen screen, MusicSearchResult result) {
                this.screen = screen;
                this.result = result;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                Minecraft minecraft = Minecraft.getInstance();
                int textWidth = width - 8;
                guiGraphics.drawString(minecraft.font, minecraft.font.plainSubstrByWidth(this.result.title(), textWidth), left + 4, top + 2, -1);
                guiGraphics.drawString(minecraft.font, minecraft.font.plainSubstrByWidth(this.result.details(), textWidth), left + 4, top + 12, 0xFFA0A0A0);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                long time = Util.getMillis();
                if (time - this.lastClickTime < 250) { // Double click picks the result
                    this.screen.results.setSelected(this);
                    this.screen.selectResult();
                    return true;
                }
                this.lastClickTime = time;
                return super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", this.result.title());
            }
        }
    }
}
