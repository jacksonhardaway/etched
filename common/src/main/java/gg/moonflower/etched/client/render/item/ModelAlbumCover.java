package gg.moonflower.etched.client.render.item;

import gg.moonflower.etched.api.record.AlbumCover;
import net.minecraft.client.resources.model.ModelResourceLocation;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ModelAlbumCover implements AlbumCover {

    private final ModelResourceLocation model;

    public ModelAlbumCover(ModelResourceLocation model) {
        this.model = model;
    }

    public ModelResourceLocation getModel() {
        return model;
    }
}
