package silly.homak.bigitems.interfaces;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.item.ItemStack;
import silly.homak.bigitems.util.BooleanHolder;

public interface IBigItem {

    // Has to return an array of 2 ints
    int[] getScale();

    // Override this to set a custom background color
    default int getColor() {
        return 0;
    }

    // Returns if the item is currently big or scaled down
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
            MinecraftClient client = MinecraftClient.getInstance();

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