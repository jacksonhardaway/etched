package gg.moonflower.etched.core.mixin;

import com.google.common.base.Suppliers;
import gg.moonflower.etched.api.record.AlbumCover;
import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.client.render.item.AlbumCoverItemRenderer;
import gg.moonflower.etched.client.sound.EntityRecordSoundInstance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.net.Proxy;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Mixin(RecordItem.class)
public abstract class RecordItemMixin extends Item implements PlayableRecord {

    @Unique
    private final Supplier<TrackData[]> track = Suppliers.memoize(() -> {
        Component desc = this.getDisplayName();

        String[] parts = desc.getString().split("-", 2);
        if (parts.length < 2)
            return new TrackData[]{new TrackData(this.getSound().getLocation().toString(), "Minecraft", desc)};
        return new TrackData[]{new TrackData(this.getSound().getLocation().toString(), parts[0].trim(), new TextComponent(parts[1].trim()).withStyle(desc.getStyle()))};
    });

    @Shadow
    public abstract SoundEvent getSound();

    @Shadow
    public abstract MutableComponent getDisplayName();

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
    public CompletableFuture<AlbumCover> getAlbumCover(ItemStack stack, Proxy proxy, ResourceManager resourceManager) {
        ResourceLocation key = Registry.ITEM.getKey(this);
        return resourceManager.hasResource(new ResourceLocation(key.getNamespace(), "models/item/" + AlbumCoverItemRenderer.FOLDER_NAME + "/" + key.getPath() + ".json")) ? CompletableFuture.completedFuture(AlbumCover.of(new ResourceLocation(key.getNamespace(), AlbumCoverItemRenderer.FOLDER_NAME + "/" + key.getPath()))) : CompletableFuture.completedFuture(AlbumCover.EMPTY);
    }

    @Override
    public Optional<TrackData[]> getMusic(ItemStack stack) {
        return Optional.of(this.track.get());
    }

    @Override
    public Optional<TrackData> getAlbum(ItemStack stack) {
        return Optional.empty();
    }

    @Override
    public int getTrackCount(ItemStack stack) {
        return 1;
    }
}
