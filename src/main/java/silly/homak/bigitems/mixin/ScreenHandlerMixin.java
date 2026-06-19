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
    private boolean bigItems$isHotbarSlot(Slot slot) {
        return slot != null && slot.inventory instanceof net.minecraft.entity.player.PlayerInventory && slot.getIndex() < 9;
    }

    @Unique
    private static boolean bigItems$shouldEnforceGridConstraints(ScreenHandler handler, Slot slot) {
        if (slot == null) return false;

        if (slot.inventory instanceof net.minecraft.entity.player.PlayerInventory && slot.getIndex() >= 9 && slot.getIndex() < 36) {
            return true;
        }

        if (!(slot.inventory instanceof net.minecraft.entity.player.PlayerInventory)) {
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

    @Unique
    private Slot bigItems$getAnchorSlotForCoveredSlot(Slot checkSlot) {
        if (checkSlot == null) return null;
        for (Slot s : this.slots) {
            if (s != checkSlot && !s.getStack().isEmpty() && s.getStack().getItem() instanceof IBigItem bi) {
                if (bigItems$isHotbarSlot(s)) continue;
                if (!bigItems$shouldEnforceGridConstraints((ScreenHandler) (Object) this, s)) continue;

                int[] scale = bi.getScale();
                int gw = scale[0];
                int gh = scale[1];

                if (gw > 1 || gh > 1) {
                    int left = s.x;
                    int right = s.x + (gw - 1) * 18;
                    int bottom = s.y;
                    int top = s.y - (gh - 1) * 18;

                    if (checkSlot.x >= left && checkSlot.x <= right && checkSlot.y >= top && checkSlot.y <= bottom) {
                        if ((checkSlot.x - left) % 18 == 0 && (bottom - checkSlot.y) % 18 == 0) {
                            return s;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Inject(
            method = "onSlotClick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onSlotClick(int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (bigItems$isRedirectingClick) return;

        if (slotId < 0 || slotId >= this.slots.size()) return;
        Slot slot = this.slots.get(slotId);
        ItemStack cursorStack = this.getCursorStack();

        Slot anchorSlot = bigItems$getAnchorSlotForCoveredSlot(slot);
        if (anchorSlot != null && anchorSlot != slot) {
            bigItems$isRedirectingClick = true;
            try {
                int anchorIndex = this.slots.indexOf(anchorSlot);
                ((ScreenHandler) (Object) this).onSlotClick(anchorIndex, button, actionType, player);
            } finally {
                bigItems$isRedirectingClick = false;
            }
            ci.cancel();
            return;
        }

        if ((actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_CRAFT) && !cursorStack.isEmpty()) {
            if (cursorStack.getItem() instanceof IBigItem bigItem) {
                if (bigItems$isHotbarSlot(slot)) {
                    return;
                }

                if (slot.hasStack() && ItemStack.areItemsAndComponentsEqual(cursorStack, slot.getStack())) {
                    return;
                }

                if (bigItems$shouldEnforceGridConstraints((ScreenHandler) (Object) this, slot)) {
                    if (!bigItems$isValidBigItemPlacement(slot, bigItem)) {
                        if (bigItems$tryRedirectClick(slot, bigItem, button, actionType, player)) {
                            ci.cancel();
                            return;
                        }
                        ci.cancel();
                        return;
                    }
                }
            }
        }

        if (actionType == SlotActionType.SWAP && button >= 0 && button < 9) {
            ItemStack hotbarStack = player.getInventory().getStack(button);
            if (!hotbarStack.isEmpty() && hotbarStack.getItem() instanceof IBigItem bigItem) {
                if (bigItems$isHotbarSlot(slot)) {
                    return;
                }

                if (slot.hasStack() && ItemStack.areItemsAndComponentsEqual(hotbarStack, slot.getStack())) {
                    return;
                }

                if (bigItems$shouldEnforceGridConstraints((ScreenHandler) (Object) this, slot)) {
                    if (!bigItems$isValidBigItemPlacement(slot, bigItem)) {
                        if (bigItems$tryRedirectClick(slot, bigItem, button, actionType, player)) {
                            ci.cancel();
                            return;
                        }
                        ci.cancel();
                        return;
                    }
                }
            }
        }

        if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.SWAP || actionType == SlotActionType.QUICK_CRAFT) {
            if (bigItems$isSlotCoveredByOtherBigItem(slot, null)) {
                if (actionType == SlotActionType.PICKUP && cursorStack.isEmpty() && slot.hasStack() && slot.getStack().getItem() instanceof IBigItem) {
                    return;
                }
                ci.cancel();
            }
        }
    }

    @Inject(
            method = "insertItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onInsertItem(ItemStack stack, int startIndex, int endIndex, boolean fromLast, CallbackInfoReturnable<Boolean> cir) {
        if (stack.isEmpty()) return;

        boolean changed = false;

        if (stack.getItem() instanceof IBigItem bigItem) {
            int i = fromLast ? endIndex - 1 : startIndex;
            while (!stack.isEmpty()) {
                if (fromLast ? i < startIndex : i >= endIndex) break;

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
                while (!stack.isEmpty()) {
                    if (fromLast ? i < startIndex : i >= endIndex) break;

                    Slot slot = this.slots.get(i);
                    if (slot.getStack().isEmpty() && slot.canInsert(stack)) {
                        if (bigItems$isHotbarSlot(slot)) {
                            int maxCount = Math.min(slot.getMaxItemCount(stack), stack.getCount());
                            slot.setStack(stack.split(maxCount));
                            slot.markDirty();
                            changed = true;
                        } else if (bigItems$shouldEnforceGridConstraints((ScreenHandler) (Object) this, slot)) {
                            if (bigItems$isValidBigItemPlacement(slot, bigItem)) {
                                int maxCount = Math.min(slot.getMaxItemCount(stack), stack.getCount());
                                slot.setStack(stack.split(maxCount));
                                slot.markDirty();
                                changed = true;
                            }
                        } else {
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
            return;
        }

        if (stack.isStackable()) {
            int i = fromLast ? endIndex - 1 : startIndex;
            while (!stack.isEmpty()) {
                if (fromLast ? i < startIndex : i >= endIndex) break;

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
        }

        if (!stack.isEmpty()) {
            int i = fromLast ? endIndex - 1 : startIndex;
            while (!stack.isEmpty()) {
                if (fromLast ? i < startIndex : i >= endIndex) break;

                Slot slot = this.slots.get(i);
                if (slot.getStack().isEmpty() && slot.canInsert(stack) && !bigItems$isSlotCoveredByOtherBigItem(slot, null)) {
                    int maxCount = Math.min(slot.getMaxItemCount(stack), stack.getCount());
                    slot.setStack(stack.split(maxCount));
                    slot.markDirty();
                    changed = true;
                }
                i += fromLast ? -1 : 1;
            }
        }

        cir.setReturnValue(changed);
    }

    @Unique
    private boolean bigItems$tryRedirectClick(Slot slot, IBigItem bigItem, int button, SlotActionType actionType, PlayerEntity player) {
        Slot validAnchor = bigItems$findValidAnchorForSlot(slot, bigItem);
        if (validAnchor != null) {
            bigItems$isRedirectingClick = true;
            try {
                int anchorIndex = this.slots.indexOf(validAnchor);
                ((ScreenHandler) (Object) this).onSlotClick(anchorIndex, button, actionType, player);
            } finally {
                bigItems$isRedirectingClick = false;
            }
            return true;
        }
        return false;
    }

    @Unique
    private Slot bigItems$findValidAnchorForSlot(Slot clickedSlot, IBigItem bigItem) {
        int[] scale = bigItem.getScale();
        int gridWidth = scale[0];
        int gridHeight = scale[1];

        for (int h_offset = 0; h_offset < gridHeight; h_offset++) {
            for (int w_offset = 0; w_offset < gridWidth; w_offset++) {
                int anchorX = clickedSlot.x - (w_offset * 18);
                int anchorY = clickedSlot.y + ((gridHeight - 1 - h_offset) * 18);

                for (Slot potentialAnchor : this.slots) {
                    if (potentialAnchor.x == anchorX && potentialAnchor.y == anchorY) {
                        if (bigItems$isHotbarSlot(potentialAnchor)) continue;
                        if (bigItems$isValidBigItemPlacement(potentialAnchor, bigItem)) {
                            return potentialAnchor;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Unique
    private boolean bigItems$isValidBigItemPlacement(Slot anchorSlot, IBigItem bigItem) {
        if (bigItems$isHotbarSlot(anchorSlot)) return true;

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
                if (slotAtPos != anchorSlot && !slotAtPos.getStack().isEmpty()) return false;
                if (bigItems$isSlotCoveredByOtherBigItem(slotAtPos, anchorSlot)) return false;
            }
        }
        return true;
    }

    @Unique
    private boolean bigItems$isSlotCoveredByOtherBigItem(Slot checkSlot, Slot ignoreAnchor) {
        for (Slot s : this.slots) {
            if (s != ignoreAnchor && !s.getStack().isEmpty() && s.getStack().getItem() instanceof IBigItem bi) {
                if (bigItems$isHotbarSlot(s)) continue;
                if (!bigItems$shouldEnforceGridConstraints((ScreenHandler) (Object) this, s)) continue;

                int[] scale = bi.getScale();
                int gw = scale[0];
                int gh = scale[1];

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
}