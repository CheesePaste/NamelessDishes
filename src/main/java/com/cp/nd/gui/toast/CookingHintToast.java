
package com.cp.nd.gui.toast;

import com.cp.nd.NamelessDishes;
import com.cp.nd.util.RecipeMatcher.HintType;
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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class CookingHintToast implements Toast {
    private static final ResourceLocation DEFAULT_TEXTURE =
            new ResourceLocation(NamelessDishes.MOD_ID, "textures/gui/toast3.png");

    private final Component title;
    private final Component hint;
    private final ItemStack targetItem;
    private final HintType hintType;
    private long lastChanged;
    private boolean playedSound;
    private float animationProgress = 0.0f;

    public CookingHintToast(String hintText, HintType type, ItemStack targetItem) {
        this.hintType = type;
        this.title = Component.translatable("toast.nameless_dishes.cooking_hint")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        this.hint = Component.literal(hintText).withStyle(ChatFormatting.WHITE);
        this.targetItem = targetItem != null ? targetItem : ItemStack.EMPTY;
        this.lastChanged = System.currentTimeMillis();
        this.playedSound = false;
        this.animationProgress = 0.0f;
    }

    public CookingHintToast(String hintText, HintType type) {
        this(hintText, type, ItemStack.EMPTY);
    }

    @Override
    public @NotNull Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long timeSinceLastVisible) {
        // 更新动画进度
        animationProgress = Mth.clamp(animationProgress + 0.05f, 0.0f, 1.0f);

        // 播放音效
        if (!this.playedSound) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(hintType.getSound(), 0.7F, 0.8F + (float)Math.random() * 0.1F)
            );
            this.playedSound = true;
        }

        // 渲染背景（使用DEFAULT_TEXTURE，带渐入动画）
        float alpha = animationProgress;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        guiGraphics.blit(DEFAULT_TEXTURE, 0, 0, 0, 0, this.width(), this.height(), 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制图标区域
        int iconX = 6;
        int iconY = 6;
        int iconSize = 20;

        // 绘制图标背景
        guiGraphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize,
                0x80000000 | (hintType.getColor() & 0xFFFFFF));

        // 绘制目标物品的正常材质（不再使用黑色剪影）
        if (!targetItem.isEmpty()) {
            drawItem(guiGraphics, targetItem, iconX + 2, iconY + 2);
        }

        // 绘制边框
        guiGraphics.renderOutline(iconX, iconY, iconSize, iconSize, hintType.getColor());

        // 绘制发光效果
        if (animationProgress < 1.0f || (System.currentTimeMillis() - lastChanged) % 1000 < 200) {
            int glowColor = hintType.getColor();
            int glowAlpha = (int)((1.0f - Math.abs((System.currentTimeMillis() % 1000) / 500.0f - 1.0f)) * 100);
            guiGraphics.fill(iconX - 1, iconY - 1, iconX + iconSize + 1, iconY + iconSize + 1,
                    (glowAlpha << 24) | (glowColor & 0xFFFFFF));
        }

        // 绘制标题和提示
        Minecraft minecraft = Minecraft.getInstance();

        // 标题（带渐入动画）
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        guiGraphics.drawString(minecraft.font, title, 30, 7, 0xFFFFFF, false);

        // 提示文本（带渐入动画）
        String hintText = hint.getString();
        int maxWidth = 120;
        int yPos = 18;

        // 自动换行处理
        if (minecraft.font.width(hintText) > maxWidth) {
            List<String> lines = wrapText(hintText, maxWidth, minecraft.font);
            for (int i = 0; i < Math.min(lines.size(), 2); i++) {
                guiGraphics.drawString(minecraft.font, lines.get(i), 30, yPos + i * 10, 0xCCCCCC, false);
            }
        } else {
            guiGraphics.drawString(minecraft.font, hint, 30, yPos, 0xCCCCCC, false);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制进度条
        float progress = (float)timeSinceLastVisible / 3000.0F; // 3秒显示时间
        if (progress >= 1.0F) {
            return Visibility.HIDE;
        }

        int progressWidth = (int)(160 * Mth.clamp(progress, 0.0F, 1.0F));
        int progressColor = (0xFF << 24) | (hintType.getColor() & 0xFFFFFF);
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

    /**
     * 文本自动换行
     */
    private List<String> wrapText(String text, int maxWidth, net.minecraft.client.gui.Font font) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (font.width(testLine) <= maxWidth) {
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

        return lines;
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