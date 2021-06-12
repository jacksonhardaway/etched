package me.jaackson.etched.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.jaackson.etched.common.blockentity.AlbumJukeboxBlockEntity;
import me.jaackson.etched.common.menu.AlbumJukeboxMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * @author Ocelot
 */
public class AlbumJukeboxScreen extends AbstractContainerScreen<AlbumJukeboxMenu> {

    private static final ResourceLocation CONTAINER_LOCATION = new ResourceLocation("textures/gui/container/dispenser.png");

    private BlockPos pos;
    private int playingIndex;

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
            List<Component> tooltip = this.getTooltipFromItem(this.hoveredSlot.getItem());
            if (this.hoveredSlot.index == this.playingIndex)
                tooltip.add(new TextComponent("Now Playing").withStyle(ChatFormatting.YELLOW));
            this.renderComponentTooltip(poseStack, tooltip, i, j);
        }
    }
}
