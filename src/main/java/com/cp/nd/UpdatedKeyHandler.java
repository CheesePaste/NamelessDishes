// UpdatedKeyHandler.java
package com.cp.nd;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class UpdatedKeyHandler {

    private static boolean recipesAdded = false;
    private static final List<ResourceLocation> addedRecipeIds = new ArrayList<>();

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        if (event.getKey() == GLFW.GLFW_KEY_T && event.getAction() == GLFW.GLFW_PRESS) {
            if (!recipesAdded) {
                addRecipesWithNotification();

                recipesAdded = true;
            }
        }

        // 调试按键：强制刷新所有GUI
        if (event.getKey() == GLFW.GLFW_KEY_R && event.getAction() == GLFW.GLFW_PRESS &&
                event.getModifiers() == GLFW.GLFW_MOD_CONTROL) {
            forceRefreshAll();
        }
    }

    private static void addRecipesWithNotification() {
        Minecraft mc = Minecraft.getInstance();

        try {
            // 清空之前的ID列表
            addedRecipeIds.clear();

            // 添加几个确保可见的配方
            System.out.println("开始添加可见配方...");

            // 2. 简单的无序配方
            ShapelessRecipe recipe2 = BetterRecipeBuilder.createVisibleShapelessRecipe();
            addedRecipeIds.add(recipe2.getId());
            RecipeRefreshUtil.addRecipeWithRefresh(recipe2);


            // 通知玩家
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§a已添加4个测试配方！"));
            }
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§e请尝试："));
            }
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§7- 4个木板 -> 4个木棍 (2x2合成)"));
            }
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§7- 1个圆石 -> 1个石头 (无序)"));
            }
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§7- 9个金锭 -> 1个钻石 (3x3工作台)"));
            }
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§7- 9个铁锭 -> 1个金锭 (3x3工作台)"));
            }

        } catch (Exception e) {
            System.err.println("添加配方失败: " + e.getMessage());
            e.printStackTrace();
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§c添加配方失败，查看控制台日志"));
            }
        }
    }


    private static void forceRefreshAll() {
        System.out.println("强制刷新所有GUI和缓存...");
        Minecraft mc = Minecraft.getInstance();

        // 重新加载当前GUI
        if (mc.screen != null) {
            System.out.println("重新加载当前屏幕: " + mc.screen.getClass().getSimpleName());
            mc.setScreen(mc.screen);
        }

        // 刷新配方显示
        RecipeRefreshUtil.refreshClientRecipeDisplay();

        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§e已强制刷新GUI和配方缓存"));
        }
    }



}