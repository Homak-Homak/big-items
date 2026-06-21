package silly.homak.bigitems.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silly.homak.bigitems.util.BooleanHolder;
import silly.homak.bigitems.util.TextureUtils;
import silly.homak.bigitems.interfaces.IBigItem;
import silly.homak.bigitems.util.GridUtils;

@Mixin(DrawContext.class)
public abstract class makeItemsBig_DrawContextMixin {
    @Shadow
    public abstract MatrixStack getMatrices();

    @Unique private static final float SHADOW_OFFSET = 0.55f;
    @Unique private static final float SHADOW_ALPHA = 0.3f;

    @Unique
    private boolean bigItems$shouldBypassScaling() {
        if (BooleanHolder.bigItems$isRenderingShadow) return true;
        if (MinecraftClient.getInstance().currentScreen == null) return false;
        if (BooleanHolder.bigItems$bypassScale) return true;
        if (MinecraftClient.getInstance().currentScreen instanceof CreativeInventoryScreen creativeScreen &&
                !creativeScreen.isInventoryTabSelected()) {
            return true;
        }
        return false;
    }

    @Unique
    private boolean bigItems$isInHotbar(ItemStack stack) {
        if (MinecraftClient.getInstance().player == null) return false;

        for (int i = 0; i < 9; i++) {
            if (MinecraftClient.getInstance().player.getInventory().getStack(i) == stack) {
                return true;
            }
        }
        return false;
    }

    @Inject(method = "drawItem(Lnet/minecraft/item/ItemStack;III)V", at = @At("HEAD"))
    private void scaleBigItem(ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        if (bigItems$shouldBypassScaling()) return;

        if (stack.getItem() instanceof IBigItem bigItem) {
            int[] scale = bigItem.getScale();
            int gridWidth = scale[0];
            int gridHeight = scale[1];

            boolean isHotbarSlot = BooleanHolder.bigItems$isHotbarSlot || bigItems$isInHotbar(stack);

            if (isHotbarSlot) {
                return;
            }

            if (gridWidth > 1 || gridHeight > 1) {
                DrawContext context = (DrawContext) (Object) this;

                int backgroundColor = bigItem.getColor() != 0 ? bigItem.getColor() :
                        TextureUtils.desaturateColor(TextureUtils.fadeColor(TextureUtils.darkenColor(TextureUtils.getAverageColor(stack.getItem()), 0.75), 1), 0.95);

                int step = 18;
                int padding = step - 16;

                int bgLeft = x;
                int bgTop = y - (gridHeight - 1) * step;
                int bgRight = x + (gridWidth * step) - padding;
                int bgBottom = y + 16;

                context.fill(bgLeft - 1, bgTop - 1, bgRight + 1, bgBottom + 1, TextureUtils.darkenColor(backgroundColor, 0.55));
                context.fill(bgLeft, bgTop, bgRight, bgBottom, backgroundColor);

                float uniformScale = Math.min(gridWidth, gridHeight);
                float bgCenterX = (bgLeft + bgRight) / 2.0f;
                float bgCenterY = (bgTop + bgBottom) / 2.0f;
                float itemCenterX = x + 8.0f;
                float itemCenterY = y + 8.0f;

                MatrixStack matrices = this.getMatrices();

                matrices.push();
                matrices.translate(bgCenterX, bgCenterY, 0.0f);
                matrices.scale(uniformScale, uniformScale, 1.0f);
                matrices.translate(-itemCenterX + SHADOW_OFFSET, -itemCenterY + SHADOW_OFFSET, 0.0f);

                RenderSystem.setShaderColor(0.0f, 0.0f, 0.0f, SHADOW_ALPHA);
                BooleanHolder.bigItems$isRenderingShadow = true;
                try {
                    context.drawItem(stack, x, y, seed);
                } finally {
                    BooleanHolder.bigItems$isRenderingShadow = false;
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                }

                matrices.pop();
                matrices.push();

                matrices.translate(bgCenterX, bgCenterY, 0.0f);
                matrices.scale(uniformScale, uniformScale, 1.0f);
                matrices.translate(-itemCenterX, -itemCenterY, 0.0f);
            }
        }
    }

    @Inject(method = "drawItem(Lnet/minecraft/item/ItemStack;III)V", at = @At("TAIL"))
    private void resetItemScale(ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        if (bigItems$shouldBypassScaling()) return;

        if (stack.getItem() instanceof IBigItem bigItem) {
            int[] scale = bigItem.getScale();

            boolean isHotbarSlot = BooleanHolder.bigItems$isHotbarSlot || bigItems$isInHotbar(stack);
            if (isHotbarSlot) return;

            if (scale[0] > 1 || scale[1] > 1) {
                this.getMatrices().pop();
            }
        }
    }

    @Inject(method = "drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void scaleAndMoveItemDecorations(TextRenderer textRenderer, ItemStack stack, int x, int y, String countOverride, CallbackInfo ci) {
        boolean isHud = MinecraftClient.getInstance().currentScreen == null;
        if (BooleanHolder.bigItems$bypassScale || (!BooleanHolder.bigItems$isRenderingSlot && !isHud)) return;
        if (MinecraftClient.getInstance().currentScreen instanceof CreativeInventoryScreen creativeScreen && !creativeScreen.isInventoryTabSelected()) return;

        if (!stack.isEmpty() && stack.getItem() instanceof IBigItem bigItem) {
            int[] scale = bigItem.getScale();
            int gridWidth = scale[0];
            int gridHeight = scale[1];

            boolean isHotbarSlot = BooleanHolder.bigItems$isHotbarSlot || bigItems$isInHotbar(stack);
            if (isHotbarSlot) return;

            if (gridWidth > 1 || gridHeight > 1) {
                ci.cancel();

                int step = 18;
                int padding = step - 16;

                int bgLeft = x;
                int bgTop = y - (gridHeight - 1) * step;
                int bgRight = x + (gridWidth * step) - padding;
                int bgBottom = y + 16;

                MatrixStack matrices = this.getMatrices();
                DrawContext context = (DrawContext) (Object) this;

                net.minecraft.client.network.ClientPlayerEntity player = MinecraftClient.getInstance().player;
                if (player != null) {
                    float cooldownProgress = player.getItemCooldownManager().getCooldownProgress(stack.getItem(), 0.0F);
                    if (cooldownProgress > 0.0F) {
                        context.fill(bgLeft, bgBottom - Math.round(cooldownProgress * (bgBottom - bgTop)), bgRight, bgBottom, 0x7FFFFFFF);
                    }
                }

                if (stack.isItemBarVisible()) {
                    int barX = x + 2;
                    int barY = y + 13;
                    int totalBarWidth = (gridWidth * step) - (padding + 4);
                    int fillWidth = Math.round((stack.getItemBarStep() / 13.0f) * totalBarWidth);

                    context.fill(barX, barY, barX + totalBarWidth, barY + 2, 0xFF000000);
                    context.fill(barX, barY, barX + fillWidth, barY + 1, stack.getItemBarColor() | 0xFF000000);
                }

                if (stack.getCount() > 1 || countOverride != null) {
                    String countText = countOverride != null ? countOverride : String.valueOf(stack.getCount());
                    float textScale = 1.0f + (Math.min(gridWidth, gridHeight) - 1.0f) * 0.25f;

                    matrices.push();
                    matrices.translate((bgRight + 1) - (textRenderer.getWidth(countText) * textScale), bgBottom - (7 * textScale), 200.0f);
                    matrices.scale(textScale, textScale, 1.0f);

                    context.drawText(textRenderer, countText, 0, 0, 0xFFFFFF, true);
                    matrices.pop();
                }
            }
        }
    }
}