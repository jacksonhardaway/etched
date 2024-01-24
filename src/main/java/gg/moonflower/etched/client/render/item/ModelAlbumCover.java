package gg.moonflower.etched.client.render.item;

import gg.moonflower.etched.api.record.AlbumCover;
import net.minecraft.client.resources.model.ModelResourceLocation;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public record ModelAlbumCover(ModelResourceLocation model) implements AlbumCover {
}
