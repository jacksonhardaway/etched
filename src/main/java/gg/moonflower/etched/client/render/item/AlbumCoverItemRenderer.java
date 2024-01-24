
package gg.moonflower.etched.client.render.item;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gg.moonflower.etched.api.record.AlbumCover;
import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.common.item.AlbumCoverItem;
import gg.moonflower.etched.core.Etched;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * @author Ocelot
 */
public class AlbumCoverItemRenderer extends BlockEntityWithoutLevelRenderer implements PreparableReloadListener {

    public static final AlbumCoverItemRenderer INSTANCE = new AlbumCoverItemRenderer();
    public static final String FOLDER_NAME = Etched.MOD_ID + "_album_cover";

    private static final ModelResourceLocation BLANK_ALBUM_COVER = new ModelResourceLocation(new ResourceLocation(Etched.MOD_ID, FOLDER_NAME + "/blank"), "inventory");
    private static final ModelResourceLocation DEFAULT_ALBUM_COVER = new ModelResourceLocation(new ResourceLocation(Etched.MOD_ID, FOLDER_NAME + "/default"), "inventory");
    private static final ResourceLocation ALBUM_COVER_OVERLAY = new ResourceLocation(Etched.MOD_ID, "textures/item/album_cover_overlay.png");

    private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();
    private static final BlockModel MODEL = BlockModel.fromString("{\"gui_light\":\"front\",\"textures\":{\"layer0\":\"texture\"},\"display\":{\"ground\":{\"rotation\":[0,0,0],\"translation\":[0,2,0],\"scale\":[0.5,0.5,0.5]},\"head\":{\"rotation\":[0,180,0],\"translation\":[0,13,7],\"scale\":[1,1,1]},\"thirdperson_righthand\":{\"rotation\":[0,0,0],\"translation\":[0,3,1],\"scale\":[0.55,0.55,0.55]},\"firstperson_righthand\":{\"rotation\":[0,-90,25],\"translation\":[1.13,3.2,1.13],\"scale\":[0.68,0.68,0.68]},\"fixed\":{\"rotation\":[0,180,0],\"scale\":[1,1,1]}}}");

    private final Map<CompoundTag, CompletableFuture<ModelData>> covers;
    private CoverData data;

    static {
        MinecraftForge.EVENT_BUS.<ClientPlayerNetworkEvent.LoggingOut>addListener(event -> INSTANCE.close());
    }

    private AlbumCoverItemRenderer() {
        super(null, null);
        this.covers = new HashMap<>();
        this.data = null;
    }

    @Deprecated
    public static void init() {
//        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, INSTANCE, new ResourceLocation(Etched.MOD_ID, "builtin_album_cover"));
//        ClientNetworkEvent.DISCONNECT.register((controller, player, connection) -> INSTANCE.close());
    }

    public static NativeImage getOverlayImage() {
        return INSTANCE.data.overlay.getImage();
    }

    private static void renderModelLists(BakedModel model, int combinedLight, int combinedOverlay, PoseStack matrixStack, VertexConsumer buffer) {
        RandomSource randomsource = RandomSource.create();

        for (Direction direction : Direction.values()) {
            randomsource.setSeed(42L);
            renderQuadList(matrixStack, buffer, model.getQuads(null, direction, randomsource), combinedLight, combinedOverlay);
        }

        randomsource.setSeed(42L);
        renderQuadList(matrixStack, buffer, model.getQuads(null, null, randomsource), combinedLight, combinedOverlay);
    }

    private static void renderQuadList(PoseStack matrixStack, VertexConsumer buffer, List<BakedQuad> quads, int combinedLight, int combinedOverlay) {
        PoseStack.Pose pose = matrixStack.last();
        for (BakedQuad bakedQuad : quads) {
            buffer.putBulkData(pose, bakedQuad, 1, 1, 1, combinedLight, combinedOverlay);
        }
    }

    private static NativeImage getCoverOverlay(ResourceManager resourceManager) {
        try {
            try (InputStream stream = resourceManager.getResourceOrThrow(AlbumCoverItemRenderer.ALBUM_COVER_OVERLAY).open()) {
                return NativeImage.read(stream);
            }
        } catch (IOException e) {
            e.printStackTrace();

            NativeImage nativeImage = new NativeImage(16, 16, false);
            for (int k = 0; k < 16; ++k) {
                for (int l = 0; l < 16; ++l) {
                    if (k < 8 ^ l < 8) {
                        nativeImage.setPixelRGBA(l, k, -524040);
                    } else {
                        nativeImage.setPixelRGBA(l, k, -16777216);
                    }
                }
            }

            nativeImage.untrack();
            return nativeImage;
        }
    }

    private void close() {
        this.covers.values().forEach(future -> future.thenAcceptAsync(data -> {
            if (!this.data.is(data)) {
                data.close();
            }
        }, task -> RenderSystem.recordRenderCall(task::run)));
        this.covers.clear();
    }

    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> new CoverData(getCoverOverlay(resourceManager)), backgroundExecutor)
                .thenCompose(preparationBarrier::wait)
                .thenAcceptAsync(data -> {
                    if (this.data != null) {
                        this.data.close();
                    }
                    this.data = data;
                    this.close();
                }, gameExecutor);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (stack.isEmpty()) {
            return;
        }
        ModelData model = stack.getTagElement("CoverRecord") == null ? this.data.blank : this.covers.computeIfAbsent(stack.getTagElement("CoverRecord"), __ -> {
            ItemStack coverStack = AlbumCoverItem.getCoverStack(stack).orElse(ItemStack.EMPTY);
            if (!coverStack.isEmpty() && coverStack.getItem() instanceof PlayableRecord) {
                return ((PlayableRecord) coverStack.getItem()).getAlbumCover(coverStack, Minecraft.getInstance().getProxy(), Minecraft.getInstance().getResourceManager()).thenApply(cover -> ModelData.of(cover).orElse(this.data.defaultCover)).exceptionally(e -> {
                    e.printStackTrace();
                    return this.data.defaultCover;
                });
            }
            return CompletableFuture.completedFuture(this.data.blank);
        }).getNow(this.data.defaultCover);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        model.render(stack, displayContext, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    public static class CoverData {

        private final DynamicModelData overlay;
        private final ModelData blank;
        private final ModelData defaultCover;

        private CoverData(NativeImage overlay) {
            this.overlay = new DynamicModelData(overlay);
            this.blank = new BakedModelData(BLANK_ALBUM_COVER);
            this.defaultCover = new BakedModelData(DEFAULT_ALBUM_COVER);
        }

        public void close() {
            this.overlay.close();
            this.blank.close();
            this.defaultCover.close();
        }

        public boolean is(ModelData data) {
            return this.overlay == data || this.blank == data || this.defaultCover == data;
        }
    }

    private static class BakedModelData implements ModelData {

        private final ModelResourceLocation model;
        private boolean rendering;

        private BakedModelData(ModelResourceLocation model) {
            this.model = model;
        }

        @Override
        public void render(ItemStack stack, ItemDisplayContext transformType, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, int combinedOverlay) {
            ModelManager modelManager = Minecraft.getInstance().getModelManager();
            BakedModel model = this.rendering ? modelManager.getMissingModel() : modelManager.getModel(this.model);
            this.rendering = true; // Prevent deadlock from repeated rendering
            Minecraft.getInstance().getItemRenderer().render(stack, transformType, transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND, matrixStack, buffer, packedLight, combinedOverlay, model);
            this.rendering = false;
        }

        @Override
        public void close() {
        }
    }

    private static class DynamicModelData extends TextureAtlasSprite implements ModelData {

        private static final ResourceLocation ATLAS = new ResourceLocation(Etched.MOD_ID, DigestUtils.md5Hex(UUID.randomUUID().toString()));
        private BakedModel model;

        private DynamicModelData(NativeImage image) {
            super(ATLAS, new SpriteContents(new ResourceLocation(Etched.MOD_ID, DigestUtils.md5Hex(UUID.randomUUID().toString())), new FrameSize(image.getWidth(), image.getHeight()), image, AnimationMetadataSection.EMPTY, null), image.getWidth(), image.getHeight(), 0, 0);
        }

        @Override
        public void render(ItemStack stack, ItemDisplayContext transformType, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, int combinedOverlay) {
            BakedModel model = this.getModel();
            if (model.isCustomRenderer()) {
                return;
            }
            model.getTransforms().getTransform(transformType).apply(transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND, matrixStack);
            matrixStack.translate(-0.5D, -0.5D, -0.5D);
            renderModelLists(model, packedLight, combinedOverlay, matrixStack, ItemRenderer.getFoilBufferDirect(buffer, RenderType.entityCutout(this.contents().name()), false, stack.hasFoil()));
        }

        private BakedModel getModel() {
            ResourceLocation name = this.contents().name();
            if (this.model == null) {
                ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
                profiler.push("buildAlbumCoverModel");
                this.model = ITEM_MODEL_GENERATOR.generateBlockModel(material -> this, MODEL).bake(null, MODEL, material -> this, BlockModelRotation.X0_Y0, name, false);
                profiler.pop();
            }
            if (Minecraft.getInstance().getTextureManager().getTexture(name, null) == null) {
                Minecraft.getInstance().getTextureManager().register(name, new DynamicTexture(this.getImage()));
            }
            return this.model;
        }

        public NativeImage getImage() {
            return this.contents().getOriginalImage();
        }

        @Override
        public float uvShrinkRatio() {
            return 0.0F;
        }

        @Override
        public VertexConsumer wrap(VertexConsumer buffer) {
            return buffer;
        }

        @Override
        public void close() {
            this.contents().close();
            Minecraft.getInstance().getTextureManager().release(this.contents().name());
        }
    }

    public interface ModelData {

        static Optional<ModelData> of(AlbumCover cover) {
            if (cover instanceof ModelAlbumCover) {
                return Optional.of(new BakedModelData(((ModelAlbumCover) cover).model()));
            }
            if (cover instanceof ImageAlbumCover) {
                return Optional.of(new DynamicModelData(((ImageAlbumCover) cover).image()));
            }
            return Optional.empty();
        }

        void render(ItemStack stack, ItemDisplayContext transformType, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, int combinedOverlay);

        void close();
    }
}
