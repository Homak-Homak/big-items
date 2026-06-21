package silly.homak.bigitems.util;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.item.Item;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import silly.homak.bigitems.mixin.SpriteContentsAccessor;

import java.awt.Color;

public class TextureUtils {

    public static int getAverageColor(Item item) {
        Identifier itemId = net.minecraft.registry.Registries.ITEM.getId(item);
        Identifier spriteId = Identifier.of(itemId.getNamespace(), "item/" + itemId.getPath());
        SpriteIdentifier spriteIdentifier = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, spriteId);

        Sprite sprite = spriteIdentifier.getSprite();
        NativeImage nativeImage = ((SpriteContentsAccessor) sprite.getContents()).getImage();
        if (nativeImage == null) {
            return 0x7F000000;
        }

        long totalR = 0, totalG = 0, totalB = 0, totalA = 0;
        int pixelCount = 0;

        int width = sprite.getContents().getWidth();
        int height = sprite.getContents().getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = nativeImage.getColor(x, y);
                int a = (color >> 24) & 0xFF;
                int b = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int r = color & 0xFF;

                if (a > 10) {
                    totalR += r;
                    totalG += g;
                    totalB += b;
                    totalA += a;
                    pixelCount++;
                }
            }
        }

        if (pixelCount == 0) return 0x7F000000;

        return ((int) (totalA / pixelCount) << 24) |
                ((int) (totalR / pixelCount) << 16) |
                ((int) (totalG / pixelCount) << 8)  |
                (int) (totalB / pixelCount);
    }

    public static int darkenColor(int color, double factor) {
        factor = Math.clamp(factor, 0.0, 1.0);

        int a = (color >> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int fadeColor(int color, double factor) {
        return (((int) (factor * 255.0f)) << 24) | (color & 0x00FFFFFF);
    }

    public static int desaturateColor(int intColor, double factor) {
        int r = (intColor >> 16) & 0xFF;
        int g = (intColor >> 8) & 0xFF;
        int b = intColor & 0xFF;
        int alpha = (intColor >> 24) & 0xFF;

        float[] hsbValues = new float[3];
        Color.RGBtoHSB(r, g, b, hsbValues);
        int rgbResult = Color.HSBtoRGB(hsbValues[0], (float) Math.max(0.0f, hsbValues[1] * factor), hsbValues[2]);

        return alpha != 0 ? (rgbResult & 0x00FFFFFF) | (alpha << 24) : rgbResult;
    }
}