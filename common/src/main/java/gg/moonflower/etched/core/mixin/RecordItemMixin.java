package gg.moonflower.etched.core.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.client.sound.EntityRecordSoundInstance;
import gg.moonflower.etched.core.Etched;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.RecordItem;
import org.spongepowered.asm.mixin.Mixin;

import java.io.IOException;
import java.net.Proxy;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Mixin(RecordItem.class)
public abstract class RecordItemMixin extends Item implements PlayableRecord {

    private RecordItemMixin(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canPlay(ItemStack stack) {
        return true;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public Optional<SoundInstance> createEntitySound(ItemStack stack, Entity entity, int track) {
        if (track != 0 || !(stack.getItem() instanceof RecordItem))
            return Optional.empty();

        if (PlayableRecord.canShowMessage(entity.getX(), entity.getY(), entity.getZ()))
            PlayableRecord.showMessage(((RecordItem) stack.getItem()).getDisplayName());
        return Optional.of(new EntityRecordSoundInstance(((RecordItem) stack.getItem()).getSound(), entity));
    }

    @Override
    public CompletableFuture<Optional<NativeImage>> getAlbumCover(ItemStack stack, Proxy proxy, ResourceManager resourceManager) {
        ResourceLocation registry = Registry.ITEM.getKey(this);
        ResourceLocation name = "minecraft".equals(registry.getNamespace()) ? new ResourceLocation(Etched.MOD_ID, "vanilla") : registry;
        ResourceLocation location = new ResourceLocation(name.getNamespace(), "textures/item/" + name.getPath() + "_cover.png");
        return !resourceManager.hasResource(location) ? CompletableFuture.completedFuture(Optional.empty()) : CompletableFuture.supplyAsync(() -> {
            try (Resource resource = resourceManager.getResource(location)) {
                return Optional.of(NativeImage.read(resource.getInputStream()));
            } catch (IOException e) {
                throw new CompletionException("Failed to read album cover from '" + location + "'", e);
            }
        }, Util.ioPool());
    }
}
