// DebugKeyHandler.java
package com.cp.nd;

import com.cp.nd.DebugRecipeUtil;
import com.cp.nd.NamelessDishes;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DebugKeyHandler {

    private static boolean testRecipeAdded = false;
    private static final Random random = new Random();

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        // 使用更可靠的按键检测
        if (event.getKey() == GLFW.GLFW_KEY_T && event.getAction() == GLFW.GLFW_PRESS) {
            System.out.println("=== T键被按下 ===");

            if (!testRecipeAdded) {
                System.out.println("开始添加测试配方...");
                addTestRecipes();
                testRecipeAdded = true;

                // 给玩家发送消息
                minecraft.player.sendSystemMessage(
                        Component.literal("§a[调试] 已添加测试配方！打开合成台查看。")
                );
            } else {
                System.out.println("开始移除测试配方...");
                removeTestRecipes();
                testRecipeAdded = false;

                minecraft.player.sendSystemMessage(
                        Component.literal("§c[调试] 已移除测试配方！")
                );
            }

            // 打印所有配方列表
            DebugRecipeUtil.printAllRecipes();
        }

        // 添加额外的调试按键
        if (event.getKey() == GLFW.GLFW_KEY_Y && event.getAction() == GLFW.GLFW_PRESS) {
            System.out.println("=== Y键被按下 - 打印当前配方列表 ===");
            DebugRecipeUtil.printAllRecipes();
        }

        if (event.getKey() == GLFW.GLFW_KEY_U && event.getAction() == GLFW.GLFW_PRESS) {
            System.out.println("=== U键被按下 - 添加一个随机测试配方 ===");
            addRandomTestRecipe();
            DebugRecipeUtil.printAllRecipes();
        }
    }

    private static void addTestRecipes() {
        try {
            // 3. 无序配方
            ResourceLocation recipeId3 = new ResourceLocation(NamelessDishes.MOD_ID, "debug_shapeless_test");
            ShapelessRecipe recipe3 = createShapelessRecipe(recipeId3,
                    NonNullList.of(Ingredient.EMPTY,
                            Ingredient.of(Items.COBBLESTONE),
                            Ingredient.of(Items.COAL)),
                    new ItemStack(Items.COAL_BLOCK));

            System.out.println("添加配方3: " + recipeId3);
            boolean result3 = DebugRecipeUtil.addRecipeDebug(recipe3);
            System.out.println("结果: " + (result3 ? "成功" : "失败"));

        } catch (Exception e) {
            System.err.println("添加测试配方时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void removeTestRecipes() {
        System.out.println("移除测试配方...");
        // 我们暂时不实现移除，因为我们需要知道确切发生了什么
    }

    private static void addRandomTestRecipe() {
        ResourceLocation id = new ResourceLocation(NamelessDishes.MOD_ID, "random_test_" + random.nextInt(1000));

        // 创建一个非常简单的配方：1个圆石 -> 1个石头
        ShapelessRecipe recipe = createShapelessRecipe(id,
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.COBBLESTONE)),
                new ItemStack(Items.STONE));

        System.out.println("添加随机测试配方: " + id);
        DebugRecipeUtil.addRecipeDebug(recipe);
    }



    private static ShapelessRecipe createShapelessRecipe(ResourceLocation id,
                                                         NonNullList<Ingredient> ingredients,
                                                         ItemStack result) {
        return new ShapelessRecipe(
                id,
                id.toString(),
                CraftingBookCategory.MISC,
                result,
                ingredients
        );
    }
}