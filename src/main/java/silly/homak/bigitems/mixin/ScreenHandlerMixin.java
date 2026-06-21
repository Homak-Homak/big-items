package silly.homak.bigitems.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silly.homak.bigitems.interfaces.IBigItem;
import silly.homak.bigitems.util.GridUtils;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {

    @Final
    @Shadow
    public DefaultedList<Slot> slots;

    @Shadow
    public abstract ItemStack getCursorStack();

    @Unique
    private boolean bigItems$isRedirectingClick = false;

    @Unique
    private boolean bigItems$isSlotCoveredByOtherBigItem(Slot checkSlot, Slot ignoreAnchor) {
        if (checkSlot == null) return false;
        ScreenHandler currentHandler = (ScreenHandler) (Object) this;
        if (GridUtils.isHotbarSlot(checkSlot)) return false;

        for (Slot s : this.slots) {
            if (s != checkSlot && s != ignoreAnchor && !s.getStack().isEmpty() && s.getStack().getItem() instanceof IBigItem bi) {
                if (GridUtils.isHotbarSlot(s)) continue;

                int[] scale = bi.getScale();
                int gw = scale[0];
                int gh = scale[1];

                if (!GridUtils.shouldEnforceGridConstraints(currentHandler, s)) continue;

                if (gw > 1 || gh > 1) {
                    int left = s.x;
                    int right = s.x + (gw - 1) * 18;
                    int bottom = s.y;
                    int top = s.y - (gh - 1) * 18;

                    if (checkSlot.x >= left && checkSlot.x <= right && checkSlot.y >= top && checkSlot.y <= bottom) {
                        if ((checkSlot.x - left) % 18 == 0 && (bottom - checkSlot.y) % 18 == 0) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Unique
    private boolean bigItems$isValidBigItemPlacement(Slot anchorSlot, IBigItem bigItem) {
        if (GridUtils.isHotbarSlot(anchorSlot)) return true;

        int[] scale = bigItem.getScale();
        int gridWidth = scale[0];
        int gridHeight = scale[1];

        for (int h = 0; h < gridHeight; h++) {
            for (int w = 0; w < gridWidth; w++) {
                int targetX = anchorSlot.x + (w * 18);
                int targetY = anchorSlot.y - ((gridHeight - 1 - h) * 18);

                Slot slotAtPos = null;
                for (Slot s : this.slots) {
                    if (s.x == targetX && s.y == targetY) {
                        slotAtPos = s;
                        break;
                    }
                }

                if (slotAtPos == null) return false;
                if (slotAtPos.inventory != anchorSlot.inventory) return false; // Prevent crossing inv boundaries
                if (slotAtPos != anchorSlot && !slotAtPos.getStack().isEmpty()) return false;
                if (bigItems$isSlotCoveredByOtherBigItem(slotAtPos, anchorSlot)) return false;
            }
        }
        return true;
    }

    @Unique
    private boolean bigItems$tryRedirectClick(Slot slot, IBigItem bigItem, int button, SlotActionType actionType, PlayerEntity player) {
        Slot validAnchor = bigItems$findValidAnchorForSlot(slot, bigItem);
        if (validAnchor != null) {
            bigItems$isRedirectingClick = true;
            try {
                ((ScreenHandler) (Object) this).onSlotClick(this.slots.indexOf(validAnchor), button, actionType, player);
            } finally {
                bigItems$isRedirectingClick = false;
            }
            return true;
        }
        return false;
    }

    @Unique
    private Slot bigItems$findValidAnchorForSlot(Slot clickedSlot, IBigItem bigItem) {
        if (GridUtils.isHotbarSlot(clickedSlot)) return clickedSlot;

        int[] scale = bigItem.getScale();
        int gridWidth = scale[0];
        int gridHeight = scale[1];

        for (int h_offset = 0; h_offset < gridHeight; h_offset++) {
            for (int w_offset = 0; w_offset < gridWidth; w_offset++) {
                int anchorX = clickedSlot.x - (w_offset * 18);
                int anchorY = clickedSlot.y + ((gridHeight - 1 - h_offset) * 18);

                for (Slot potentialAnchor : this.slots) {
                    if (potentialAnchor.x == anchorX && potentialAnchor.y == anchorY) {
                        if (!GridUtils.isHotbarSlot(potentialAnchor) &&
                                bigItems$isValidBigItemPlacement(potentialAnchor, bigItem)) {
                            return potentialAnchor;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void onSlotClick(int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (bigItems$isRedirectingClick || slotId < 0 || slotId >= this.slots.size()) return;

        Slot slot = this.slots.get(slotId);
        ItemStack cursorStack = this.getCursorStack();
        ScreenHandler currentHandler = (ScreenHandler) (Object) this;

        if (!GridUtils.isHotbarSlot(slot) && bigItems$isSlotCoveredByOtherBigItem(slot, null)) {
            ci.cancel();
            return;
        }

        if ((actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_CRAFT) && !cursorStack.isEmpty()) {
            if (cursorStack.getItem() instanceof IBigItem bigItem) {
                if (slot.hasStack() && ItemStack.areItemsAndComponentsEqual(cursorStack, slot.getStack())) return;

                if (GridUtils.isHotbarSlot(slot)) return;

                if (GridUtils.shouldEnforceGridConstraints(currentHandler, slot) &&
                        !bigItems$isValidBigItemPlacement(slot, bigItem)) {
                    bigItems$tryRedirectClick(slot, bigItem, button, actionType, player);
                    ci.cancel();
                    return;
                }
            }
        }

        if (actionType == SlotActionType.SWAP && button >= 0 && button < 9) {
            ItemStack hotbarStack = player.getInventory().getStack(button);

            if (slot.hasStack() && slot.getStack().getItem() instanceof IBigItem slotBigItem) {
                Slot hotbarSlot = null;
                for (Slot s : this.slots) {
                    if (s.inventory == player.getInventory() && s.getIndex() == button) {
                        hotbarSlot = s;
                        break;
                    }
                }
                if (hotbarSlot != null && !GridUtils.isHotbarSlot(hotbarSlot) &&
                        !bigItems$isValidBigItemPlacement(hotbarSlot, slotBigItem)) {
                    ci.cancel();
                    return;
                }
            }

            if (!hotbarStack.isEmpty() && hotbarStack.getItem() instanceof IBigItem bigItem) {
                if (slot.hasStack() && ItemStack.areItemsAndComponentsEqual(hotbarStack, slot.getStack())) return;

                if (GridUtils.isHotbarSlot(slot)) return;

                if (GridUtils.shouldEnforceGridConstraints(currentHandler, slot) &&
                        !bigItems$isValidBigItemPlacement(slot, bigItem)) {
                    bigItems$tryRedirectClick(slot, bigItem, button, actionType, player);
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true)
    private void onInsertItem(ItemStack stack, int startIndex, int endIndex, boolean fromLast, CallbackInfoReturnable<Boolean> cir) {
        if (stack.isEmpty()) return;

        boolean changed = false;
        IBigItem bigItem = (stack.getItem() instanceof IBigItem bi) ? bi : null;
        ScreenHandler currentHandler = (ScreenHandler) (Object) this;

        int i = fromLast ? endIndex - 1 : startIndex;
        while (fromLast ? i >= startIndex : i < endIndex) {
            if (stack.isEmpty()) break;

            Slot slot = this.slots.get(i);
            ItemStack slotStack = slot.getStack();

            if (!slotStack.isEmpty() && ItemStack.areItemsAndComponentsEqual(stack, slotStack)) {
                int maxCount = Math.min(slot.getMaxItemCount(stack), stack.getMaxCount());
                int rem = maxCount - slotStack.getCount();
                if (rem > 0) {
                    int countToMove = Math.min(stack.getCount(), rem);
                    stack.decrement(countToMove);
                    slotStack.increment(countToMove);
                    slot.markDirty();
                    changed = true;
                }
            }
            i += fromLast ? -1 : 1;
        }

        if (!stack.isEmpty()) {
            i = fromLast ? endIndex - 1 : startIndex;
            while (fromLast ? i >= startIndex : i < endIndex) {
                if (stack.isEmpty()) break;

                Slot slot = this.slots.get(i);
                if (slot.getStack().isEmpty() && slot.canInsert(stack)) {
                    boolean canPlace;

                    if (bigItem != null) {
                        if (GridUtils.isHotbarSlot(slot)) {
                            canPlace = true;
                        } else if (!GridUtils.shouldEnforceGridConstraints(currentHandler, slot)) {
                            canPlace = true;
                        } else {
                            canPlace = bigItems$isValidBigItemPlacement(slot, bigItem);
                        }
                    } else {
                        canPlace = !bigItems$isSlotCoveredByOtherBigItem(slot, null);
                    }

                    if (canPlace) {
                        int maxCount = Math.min(slot.getMaxItemCount(stack), stack.getCount());
                        slot.setStack(stack.split(maxCount));
                        slot.markDirty();
                        changed = true;
                    }
                }
                i += fromLast ? -1 : 1;
            }
        }

        cir.setReturnValue(changed);
    }
}