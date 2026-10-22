package gg.moonflower.etched.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import gg.moonflower.etched.client.sound.AlbumPlayback;
import gg.moonflower.etched.common.menu.AlbumJukeboxMenu;
import gg.moonflower.etched.common.menu.SoundSettingsMenu;
import gg.moonflower.etched.common.network.play.ServerboundSoundSettingsPacket;
import gg.moonflower.etched.common.network.play.SetAlbumJukeboxTrackPacket;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.compat.create.CreateCompatClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Ocelot
 */
public class AlbumJukeboxScreen extends AbstractContainerScreen<AlbumJukeboxMenu> {

    private static final ResourceLocation CONTAINER_LOCATION = Etched.etchedPath("textures/gui/container/album_jukebox.png");
    private static final Component NOW_PLAYING = Component.translatable("screen." + Etched.MOD_ID + ".album_jukebox.now_playing").withStyle(ChatFormatting.YELLOW);

    private static final Component PREVIOUS = Component.translatable("screen." + Etched.MOD_ID + ".album_jukebox.previous");
    private static final Component NEXT = Component.translatable("screen." + Etched.MOD_ID + ".album_jukebox.next");

    private static final int TEXTURE_HEIGHT = 166;

    private int playingIndex;
    private int playingTrack;
    private int volume;
    private int radius;

    public AlbumJukeboxScreen(AlbumJukeboxMenu dispenserMenu, Inventory inventory, Component component) {
        super(dispenserMenu, inventory, component);
        this.imageHeight = TEXTURE_HEIGHT + AlbumJukeboxMenu.SETTINGS_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
        this.volume = Math.round(dispenserMenu.getInitialVolume() * 100.0F);
        this.radius = dispenserMenu.getInitialRadius();
    }

    /**
     * @return The playback of a jukebox on a moving contraption or <code>null</code> for a placed jukebox
     */
    private @Nullable AlbumPlayback getContraptionPlayback() {
        return this.menu.getContraptionId() != -1 && Etched.CREATE_LOADED ? CreateCompatClient.getAlbumPlayback(this.menu.getContraptionId(), this.menu.getPos()) : null;
    }

    private void update(boolean next) {
        ClientLevel level = this.minecraft.level;
        if (level == null) {
            return;
        }

        if (this.menu.getContraptionId() != -1) {
            AlbumPlayback playback = this.getContraptionPlayback();
            if (playback != null) {
                if (next) {
                    playback.next();
                } else {
                    playback.previous();
                }
            }
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(this.menu.getPos());
        if (!(blockEntity instanceof AlbumJukeboxBlockEntity jukebox) || !jukebox.isPlaying()) {
            return;
        }

        int oldIndex = jukebox.getPlayingIndex();
        int oldTrack = jukebox.getTrack();
        if (next) {
            jukebox.next();
        } else {
            jukebox.previous();
        }

        if (((jukebox.getPlayingIndex() == oldIndex && jukebox.getTrack() != oldTrack) || jukebox.recalculatePlayingIndex(!next)) && jukebox.getPlayingIndex() != -1) {
            SoundTracker.playAlbum(jukebox, jukebox.getBlockState(), level, this.menu.getPos(), true);
            PacketDistributor.sendToServer(new SetAlbumJukeboxTrackPacket(jukebox.getPlayingIndex(), jukebox.getTrack()));
        }
    }

    @Override
    protected void init() {
        super.init();

        int buttonPadding = 6;
        Font font = Minecraft.getInstance().font;
        this.addRenderableWidget(Button.builder(PREVIOUS, b -> this.update(false))
                .bounds(this.leftPos + 3 + (58 - font.width(PREVIOUS)) / 2 - buttonPadding, this.topPos + 33, font.width(PREVIOUS) + 2 * buttonPadding, 20)
                .build());
        this.addRenderableWidget(Button.builder(NEXT, b -> this.update(true))
                .bounds(this.leftPos + 115 + (58 - font.width(NEXT)) / 2 - buttonPadding, this.topPos + 33, font.width(NEXT) + 2 * buttonPadding, 20)
                .build());
        this.addRenderableWidget(new SettingSlider(this.leftPos + 8, this.topPos + 74, 160, 14, "screen." + Etched.MOD_ID + ".volume", 0, 100, this.volume, value -> {
            this.volume = value;
            this.sendSettings();
        }));
        this.addRenderableWidget(new SettingSlider(this.leftPos + 8, this.topPos + 90, 160, 14, "screen." + Etched.MOD_ID + ".radius", SoundSettingsMenu.MIN_RADIUS, SoundSettingsMenu.MAX_RADIUS, this.radius, value -> {
            this.radius = value;
            this.sendSettings();
        }));
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Container screens only drag items, so the sliders have to be passed drags themselves
        if (button == 0 && this.isDragging() && this.getFocused() instanceof SettingSlider slider) {
            return slider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void sendSettings() {
        PacketDistributor.sendToServer(new ServerboundSoundSettingsPacket(this.volume / 100.0F, this.radius));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;
        // Stretch a plain row between the discs and the inventory to make room for the sound settings
        int settingsHeight = AlbumJukeboxMenu.SETTINGS_HEIGHT;
        graphics.blit(CONTAINER_LOCATION, guiLeft, guiTop, 0, 0, this.imageWidth, 72);
        graphics.blit(CONTAINER_LOCATION, guiLeft, guiTop + 72, this.imageWidth, settingsHeight, 0, 72, this.imageWidth, 1, 256, 256);
        graphics.blit(CONTAINER_LOCATION, guiLeft, guiTop + 72 + settingsHeight, 0, 72, this.imageWidth, TEXTURE_HEIGHT - 72);

        this.playingIndex = -1;
        this.playingTrack = 0;
        ClientLevel level = this.minecraft.level;
        if (level == null) {
            return;
        }

        if (this.menu.getContraptionId() != -1) {
            AlbumPlayback playback = this.getContraptionPlayback();
            if (playback == null) {
                return;
            }
            this.playingIndex = playback.getPlayingIndex();
            this.playingTrack = playback.getTrack();
        } else {
            BlockEntity blockEntity = level.getBlockEntity(this.menu.getPos());
            if (!(blockEntity instanceof AlbumJukeboxBlockEntity jukebox)) {
                return;
            }
            this.playingIndex = jukebox.getPlayingIndex();
            this.playingTrack = jukebox.getTrack();
        }
        if (this.playingIndex != -1) {
            int x = this.playingIndex % 3;
            int y = this.playingIndex / 3;
            graphics.fill(guiLeft + 62 + x * 18, guiTop + 17 + y * 18, guiLeft + 78 + x * 18, guiTop + 33 + y * 18, 0x3CF6FF00);
        }
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> tooltip = super.getTooltipFromContainerItem(stack);

        if (this.hoveredSlot != null && this.hoveredSlot.index == this.playingIndex) {
            if (this.playingTrack >= 0) {
                List<TrackData> tracks = PlayableRecord.getTracks(this.minecraft.getConnection().registryAccess(), stack);
                if (this.playingTrack < tracks.size()) {
                    // TODO use lang key
                    TrackData track = tracks.get(this.playingTrack);
                    tooltip.add(NOW_PLAYING.copy().append(": ").append(track.getDisplayName()).append(" (" + (this.playingTrack + 1) + "/" + tracks.size() + ")"));
                    return tooltip;
                }
            }
            tooltip.add(NOW_PLAYING);
        }

        return tooltip;
    }
}
