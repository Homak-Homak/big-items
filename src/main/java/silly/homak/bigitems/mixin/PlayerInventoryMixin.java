package silly.homak.bigitems.mixin;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silly.homak.bigitems.interfaces.IBigItem;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {

    @Unique
    private static final ThreadLocal<ItemStack> CURRENT_INSERTING_STACK = ThreadLocal.withInitial(() -> ItemStack.EMPTY);

    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"))
    private void captureStack(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        CURRENT_INSERTING_STACK.set(stack);
    }

    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At("RETURN"))
    private void releaseStack(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        CURRENT_INSERTING_STACK.remove();
    }

    @Inject(method = "insertStack(ILnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"))
    private void captureStackSlot(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        CURRENT_INSERTING_STACK.set(stack);
    }

    @Inject(method = "insertStack(ILnet/minecraft/item/ItemStack;)Z", at = @At("RETURN"))
    private void releaseStackSlot(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        CURRENT_INSERTING_STACK.remove();
    }

    @Unique
    private boolean isSlotCovered(int slot) {
        if (slot < 9 || slot >= 36) return false;

        PlayerInventory inv = (PlayerInventory) (Object) this;
        ItemStack slotStack = inv.main.get(slot);
        if (!slotStack.isEmpty() && slotStack.getItem() instanceof IBigItem) {
            return false;
        }

        int slotRow = (slot - 9) / 9;
        int slotCol = (slot - 9) % 9;

        for (int i = 9; i < 36; i++) {
            if (i == slot) continue;
            ItemStack stack = inv.main.get(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IBigItem bigItem) {
                int[] scale = bigItem.getScale();
                int gridWidth = scale[0];
                int gridHeight = scale[1];
                if (gridWidth <= 1 && gridHeight <= 1) continue;

                int anchorRow = (i - 9) / 9;
                int anchorCol = (i - 9) % 9;

                int rowOffset = anchorRow - slotRow;
                int colOffset = slotCol - anchorCol;

                if (rowOffset >= 0 && rowOffset < gridHeight &&
                        colOffset >= 0 && colOffset < gridWidth) {
                    return true;
                }
            }
        }
        return false;
    }

    @Unique
    private boolean canBigItemFit(int slot, IBigItem bigItem) {
        if (slot < 9 || slot >= 36) return true;

        int[] scale = bigItem.getScale();
        int gridWidth = scale[0];
        int gridHeight = scale[1];
        if (gridWidth <= 1 && gridHeight <= 1) return true;

        int anchorRow = (slot - 9) / 9;
        int anchorCol = (slot - 9) % 9;

        int topRow = anchorRow - (gridHeight - 1);
        int rightCol = anchorCol + (gridWidth - 1);

        if (topRow < 0) return false;
        if (rightCol >= 9) return false;

        PlayerInventory inv = (PlayerInventory) (Object) this;
        for (int r = 0; r < gridHeight; r++) {
            for (int c = 0; c < gridWidth; c++) {
                int targetRow = anchorRow - r;
                int targetCol = anchorCol + c;
                int targetSlot = 9 + (targetRow * 9) + targetCol;

                if (!inv.main.get(targetSlot).isEmpty() || isSlotCovered(targetSlot)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Inject(method = "getEmptySlot", at = @At("HEAD"), cancellable = true)
    private void getEmptySlot(CallbackInfoReturnable<Integer> cir) {
        PlayerInventory inv = (PlayerInventory) (Object) this;
        ItemStack inserting = CURRENT_INSERTING_STACK.get();
        boolean isBigItem = !inserting.isEmpty() && inserting.getItem() instanceof IBigItem;
        IBigItem bigItem = isBigItem ? (IBigItem) inserting.getItem() : null;

        for (int i = 0; i < inv.main.size(); i++) {
            if (inv.main.get(i).isEmpty() && !isSlotCovered(i)) {
                if (isBigItem && !canBigItemFit(i, bigItem)) {
                    continue;
                }
                cir.setReturnValue(i);
                return;
            }
        }
        cir.setReturnValue(-1);
    }

    @Inject(method = "getOccupiedSlotWithRoomForStack", at = @At("HEAD"), cancellable = true)
    private void getOccupiedSlotWithRoom(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        PlayerInventory inv = (PlayerInventory) (Object) this;
        if (stack.isEmpty()) {
            cir.setReturnValue(-1);
            return;
        }
        for (int i = 0; i < inv.main.size(); i++) {
            ItemStack slotStack = inv.main.get(i);
            if (!slotStack.isEmpty() && !isSlotCovered(i) &&
                    ItemStack.areItemsAndComponentsEqual(slotStack, stack) &&
                    slotStack.getCount() < slotStack.getMaxCount() &&
                    slotStack.getCount() < inv.getMaxCountPerStack()) {
                cir.setReturnValue(i);
                return;
            }
        }
        cir.setReturnValue(-1);
    }

    @Inject(method = "getStack", at = @At("HEAD"), cancellable = true)
    private void virtualizeCoveredSlotsAsEmpty(int slot, CallbackInfoReturnable<ItemStack> cir) {
        PlayerInventory inv = (PlayerInventory) (Object) this;
        if (slot >= 0 && slot < 36) {
            ItemStack slotStack = inv.main.get(slot);
            if (!slotStack.isEmpty() && slotStack.getItem() instanceof IBigItem) {
                return;
            }
            if (isSlotCovered(slot)) {
                cir.setReturnValue(ItemStack.EMPTY);
            }
        }
    }
}