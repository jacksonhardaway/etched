package gg.moonflower.etched.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.download.SoundSourceManager;
import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import gg.moonflower.etched.common.item.EtchedMusicDiscItem;
import gg.moonflower.etched.common.menu.AlbumJukeboxMenu;
import gg.moonflower.etched.core.Etched;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * @author Ocelot
 */
public class AlbumJukeboxScreen extends AbstractContainerScreen<AlbumJukeboxMenu> {

    private static final ResourceLocation CONTAINER_LOCATION = new ResourceLocation("textures/gui/container/dispenser.png");
    private static final Component NOW_PLAYING = new TranslatableComponent("screen." + Etched.MOD_ID + ".album_jukebox.now_playing").withStyle(ChatFormatting.YELLOW);

    private BlockPos pos;
    private int playingIndex;
    private int playingTrack;

    public AlbumJukeboxScreen(AlbumJukeboxMenu dispenserMenu, Inventory inventory, Component component) {
        super(dispenserMenu, inventory, component);
        this.pos = BlockPos.ZERO;
    }

    @Override
    protected void init() {
        super.init();
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

        int[] menuPos = this.menu.pos;
        if (this.pos.getX() != menuPos[0] || this.pos.getY() != menuPos[1] || this.pos.getZ() != menuPos[2])
            this.pos = new BlockPos(menuPos[0], menuPos[1], menuPos[2]);

        if (this.pos == BlockPos.ZERO)
            return;

        BlockEntity blockEntity = level.getBlockEntity(this.pos);
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
                tooltip.add(NOW_PLAYING);
                if (this.playingTrack >= 0 && EtchedMusicDiscItem.getTrackCount(stack) > 1) {
                    EtchedMusicDiscItem.getMusic(stack).filter(tracks -> this.playingTrack < tracks.length).ifPresent(tracks -> {
                        TrackData track = tracks[this.playingTrack];
                        tooltip.add(track.getDisplayName().copy().withStyle(ChatFormatting.GRAY));
                        SoundSourceManager.getBrandText(track.getUrl()).ifPresent(tooltip::add);
                    });
                }
            }
            this.renderComponentTooltip(poseStack, tooltip, i, j);
        }
    }
}
