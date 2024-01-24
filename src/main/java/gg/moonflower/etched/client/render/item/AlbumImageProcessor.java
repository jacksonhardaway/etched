package gg.moonflower.etched.client.render.item;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class AlbumImageProcessor {

    private static final int COLOR_DIVISIONS = 16;
    private static final float[] POW22 = Util.make(new float[256], fs -> {
        for (int i = 0; i < fs.length; ++i) {
            fs[i] = (float) Math.pow((float) i / 255.0F, 2.2);
        }
    });

    public static NativeImage apply(NativeImage image, NativeImage overlay, int border) {
        NativeImage nativeImage2 = new NativeImage(overlay.getWidth(), overlay.getHeight(), true);
        int k = nativeImage2.getWidth();
        int l = nativeImage2.getHeight();
        int w = overlay.getWidth() / 16;
        int h = overlay.getHeight() / 16;

        float xFactor = (float) image.getWidth() / (float) (overlay.getWidth() * 2);
        float yFactor = (float) image.getHeight() / (float) (overlay.getHeight() * 2);
        for (int m = border * w; m < k - border * w; ++m) {
            for (int n = border * h; n < l - border * h; ++n) {
                int x1 = (int) (xFactor * m * 2);
                int x2 = (int) (xFactor * (m * 2 + 1));
                int y1 = (int) (yFactor * n * 2);
                int y2 = (int) (yFactor * (n * 2 + 1));
                int baseColor = alphaBlend(image.getPixelRGBA(x1, y1), image.getPixelRGBA(x2, y1), image.getPixelRGBA(x1, y2), image.getPixelRGBA(x2, y2));
                int overlayColor = overlay.getPixelRGBA(m, n);
                nativeImage2.setPixelRGBA(m, n, (baseColor & 0xFF000000) | multiply(baseColor, overlayColor, 16) | multiply(baseColor, overlayColor, 8) | multiply(baseColor, overlayColor, 0));
            }
        }

        nativeImage2.fillRect(w, h, w, h, 0);
        nativeImage2.fillRect(overlay.getWidth() - w * 2, h, w, h, 0);
        nativeImage2.fillRect(w, overlay.getHeight() - h * 2, w, h, 0);
        nativeImage2.fillRect(overlay.getWidth() - w * 2, overlay.getHeight() - h * 2, w, h, 0);

        image.close();
        return nativeImage2;
    }

    private static int multiply(int col1, int col2, int bitOffset) {
        return (((int) ((float) ((col1 >> bitOffset) & 0xFF) * ((float) ((col2 >> bitOffset) & 0xFF) / 255.0F) / (float) COLOR_DIVISIONS) * COLOR_DIVISIONS) & 0xFF) << bitOffset;
    }

    private static int alphaBlend(int col1, int col2, int col3, int col4) {
        int o = gammaBlend(col1, col2, col3, col4, 16);
        int p = gammaBlend(col1, col2, col3, col4, 8);
        int q = gammaBlend(col1, col2, col3, col4, 0);
        return 0xFF000000 | o << 16 | p << 8 | q;
    }

    private static int gammaBlend(int col1, int col2, int col3, int col4, int bitOffset) {
        float f = getPow22(col1 >> bitOffset);
        float g = getPow22(col2 >> bitOffset);
        float h = getPow22(col3 >> bitOffset);
        float i = getPow22(col4 >> bitOffset);
        float j = (float) ((double) ((float) Math.pow((double) (f + g + h + i) * 0.25, 0.45454545454545453)));
        return (int) ((double) j * 255.0);
    }

    private static float getPow22(int val) {
        return POW22[val & 0xFF];
    }
}
