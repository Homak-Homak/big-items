package silly.homak.bigitems.interfaces;

import jdk.jfr.Enabled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.item.ItemStack;
import silly.homak.bigitems.util.BooleanHolder;

public interface IBigItem {
    // Implement this in your item class

    // Has to return a 2-length array
    int[] getScale();

    // Overrides the background color, if 0 uses the texture average
    default int getColor() {
        return 0;
    }

    default boolean isScaled(ItemStack stack) {
        int[] scale = getScale();
        if (scale == null || scale.length < 2 || (scale[0] <= 1 && scale[1] <= 1)) {
            return false;
        }

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return ClientContext.isCurrentlyScaled();
        }

        return false;
    }

    @Environment(EnvType.CLIENT)
    class ClientContext {
        private static boolean isCurrentlyScaled() {
            MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();

            if (client == null || client.currentScreen == null) {
                return false;
            }

            if (client.currentScreen instanceof CreativeInventoryScreen creativeScreen) {
                return creativeScreen.isInventoryTabSelected();
            }

            return !BooleanHolder.bigItems$bypassScale;
        }
    }
}