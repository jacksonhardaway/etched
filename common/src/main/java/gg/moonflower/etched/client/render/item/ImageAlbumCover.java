package gg.moonflower.etched.client.render.item;

import com.mojang.blaze3d.platform.NativeImage;
import gg.moonflower.etched.api.record.AlbumCover;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public record ImageAlbumCover(NativeImage image) implements AlbumCover {
}
