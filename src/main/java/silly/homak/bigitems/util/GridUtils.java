package silly.homak.bigitems.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class GridUtils {

    public static boolean isHotbarSlot(Slot slot) {
        if (slot == null) return false;

        if (slot.inventory instanceof PlayerInventory && slot.getIndex() < 9) {
            return true;
        }

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return ClientOnly.isCreativeHotbarSlot(slot);
        }

        return false;
    }

    public static boolean shouldEnforceGridConstraints(ScreenHandler handler, Slot slot) {
        if (slot == null || isHotbarSlot(slot)) return false;

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            if (ClientOnly.isCreativeInventoryTabNotSelected()) {
                return false;
            }
        }

        if (slot.inventory instanceof PlayerInventory) {
            return slot.getIndex() >= 9 && slot.getIndex() < 36;
        }

        String handlerName = handler.getClass().getName().toLowerCase();
        String invName = slot.inventory.getClass().getName().toLowerCase();

        if (handlerName.contains("furnace") || handlerName.contains("brew") || handlerName.contains("crafting") ||
                handlerName.contains("anvil") || handlerName.contains("grindstone") || handlerName.contains("enchant") ||
                handlerName.contains("hopper") || handlerName.contains("dispenser") || handlerName.contains("dropper") ||
                handlerName.contains("machine") || handlerName.contains("generator") || handlerName.contains("smelter") ||
                handlerName.contains("alloy") || handlerName.contains("crusher") || handlerName.contains("assembler")) {
            return false;
        }

        if (handlerName.contains("chest") || handlerName.contains("barrel") || handlerName.contains("shulker") ||
                handlerName.contains("storage") || handlerName.contains("crate") || handlerName.contains("vault") ||
                handlerName.contains("backpack") || handlerName.contains("dank") || handlerName.contains("container") ||
                handlerName.contains("cabinet") || handlerName.contains("colossal") ||
                invName.contains("chest") || invName.contains("barrel") || invName.contains("storage")) {
            return true;
        }

        return false;
    }

    private static class ClientOnly {
        private static boolean isCreativeHotbarSlot(Slot slot) {
            Screen currentScreen = MinecraftClient.getInstance().currentScreen;
            if (currentScreen instanceof CreativeInventoryScreen creativeScreen) {
                if (creativeScreen.isInventoryTabSelected()) {
                    return slot.getIndex() >= 36 && slot.getIndex() < 45;
                } else {
                    return slot.inventory instanceof PlayerInventory && slot.getIndex() < 9;
                }
            }
            return false;
        }

        private static boolean isCreativeInventoryTabNotSelected() {
            Screen currentScreen = MinecraftClient.getInstance().currentScreen;
            return currentScreen instanceof CreativeInventoryScreen creativeScreen && !creativeScreen.isInventoryTabSelected();
        }
    }
}