package silly.homak.bigitems.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silly.homak.bigitems.interfaces.IBigItem;
import silly.homak.bigitems.util.BooleanHolder;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow
    protected abstract boolean isPointWithinBounds(int x, int y, int width, int height, double pointX, double pointY);

    @Unique
    private static boolean bigItems$isHotbarSlot(Slot slot) {
        if (slot == null) return false;

        if (slot.inventory instanceof PlayerInventory && slot.getIndex() < 9) {
            return true;
        }

        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (currentScreen instanceof CreativeInventoryScreen creativeScreen) {
            if (creativeScreen.isInventoryTabSelected()) {
                return (slot.getIndex() >= 36 && slot.getIndex() < 45);
            } else {
                return slot.inventory instanceof PlayerInventory && slot.getIndex() < 9;
            }
        }
        return false;
    }

    @Unique
    private static boolean bigItems$shouldEnforceGridConstraints(ScreenHandler handler, Slot slot) {
        if (slot == null) return false;

        if (bigItems$isHotbarSlot(slot)) return false;

        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (currentScreen instanceof CreativeInventoryScreen creativeScreen) {
            if (!creativeScreen.isInventoryTabSelected()) {
                return false;
            }
        }

        if (slot.inventory instanceof PlayerInventory && slot.getIndex() >= 9 && slot.getIndex() < 36) {
            return true;
        }

        if (!(slot.inventory instanceof PlayerInventory)) {
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

            int slotsInThisInventory = 0;
            for (Slot s : handler.slots) {
                if (s.inventory == slot.inventory) {
                    slotsInThisInventory++;
                }
            }

            if (slotsInThisInventory >= 9 && (slotsInThisInventory % 9 == 0)) {
                return true;
            }
        }

        return false;
    }

    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void onBeforeDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        ScreenHandler handler = screen.getScreenHandler();

        if (bigItems$isHotbarSlot(slot) || !bigItems$shouldEnforceGridConstraints(handler, slot)) {
            BooleanHolder.bigItems$bypassScale = true;
        }
    }

    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void onAfterDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        BooleanHolder.bigItems$bypassScale = false;
    }

    @Inject(
            method = "drawSlot",
            at = @At("HEAD")
    )
    private void startSlotRendering(DrawContext context, Slot slot, CallbackInfo ci) {
        BooleanHolder.bigItems$isRenderingSlot = true;
    }

    @Inject(
            method = "drawSlot",
            at = @At("TAIL")
    )
    private void endSlotRendering(DrawContext context, Slot slot, CallbackInfo ci) {
        BooleanHolder.bigItems$isRenderingSlot = false;
    }

    @Inject(
            method = "isPointOverSlot",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onIsPointOverSlot(Slot slot, double pointX, double pointY, CallbackInfoReturnable<Boolean> cir) {
        if (slot == null) return;
        if (bigItems$isHotbarSlot(slot)) return;

        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        ScreenHandler handler = screen.getScreenHandler();

        if (bigItems$shouldEnforceGridConstraints(handler, slot)) {
            if (slot.hasStack() && slot.getStack().getItem() instanceof IBigItem) {
                int[] scale = ((IBigItem) slot.getStack().getItem()).getScale();
                int gridWidth = scale[0];
                int gridHeight = scale[1];

                if (gridWidth > 1 || gridHeight > 1) {
                    int bgLeft = slot.x;
                    int bgTop = slot.y - (gridHeight - 1) * 18;
                    int bgWidth = gridWidth * 18 - 2;
                    int bgHeight = gridHeight * 18 - 2;

                    if (this.isPointWithinBounds(bgLeft, bgTop, bgWidth, bgHeight, pointX, pointY)) {
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }
        }

        for (Slot otherSlot : handler.slots) {
            if (otherSlot != slot && otherSlot.hasStack() && otherSlot.getStack().getItem() instanceof IBigItem) {
                if (bigItems$isHotbarSlot(otherSlot)) continue;
                if (!bigItems$shouldEnforceGridConstraints(handler, otherSlot)) continue;

                int[] scale = ((IBigItem) otherSlot.getStack().getItem()).getScale();
                int gridWidth = scale[0];
                int gridHeight = scale[1];

                if (gridWidth > 1 || gridHeight > 1) {
                    int bgLeft = otherSlot.x;
                    int bgTop = otherSlot.y - (gridHeight - 1) * 18;
                    int bgWidth = gridWidth * 18 - 2;
                    int bgHeight = gridHeight * 18 - 2;

                    if (this.isPointWithinBounds(bgLeft, bgTop, bgWidth, bgHeight, pointX, pointY)) {
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }
    }

    @Inject(
            method = "drawSlotHighlight",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void onRenderSlotHighlight(DrawContext context, int x, int y, int z, CallbackInfo ci) {
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (currentScreen instanceof HandledScreen<?> handledScreen) {
            ScreenHandler handler = handledScreen.getScreenHandler();
            for (Slot slot : handler.slots) {
                if (slot.x == x && slot.y == y && slot.hasStack() && slot.getStack().getItem() instanceof IBigItem) {
                    if (bigItems$isHotbarSlot(slot)) continue;
                    if (!bigItems$shouldEnforceGridConstraints(handler, slot)) continue;

                    int[] scale = ((IBigItem) slot.getStack().getItem()).getScale();
                    int gridWidth = scale[0];
                    int gridHeight = scale[1];

                    if (gridWidth > 1 || gridHeight > 1) {
                        int bgLeft = x;
                        int bgTop = y - (gridHeight - 1) * 18;
                        int bgRight = x + (gridWidth * 18) - 2;
                        int bgBottom = y + 16;

                        context.fillGradient(bgLeft, bgTop, bgRight, bgBottom, z, -2130706433, -2130706433);
                        ci.cancel();
                        return;
                    }
                }
            }
        }
    }
}