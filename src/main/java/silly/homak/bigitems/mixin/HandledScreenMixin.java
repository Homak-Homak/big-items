package silly.homak.bigitems.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silly.homak.bigitems.interfaces.IBigItem;
import silly.homak.bigitems.util.BooleanHolder;
import silly.homak.bigitems.util.GridUtils;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow
    protected abstract boolean isPointWithinBounds(int x, int y, int width, int height, double pointX, double pointY);

    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void onBeforeDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        ScreenHandler handler = screen.getScreenHandler();

        if (GridUtils.isHotbarSlot(slot)) {
            BooleanHolder.bigItems$bypassScale = false;
            BooleanHolder.bigItems$isHotbarSlot = true;
        } else if (!GridUtils.shouldEnforceGridConstraints(handler, slot)) {
            BooleanHolder.bigItems$bypassScale = true;
            BooleanHolder.bigItems$isHotbarSlot = false;
        } else {
            BooleanHolder.bigItems$bypassScale = false;
            BooleanHolder.bigItems$isHotbarSlot = false;
        }
        BooleanHolder.bigItems$isRenderingSlot = true;
    }

    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void onAfterDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        BooleanHolder.bigItems$bypassScale = false;
        BooleanHolder.bigItems$isHotbarSlot = false;
        BooleanHolder.bigItems$isRenderingSlot = false;
    }

    @Inject(method = "isPointOverSlot", at = @At("HEAD"), cancellable = true)
    private void onIsPointOverSlot(Slot slot, double pointX, double pointY, CallbackInfoReturnable<Boolean> cir) {
        if (slot == null) return;
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        ScreenHandler handler = screen.getScreenHandler();

        if (GridUtils.isHotbarSlot(slot)) return;

        if (GridUtils.shouldEnforceGridConstraints(handler, slot) &&
                slot.hasStack() && slot.getStack().getItem() instanceof IBigItem bigItem) {
            int[] scale = bigItem.getScale();
            if (scale[0] > 1 || scale[1] > 1) {
                int width = scale[0] * 18 - 2;
                int height = scale[1] * 18 - 2;
                int x = slot.x;
                int y = slot.y - (scale[1] - 1) * 18;
                if (this.isPointWithinBounds(x, y, width, height, pointX, pointY)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }

        for (Slot otherSlot : handler.slots) {
            if (otherSlot != slot && otherSlot.hasStack() && otherSlot.getStack().getItem() instanceof IBigItem bigItem) {
                if (GridUtils.isHotbarSlot(otherSlot)) continue;

                if (GridUtils.shouldEnforceGridConstraints(handler, otherSlot)) {
                    int[] scale = bigItem.getScale();
                    if (scale[0] > 1 || scale[1] > 1) {
                        int width = scale[0] * 18 - 2;
                        int height = scale[1] * 18 - 2;
                        int x = otherSlot.x;
                        int y = otherSlot.y - (scale[1] - 1) * 18;
                        if (this.isPointWithinBounds(x, y, width, height, pointX, pointY)) {
                            cir.setReturnValue(false);
                            return;
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "drawSlotHighlight", at = @At("HEAD"), cancellable = true)
    private static void onRenderSlotHighlight(DrawContext context, int x, int y, int z, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof HandledScreen<?> handledScreen) {
            ScreenHandler handler = handledScreen.getScreenHandler();
            for (Slot slot : handler.slots) {
                if (slot.x == x && slot.y == y && slot.hasStack() && slot.getStack().getItem() instanceof IBigItem bigItem) {
                    if (GridUtils.isHotbarSlot(slot)) return;

                    if (GridUtils.shouldEnforceGridConstraints(handler, slot)) {
                        int[] scale = bigItem.getScale();
                        if (scale[0] > 1 || scale[1] > 1) {
                            int width = scale[0] * 18 - 2;
                            int height = scale[1] * 18 - 2;
                            int xPos = slot.x;
                            int yPos = slot.y - (scale[1] - 1) * 18;
                            context.fillGradient(xPos, yPos, xPos + width, yPos + height, z, -2130706433, -2130706433);
                            ci.cancel();
                            return;
                        }
                    }
                }
            }
        }
    }
}