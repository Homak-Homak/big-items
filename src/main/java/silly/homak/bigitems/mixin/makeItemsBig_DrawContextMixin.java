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

@Mixin(DrawContext.class)
public abstract class makeItemsBig_DrawContextMixin {
    @Shadow
    public abstract MatrixStack getMatrices();
    @Unique
    private static final float SHADOW_OFFSET = 0.55f;
    @Unique
    private static final float SHADOW_ALPHA = 0.3f;

    @Inject(
            method = "drawItem(Lnet/minecraft/item/ItemStack;III)V",
            at = @At("HEAD")
    )
    private void scaleBigItem(ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        if (BooleanHolder.bigItems$bypassScale || BooleanHolder.bigItems$isRenderingShadow || MinecraftClient.getInstance().currentScreen == null) return;

        if (MinecraftClient.getInstance().currentScreen instanceof CreativeInventoryScreen creativeScreen && !creativeScreen.isInventoryTabSelected()) return;

        if (stack.getItem() instanceof IBigItem) {
            int[] scale = ((IBigItem) stack.getItem()).getScale();
            int gridWidth = scale[0];
            int gridHeight = scale[1];

            if (gridWidth > 1 || gridHeight > 1) {
                DrawContext context = (DrawContext) (Object) this;

                int backgroundColor = TextureUtils.desaturateColor(TextureUtils.fadeColor(TextureUtils.darkenColor(TextureUtils.getAverageColor(stack.getItem()), 0.75), 1), 0.95);

                if (((IBigItem) stack.getItem()).getColor() != 0) {
                    backgroundColor = ((IBigItem) stack.getItem()).getColor();
                }

                int bgLeft = x;
                int bgTop = y - (gridHeight - 1) * 18;
                int bgRight = x + (gridWidth * 18) - 2;
                int bgBottom = y + 16;

                int outlineColor = TextureUtils.darkenColor(backgroundColor, 0.55);

                context.fill(bgLeft - 1, bgTop - 1, bgRight + 1, bgBottom + 1, outlineColor);
                context.fill(bgLeft, bgTop, bgRight, bgBottom, backgroundColor);

                float uniformScale = Math.min(gridWidth, gridHeight);
                float bgCenterX = x + (gridWidth * 18 - 2) / 2.0f;
                float bgCenterY = y - (gridHeight * 9) + 17.0f;
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

    @Inject(
            method = "drawItem(Lnet/minecraft/item/ItemStack;III)V",
            at = @At("TAIL")
    )
    private void resetItemScale(ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        if (BooleanHolder.bigItems$bypassScale || BooleanHolder.bigItems$isRenderingShadow || MinecraftClient.getInstance().currentScreen == null) return;
        if (MinecraftClient.getInstance().currentScreen instanceof CreativeInventoryScreen creativeScreen && !creativeScreen.isInventoryTabSelected()) return;

        if (stack.getItem() instanceof IBigItem) {
            int[] scale = ((IBigItem) stack.getItem()).getScale();
            if (scale[0] > 1 || scale[1] > 1) {
                this.getMatrices().pop();
            }
        }
    }

    @Inject(
            method = "drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void scaleAndMoveItemDecorations(TextRenderer textRenderer, ItemStack stack, int x, int y, String countOverride, CallbackInfo ci) {
        if (BooleanHolder.bigItems$bypassScale || MinecraftClient.getInstance().currentScreen == null) return;

        if (MinecraftClient.getInstance().currentScreen instanceof CreativeInventoryScreen creativeScreen && !creativeScreen.isInventoryTabSelected()) return;

        if (!BooleanHolder.bigItems$isRenderingSlot) return;

        if (!stack.isEmpty() && stack.getItem() instanceof IBigItem) {
            int[] scale = ((IBigItem) stack.getItem()).getScale();
            int gridWidth = scale[0];
            int gridHeight = scale[1];

            if (gridWidth > 1 || gridHeight > 1) {
                ci.cancel();

                int bgLeft = x;
                int bgTop = y - (gridHeight - 1) * 18;
                int bgRight = x + (gridWidth * 18) - 2;
                int bgBottom = y + 16;

                MatrixStack matrices = this.getMatrices();
                DrawContext context = (DrawContext) (Object) this;

                net.minecraft.client.network.ClientPlayerEntity player = MinecraftClient.getInstance().player;
                if (player != null) {
                    float cooldownProgress = player.getItemCooldownManager().getCooldownProgress(stack.getItem(), 0.0F);
                    if (cooldownProgress > 0.0F) {
                        int totalHeight = bgBottom - bgTop;
                        int cooldownHeight = Math.round(cooldownProgress * totalHeight);
                        context.fill(bgLeft, bgBottom - cooldownHeight, bgRight, bgBottom, 0x7FFFFFFF);
                    }
                }

                if (stack.isItemBarVisible()) {
                    int barX = x + 2;
                    int barY = y + 13;
                    int totalBarWidth = (gridWidth * 18) - 6;

                    float stepFraction = stack.getItemBarStep() / 13.0f;
                    int fillWidth = Math.round(stepFraction * totalBarWidth);
                    int barColor = stack.getItemBarColor();

                    context.fill(barX, barY, barX + totalBarWidth, barY + 2, 0xFF000000);
                    context.fill(barX, barY, barX + fillWidth, barY + 1, barColor | 0xFF000000);
                }

                if (stack.getCount() > 1 || countOverride != null) {
                    String countText = countOverride != null ? countOverride : String.valueOf(stack.getCount());
                    int textWidth = textRenderer.getWidth(countText);

                    float uniformScale = Math.min(gridWidth, gridHeight);
                    float textScale = 1.0f + (uniformScale - 1.0f) * 0.25f;

                    float renderX = (bgRight + 1) - (textWidth * textScale);
                    float renderY = bgBottom - (7 * textScale);

                    matrices.push();
                    matrices.translate(renderX, renderY, 200.0f);
                    matrices.scale(textScale, textScale, 1.0f);

                    context.drawText(textRenderer, countText, 0, 0, 0xFFFFFF, true);
                    matrices.pop();
                }
            }
        }
    }
}