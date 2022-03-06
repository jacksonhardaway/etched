package gg.moonflower.etched.client.render.item;

import com.mojang.blaze3d.platform.NativeImage;
import gg.moonflower.etched.api.record.AlbumCover;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ImageAlbumCover implements AlbumCover {

    private final NativeImage image;

    public ImageAlbumCover(NativeImage image) {
        this.image = image;
    }

    public NativeImage getImage() {
        return image;
    }
}
