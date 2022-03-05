package gg.moonflower.etched.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import gg.moonflower.etched.common.menu.AlbumJukeboxMenu;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.SetAlbumJukeboxTrackPacket;
import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandlerImpl;
import gg.moonflower.etched.core.Etched;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
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
    private static final Component NOW_PLAYING = new TranslatableComponent("screen." + Etched.MOD_ID + ".album_jukebox.now_playing").withStyle(ChatFormatting.YELLOW);

    private int playingIndex;
    private int playingTrack;

    public AlbumJukeboxScreen(AlbumJukeboxMenu dispenserMenu, Inventory inventory, Component component) {
        super(dispenserMenu, inventory, component);
    }

    private void update(boolean next) {
        ClientLevel level = this.minecraft.level;
        if (level == null || !this.menu.isInitialized())
            return;

        BlockEntity blockEntity = level.getBlockEntity(this.menu.getPos());
        if (!(blockEntity instanceof AlbumJukeboxBlockEntity) || !((AlbumJukeboxBlockEntity) blockEntity).isPlaying())
            return;

        AlbumJukeboxBlockEntity albumJukebox = (AlbumJukeboxBlockEntity) blockEntity;
        int oldIndex = albumJukebox.getPlayingIndex();
        int oldTrack = albumJukebox.getTrack();
        if (next) {
            albumJukebox.next();
        } else {
            albumJukebox.previous();
        }

        if (((albumJukebox.getPlayingIndex() == oldIndex && albumJukebox.getTrack() != oldTrack) || albumJukebox.recalculatePlayingIndex(!next)) && albumJukebox.getPlayingIndex() != -1) {
            EtchedClientPlayPacketHandlerImpl.playAlbum(albumJukebox, level, this.menu.getPos(), true);
            EtchedMessages.PLAY.sendToServer(new SetAlbumJukeboxTrackPacket(albumJukebox.getPlayingIndex(), albumJukebox.getTrack()));
        }
    }

    @Override
    protected void init() {
        super.init();

        int buttonPadding = 6;
        Component last = new TextComponent("Last");
        Component next = new TextComponent("Next");
        Font font = Minecraft.getInstance().font;
        this.addButton(new Button(this.leftPos + 7 + (54 - font.width(last)) / 2 - buttonPadding, this.topPos + 33, font.width(last) + 2 * buttonPadding, 20, last, b -> this.update(false)));
        this.addButton(new Button(this.leftPos + 115 + (54 - font.width(last)) / 2 - buttonPadding, this.topPos + 33, font.width(next) + 2 * buttonPadding, 20, next, b -> this.update(true)));
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void render(PoseStack poseStack, int i, int j, float f) {
        this.renderBackground(poseStack);
        super.render(poseStack, i, j, f);
        this.renderTooltip(poseStack, i, j);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void renderBg(PoseStack poseStack, float f, int i, int j) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bind(CONTAINER_LOCATION);
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;
        this.blit(poseStack, guiLeft, guiTop, 0, 0, this.imageWidth, this.imageHeight);

        this.playingIndex = -1;
        this.playingTrack = 0;
        ClientLevel level = this.minecraft.level;
        if (level == null || !this.menu.isInitialized())
            return;

        BlockEntity blockEntity = level.getBlockEntity(this.menu.getPos());
        if (!(blockEntity instanceof AlbumJukeboxBlockEntity))
            return;

        this.playingIndex = ((AlbumJukeboxBlockEntity) blockEntity).getPlayingIndex();
        this.playingTrack = ((AlbumJukeboxBlockEntity) blockEntity).getTrack();
        if (this.playingIndex != -1) {
            int x = this.playingIndex % 3;
            int y = this.playingIndex / 3;
            this.fillGradient(poseStack, guiLeft + 62 + x * 18, guiTop + 17 + y * 18, guiLeft + 78 + x * 18, guiTop + 33 + y * 18, 0x3CF6FF00, 0x3CF6FF00);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void renderTooltip(PoseStack poseStack, int i, int j) {
        if (this.minecraft.player.inventory.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack stack = this.hoveredSlot.getItem();
            List<Component> tooltip = this.getTooltipFromItem(stack);
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
            this.renderComponentTooltip(poseStack, tooltip, i, j);
        }
    }
}
