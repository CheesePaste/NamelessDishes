// com.cp.nd.client.gui.toast.RecipeUnlockToast.java
package com.cp.nd.gui.toast;

import com.cp.nd.NamelessDishes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class RecipeUnlockToast implements Toast {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NamelessDishes.MOD_ID, "textures/gui/toast3.png");

    private final Recipe<?> recipe;
    private final Component title;
    private final Component description;
    private long lastChanged;
    private boolean playedSound;
    private float animationProgress = 0.0f;

    public RecipeUnlockToast(Recipe<?> recipe) {
        this.recipe = recipe;
        this.title = Component.translatable("toast.nameless_dishes.recipe_unlocked")
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        this.description = Component.translatable(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()).getDescriptionId())
                .withStyle(ChatFormatting.WHITE);
        this.lastChanged = System.currentTimeMillis();
        this.playedSound = false;
        this.animationProgress = 0.0f;
    }

    @Override
    public @NotNull Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long timeSinceLastVisible) {
        // 更新动画进度
        animationProgress = Mth.clamp(animationProgress + 0.05f, 0.0f, 1.0f);

        if (!this.playedSound) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.7F)
            );
            this.playedSound = true;
        }

        // 渲染背景（带渐入动画）
        float alpha = animationProgress;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        guiGraphics.blit(TEXTURE, 0, 0, 0, 0, this.width(), this.height(), 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制图标区域
        int iconX = 6;
        int iconY = 6;
        int iconSize = 20;

        // 绘制图标背景（金色边框）
        guiGraphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, 0x80000000 | 0xFFD700);

        // 绘制配方物品图标（使用正常渲染）
        ItemStack result = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
        if (!result.isEmpty()) {
            drawItem(guiGraphics, result, iconX + 2, iconY + 2);
        }

        // 绘制边框（金色）
        guiGraphics.renderOutline(iconX, iconY, iconSize, iconSize, 0xFFD700);

        // 绘制发光效果
        if (animationProgress < 1.0f || (System.currentTimeMillis() - lastChanged) % 1000 < 500) {
            int glowAlpha = (int)((1.0f - Math.abs((System.currentTimeMillis() % 1000) / 500.0f - 1.0f)) * 100);
            guiGraphics.fill(iconX - 1, iconY - 1, iconX + iconSize + 1, iconY + iconSize + 1,
                    (glowAlpha << 24) | 0xFFD700);
        }

        // 绘制标题和描述
        Minecraft minecraft = Minecraft.getInstance();

        // 标题（带渐入动画）
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        guiGraphics.drawString(minecraft.font, title, 30, 7, 0xFFFFFF, false);

        // 描述文本（带渐入动画）
        String descriptionText = description.getString();
        int maxWidth = 120;
        int yPos = 18;

        // 自动换行处理
        if (minecraft.font.width(descriptionText) > maxWidth) {
            // 简单换行处理，如果需要更复杂可以单独提取方法
            String[] words = descriptionText.split(" ");
            StringBuilder currentLine = new StringBuilder();
            List<String> lines = new ArrayList<>();

            for (String word : words) {
                String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
                if (minecraft.font.width(testLine) <= maxWidth) {
                    currentLine.append(currentLine.isEmpty() ? word : " " + word);
                } else {
                    if (!currentLine.isEmpty()) {
                        lines.add(currentLine.toString());
                    }
                    currentLine = new StringBuilder(word);
                }
            }

            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }

            for (int i = 0; i < Math.min(lines.size(), 2); i++) {
                guiGraphics.drawString(minecraft.font, lines.get(i), 30, yPos + i * 10, 0xCCCCCC, false);
            }
        } else {
            guiGraphics.drawString(minecraft.font, description, 30, yPos, 0xCCCCCC, false);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制进度条
        float progress = (float)timeSinceLastVisible / 5000.0F; // 5秒显示时间
        if (progress >= 1.0F) {
            return Visibility.HIDE;
        }

        int progressWidth = (int)(160 * Mth.clamp(progress, 0.0F, 1.0F));
        int progressColor = 0xFF00FF00; // 绿色进度条
        guiGraphics.fill(4, this.height() - 4, 4 + progressWidth, this.height() - 2, progressColor);

        return Visibility.SHOW;
    }

    /**
     * 绘制物品的正常材质
     */
    private void drawItem(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
        if (itemStack.isEmpty()) return;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // 使用 GuiGraphics 的 renderItem 方法绘制物品
        guiGraphics.renderItem(itemStack, x, y);

        // 如果需要，可以添加物品计数
        guiGraphics.renderItemDecorations(Minecraft.getInstance().font, itemStack, x, y);

        poseStack.popPose();
    }

    @Override
    public int width() {
        return 160;
    }

    @Override
    public int height() {
        return 32;
    }
}