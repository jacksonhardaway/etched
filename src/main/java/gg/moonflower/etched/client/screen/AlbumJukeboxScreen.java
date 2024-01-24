package gg.moonflower.etched.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import gg.moonflower.etched.common.menu.AlbumJukeboxMenu;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.SetAlbumJukeboxTrackPacket;
import gg.moonflower.etched.core.Etched;
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

import java.util.List;
import java.util.Optional;

/**
 * @author Ocelot
 */
public class AlbumJukeboxScreen extends AbstractContainerScreen<AlbumJukeboxMenu> {

    private static final ResourceLocation CONTAINER_LOCATION = new ResourceLocation("textures/gui/container/dispenser.png");
    private static final Component NOW_PLAYING = Component.translatable("screen." + Etched.MOD_ID + ".album_jukebox.now_playing").withStyle(ChatFormatting.YELLOW);

    private int playingIndex;
    private int playingTrack;

    public AlbumJukeboxScreen(AlbumJukeboxMenu dispenserMenu, Inventory inventory, Component component) {
        super(dispenserMenu, inventory, component);
    }

    private void update(boolean next) {
        ClientLevel level = this.minecraft.level;
        if (level == null || !this.menu.isInitialized()) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(this.menu.getPos());
        if (!(blockEntity instanceof AlbumJukeboxBlockEntity) || !((AlbumJukeboxBlockEntity) blockEntity).isPlaying()) {
            return;
        }

        AlbumJukeboxBlockEntity albumJukebox = (AlbumJukeboxBlockEntity) blockEntity;
        int oldIndex = albumJukebox.getPlayingIndex();
        int oldTrack = albumJukebox.getTrack();
        if (next) {
            albumJukebox.next();
        } else {
            albumJukebox.previous();
        }

        if (((albumJukebox.getPlayingIndex() == oldIndex && albumJukebox.getTrack() != oldTrack) || albumJukebox.recalculatePlayingIndex(!next)) && albumJukebox.getPlayingIndex() != -1) {
            SoundTracker.playAlbum(albumJukebox, albumJukebox.getBlockState(), level, this.menu.getPos(), true);
            EtchedMessages.PLAY.sendToServer(new SetAlbumJukeboxTrackPacket(albumJukebox.getPlayingIndex(), albumJukebox.getTrack()));
        }
    }

    @Override
    protected void init() {
        super.init();

        int buttonPadding = 6;
        Component last = Component.literal("Last");
        Component next = Component.literal("Next");
        Font font = Minecraft.getInstance().font;
        this.addRenderableWidget(Button.builder(last, b -> this.update(false)).bounds(this.leftPos + 7 + (54 - font.width(last)) / 2 - buttonPadding, this.topPos + 33, font.width(last) + 2 * buttonPadding, 20).build());
        this.addRenderableWidget(Button.builder(next, b -> this.update(true)).bounds(this.leftPos + 115 + (54 - font.width(last)) / 2 - buttonPadding, this.topPos + 33, font.width(next) + 2 * buttonPadding, 20).build());
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(CONTAINER_LOCATION, guiLeft, guiTop, 0, 0, this.imageWidth, this.imageHeight);

        this.playingIndex = -1;
        this.playingTrack = 0;
        ClientLevel level = this.minecraft.level;
        if (level == null || !this.menu.isInitialized()) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(this.menu.getPos());
        if (!(blockEntity instanceof AlbumJukeboxBlockEntity)) {
            return;
        }

        this.playingIndex = ((AlbumJukeboxBlockEntity) blockEntity).getPlayingIndex();
        this.playingTrack = ((AlbumJukeboxBlockEntity) blockEntity).getTrack();
        if (this.playingIndex != -1) {
            int x = this.playingIndex % 3;
            int y = this.playingIndex / 3;
            guiGraphics.fillGradient(guiLeft + 62 + x * 18, guiTop + 17 + y * 18, guiLeft + 78 + x * 18, guiTop + 33 + y * 18, 0x3CF6FF00, 0x3CF6FF00);
        }
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> tooltip = super.getTooltipFromContainerItem(stack);

        if (this.hoveredSlot != null) {
            if (this.hoveredSlot.index == this.playingIndex) {
                if (this.playingTrack >= 0 && PlayableRecord.getStackTrackCount(stack) > 0) {
                    Optional<TrackData[]> optional = PlayableRecord.getStackMusic(stack).filter(tracks -> this.playingTrack < tracks.length);
                    if (optional.isPresent()) {
                        TrackData track = optional.get()[this.playingTrack];
                        tooltip.add(NOW_PLAYING.copy().append(": ").append(track.getDisplayName()).append(" (" + (this.playingTrack + 1) + "/" + optional.get().length + ")"));
                    } else {
                        tooltip.add(NOW_PLAYING);
                    }
                } else {
                    tooltip.add(NOW_PLAYING);
                }
            }
        }

        return tooltip;
    }
}
