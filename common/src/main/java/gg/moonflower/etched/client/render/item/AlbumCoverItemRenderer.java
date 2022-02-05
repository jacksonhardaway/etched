
package gg.moonflower.etched.client.render.item;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.common.item.AlbumCoverItem;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.pollen.api.client.render.DynamicItemRenderer;
import gg.moonflower.pollen.api.event.events.network.ClientNetworkEvents;
import gg.moonflower.pollen.api.registry.resource.PollinatedPreparableReloadListener;
import gg.moonflower.pollen.api.registry.resource.ResourceRegistry;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AlbumCoverItemRenderer extends SimplePreparableReloadListener<AlbumCoverItemRenderer.CoverData> implements DynamicItemRenderer, PollinatedPreparableReloadListener {

    public static final AlbumCoverItemRenderer INSTANCE = new AlbumCoverItemRenderer();

    private static final ResourceLocation BLANK_ALBUM_COVER = new ResourceLocation(Etched.MOD_ID, "textures/item/blank_album_cover.png");
    private static final ResourceLocation DEFAULT_ALBUM_COVER = new ResourceLocation(Etched.MOD_ID, "textures/item/default_album_cover.png");
    private static final ResourceLocation ALBUM_COVER_OVERLAY = new ResourceLocation(Etched.MOD_ID, "textures/item/album_cover_overlay.png");

    private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();
    private static final BlockModel MODEL = BlockModel.fromString("{\"gui_light\":\"front\",\"textures\":{\"layer0\":\"texture\"},\"display\":{\"ground\":{\"rotation\":[0,0,0],\"translation\":[0,2,0],\"scale\":[0.5,0.5,0.5]},\"head\":{\"rotation\":[0,180,0],\"translation\":[0,13,7],\"scale\":[1,1,1]},\"thirdperson_righthand\":{\"rotation\":[0,0,0],\"translation\":[0,3,1],\"scale\":[0.55,0.55,0.55]},\"firstperson_righthand\":{\"rotation\":[0,-90,25],\"translation\":[1.13,3.2,1.13],\"scale\":[0.68,0.68,0.68]},\"fixed\":{\"rotation\":[0,180,0],\"scale\":[1,1,1]}}}");

    private final Map<CompoundTag, CompletableFuture<ModelData>> covers;
    private CoverData data;

    private AlbumCoverItemRenderer() {
        this.covers = new HashMap<>();
        this.data = null;
        ResourceRegistry.registerReloadListener(PackType.CLIENT_RESOURCES, this);
        ClientNetworkEvents.LOGOUT.register((controller, player, connection) -> this.close());
    }

    public static NativeImage getOverlayImage() {
        return INSTANCE.data.overlay.getImage();
    }

    private void close() {
        this.covers.values().forEach(future -> future.thenAcceptAsync(data -> {
            if (!this.data.is(data))
                data.close();
        }, Minecraft.getInstance()));
        this.covers.clear();
    }

    private void renderModelLists(BakedModel model, int combinedLight, int combinedOverlay, PoseStack matrixStack, VertexConsumer buffer) {
        Random random = new Random();

        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            this.renderQuadList(matrixStack, buffer, model.getQuads(null, direction, random), combinedLight, combinedOverlay);
        }

        random.setSeed(42L);
        this.renderQuadList(matrixStack, buffer, model.getQuads(null, null, random), combinedLight, combinedOverlay);
    }

    private void renderQuadList(PoseStack matrixStack, VertexConsumer buffer, List<BakedQuad> quads, int combinedLight, int combinedOverlay) {
        PoseStack.Pose pose = matrixStack.last();
        for (BakedQuad bakedQuad : quads) {
            buffer.putBulkData(pose, bakedQuad, 1, 1, 1, combinedLight, combinedOverlay);
        }
    }

    private static NativeImage createMissing() {
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

    private static NativeImage get(ResourceManager resourceManager, ResourceLocation location) {
        try (Resource resource = resourceManager.getResource(location)) {
            return NativeImage.read(resource.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return createMissing();
        }
    }

    @Override
    protected CoverData prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return new CoverData(get(resourceManager, BLANK_ALBUM_COVER), get(resourceManager, DEFAULT_ALBUM_COVER), get(resourceManager, ALBUM_COVER_OVERLAY));
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

        ModelData data = stack.getTagElement("CoverRecord") == null ? this.data.blank : this.covers.computeIfAbsent(stack.getTagElement("CoverRecord"), __ -> {
            ItemStack coverStack = AlbumCoverItem.getCoverStack(stack).orElse(ItemStack.EMPTY);
            if (!coverStack.isEmpty() && coverStack.getItem() instanceof PlayableRecord) {
                return ((PlayableRecord) coverStack.getItem()).getAlbumCover(coverStack, Minecraft.getInstance().getProxy(), Minecraft.getInstance().getResourceManager()).thenApplyAsync(image -> image.map(ModelData::new).orElseGet(() -> this.data.defaultCover), Util.backgroundExecutor()).exceptionally(e -> {
                    e.printStackTrace();
                    return this.data.defaultCover;
                });
            }
            return CompletableFuture.completedFuture(this.data.blank);
        }).getNow(this.data.defaultCover);

        BakedModel model = data.getModel();
        matrixStack.pushPose();
        matrixStack.translate(0.5D, 0.5D, 0.5D);
        model.getTransforms().getTransform(transformType).apply(transformType == ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND || transformType == ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND, matrixStack);
        matrixStack.translate(-0.5D, -0.5D, -0.5D);
        if (!model.isCustomRenderer())
            this.renderModelLists(model, packedLight, combinedOverlay, matrixStack, ItemRenderer.getFoilBufferDirect(buffer, RenderType.entityCutout(data.getName()), false, stack.hasFoil()));

        matrixStack.popPose();
    }

    @Override
    public ResourceLocation getPollenId() {
        return new ResourceLocation(Etched.MOD_ID, "builtin_album_cover");
    }

    public static class CoverData {

        private final ModelData blank;
        private final ModelData defaultCover;
        private final ModelData overlay;

        private CoverData(NativeImage blank, NativeImage defaultCover, NativeImage overlay) {
            this.blank = new ModelData(blank);
            this.defaultCover = new ModelData(defaultCover);
            this.overlay = new ModelData(overlay);
        }

        public void close() {
            this.blank.close();
            this.defaultCover.close();
            this.overlay.close();
        }

        public boolean is(ModelData data) {
            return this.blank == data || this.defaultCover == data || this.overlay == data;
        }
    }

    private static class ModelData extends TextureAtlasSprite {

        private BakedModel model;

        private ModelData(NativeImage image) {
            super(null, new Info(new ResourceLocation(Etched.MOD_ID, DigestUtils.md5Hex(UUID.randomUUID().toString())), image.getWidth(), image.getHeight(), AnimationMetadataSection.EMPTY), 0, image.getWidth(), image.getHeight(), 0, 0, image);
        }

        public BakedModel getModel() {
            if (this.model == null)
                this.model = ITEM_MODEL_GENERATOR.generateBlockModel(material -> this, MODEL).bake(null, MODEL, material -> this, BlockModelRotation.X0_Y0, this.getName(), false);
            if (Minecraft.getInstance().getTextureManager().getTexture(this.getName()) == null)
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
}
