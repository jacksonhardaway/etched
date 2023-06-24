
package gg.moonflower.etched.client.render.item;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gg.moonflower.etched.api.record.AlbumCover;
import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.common.item.AlbumCoverItem;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.pollen.api.client.render.DynamicItemRenderer;
import gg.moonflower.pollen.api.event.events.network.ClientNetworkEvents;
import gg.moonflower.pollen.api.registry.resource.PollinatedPreparableReloadListener;
import gg.moonflower.pollen.api.registry.resource.ResourceRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author Ocelot
 */
public class AlbumCoverItemRenderer extends SimplePreparableReloadListener<AlbumCoverItemRenderer.CoverData> implements DynamicItemRenderer, PollinatedPreparableReloadListener {

    public static final AlbumCoverItemRenderer INSTANCE = new AlbumCoverItemRenderer();
    public static final String FOLDER_NAME = Etched.MOD_ID + "_album_cover";

    private static final ModelResourceLocation BLANK_ALBUM_COVER = new ModelResourceLocation(new ResourceLocation(Etched.MOD_ID, FOLDER_NAME + "/blank"), "inventory");
    private static final ModelResourceLocation DEFAULT_ALBUM_COVER = new ModelResourceLocation(new ResourceLocation(Etched.MOD_ID, FOLDER_NAME + "/default"), "inventory");
    private static final ResourceLocation ALBUM_COVER_OVERLAY = new ResourceLocation(Etched.MOD_ID, "textures/item/album_cover_overlay.png");

    private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();
    private static final BlockModel MODEL = BlockModel.fromString("{\"gui_light\":\"front\",\"textures\":{\"layer0\":\"texture\"},\"display\":{\"ground\":{\"rotation\":[0,0,0],\"translation\":[0,2,0],\"scale\":[0.5,0.5,0.5]},\"head\":{\"rotation\":[0,180,0],\"translation\":[0,13,7],\"scale\":[1,1,1]},\"thirdperson_righthand\":{\"rotation\":[0,0,0],\"translation\":[0,3,1],\"scale\":[0.55,0.55,0.55]},\"firstperson_righthand\":{\"rotation\":[0,-90,25],\"translation\":[1.13,3.2,1.13],\"scale\":[0.68,0.68,0.68]},\"fixed\":{\"rotation\":[0,180,0],\"scale\":[1,1,1]}}}");

    private final Map<CompoundTag, CompletableFuture<ModelData>> covers;
    private CoverData data;

    private AlbumCoverItemRenderer() {
        this.covers = new HashMap<>();
        this.data = null;
    }

    public static void init() {
        ResourceRegistry.registerReloadListener(PackType.CLIENT_RESOURCES, INSTANCE);
        ClientNetworkEvents.LOGOUT.register((controller, player, connection) -> INSTANCE.close());
    }

    public static NativeImage getOverlayImage() {
        return INSTANCE.data.overlay.getImage();
    }

    private static void renderModelLists(BakedModel model, int combinedLight, int combinedOverlay, PoseStack matrixStack, VertexConsumer buffer) {
        RandomSource randomSource = RandomSource.create(42L);

        for (Direction direction : Direction.values()) {
            renderQuadList(matrixStack, buffer, model.getQuads(null, direction, randomSource), combinedLight, combinedOverlay);
        }

        renderQuadList(matrixStack, buffer, model.getQuads(null, null, randomSource), combinedLight, combinedOverlay);
    }

    private static void renderQuadList(PoseStack matrixStack, VertexConsumer buffer, List<BakedQuad> quads, int combinedLight, int combinedOverlay) {
        PoseStack.Pose pose = matrixStack.last();
        for (BakedQuad bakedQuad : quads) {
            buffer.putBulkData(pose, bakedQuad, 1, 1, 1, combinedLight, combinedOverlay);
        }
    }

    private static NativeImage getCoverOverlay(ResourceManager resourceManager) {
        Optional<Resource> resource = resourceManager.getResource(AlbumCoverItemRenderer.ALBUM_COVER_OVERLAY);

        if (resource.isPresent()) {
            try {
                return NativeImage.read(resource.get().open());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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

    private void close() {
        this.covers.values().forEach(future -> future.thenAcceptAsync(data -> {
            if (!this.data.is(data))
                data.close();
        }, task -> RenderSystem.recordRenderCall(task::run)));
        this.covers.clear();
    }

    @Override
    protected CoverData prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return new CoverData(getCoverOverlay(resourceManager));
    }

    @Override
    protected void apply(CoverData data, ResourceManager resourceManager, ProfilerFiller profiler) {
        if (this.data != null)
            this.data.close();
        this.data = data;
        this.close();
    }

    @Override
    public void render(ItemStack stack, ItemTransforms.TransformType transformType, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, int combinedOverlay) {
        if (stack.isEmpty())
            return;
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

        matrixStack.pushPose();
        matrixStack.translate(0.5D, 0.5D, 0.5D);
        model.render(stack, transformType, matrixStack, buffer, packedLight, combinedOverlay);
        matrixStack.popPose();
    }

    @Override
    public ResourceLocation getPollenId() {
        return new ResourceLocation(Etched.MOD_ID, "builtin_album_cover");
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
        public void render(ItemStack stack, ItemTransforms.TransformType transformType, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, int combinedOverlay) {
            ModelManager modelManager = Minecraft.getInstance().getModelManager();
            BakedModel model = this.rendering ? modelManager.getMissingModel() : modelManager.getModel(this.model);
            this.rendering = true; // Prevent deadlock from repeated rendering
            Minecraft.getInstance().getItemRenderer().render(stack, transformType, transformType == ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND || transformType == ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND, matrixStack, buffer, packedLight, combinedOverlay, model);
            this.rendering = false;
        }

        @Override
        public void close() {
        }
    }

    private static class DynamicModelData extends TextureAtlasSprite implements ModelData {

        private static final TextureAtlas ATLAS = new TextureAtlas(new ResourceLocation(Etched.MOD_ID, DigestUtils.md5Hex(UUID.randomUUID().toString())));
        private BakedModel model;

        private DynamicModelData(NativeImage image) {
            super(ATLAS, new Info(new ResourceLocation(Etched.MOD_ID, DigestUtils.md5Hex(UUID.randomUUID().toString())), image.getWidth(), image.getHeight(), AnimationMetadataSection.EMPTY), 0, image.getWidth(), image.getHeight(), 0, 0, image);
        }

        @Override
        public void render(ItemStack stack, ItemTransforms.TransformType transformType, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, int combinedOverlay) {
            BakedModel model = this.getModel();
            if (model.isCustomRenderer())
                return;
            model.getTransforms().getTransform(transformType).apply(transformType == ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND || transformType == ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND, matrixStack);
            matrixStack.translate(-0.5D, -0.5D, -0.5D);
            renderModelLists(model, packedLight, combinedOverlay, matrixStack, ItemRenderer.getFoilBufferDirect(buffer, RenderType.entityCutout(this.getName()), false, stack.hasFoil()));
        }

        private BakedModel getModel() {
            if (this.model == null) {
                ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
                profiler.push("buildAlbumCoverModel");
                this.model = ITEM_MODEL_GENERATOR.generateBlockModel(material -> this, MODEL).bake(null, MODEL, material -> this, BlockModelRotation.X0_Y0, this.getName(), false);
                profiler.pop();
            }
            if (Minecraft.getInstance().getTextureManager().getTexture(this.getName(), null) == null)
                Minecraft.getInstance().getTextureManager().register(this.getName(), new DynamicTexture(this.mainImage[0]));
            return this.model;
        }

        public NativeImage getImage() {
            return this.mainImage[0];
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
            super.close();
            Minecraft.getInstance().getTextureManager().release(this.getName());
        }
    }

    private interface ModelData {

        static Optional<ModelData> of(AlbumCover cover) {
            if (cover instanceof ModelAlbumCover)
                return Optional.of(new BakedModelData(((ModelAlbumCover) cover).getModel()));
            if (cover instanceof ImageAlbumCover)
                return Optional.of(new DynamicModelData(((ImageAlbumCover) cover).getImage()));
            return Optional.empty();
        }

        void render(ItemStack stack, ItemTransforms.TransformType transformType, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, int combinedOverlay);

        void close();
    }
}
