// DebugRecipeUtil.java
package com.cp.nd;

import com.cp.nd.item.IRecipeManagerMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Map;

public class DebugRecipeUtil {

    public static boolean addRecipeDebug(Recipe<?> recipe) {
        System.out.println("=== 开始添加配方调试 ===");
        System.out.println("配方ID: " + recipe.getId());
        System.out.println("配方类型: " + recipe.getType());

        try {
            // 1. 获取RecipeManager
            RecipeManager recipeManager = getRecipeManager();
            System.out.println("获取的RecipeManager: " + (recipeManager != null ? "成功" : "失败"));

            if (recipeManager == null) {
                System.out.println("错误: RecipeManager为空");
                return false;
            }

            // 2. 检查配方是否已存在
            boolean exists = recipeManager.byKey(recipe.getId()).isPresent();
            System.out.println("配方是否已存在: " + exists);

            // 3. 检查当前所有配方数量
            int recipeCount = recipeManager.getRecipes().size();
            System.out.println("当前配方总数: " + recipeCount);

            // 4. 检查是否可以转换为IRecipeManagerMixin
            boolean isMixin = recipeManager instanceof IRecipeManagerMixin;
            System.out.println("是否为IRecipeManagerMixin实例: " + isMixin);

            if (!isMixin) {
                System.out.println("警告: RecipeManager不是IRecipeManagerMixin实例");
                // 检查recipes字段是否是ImmutableMap
                try {
                    // 使用反射检查内部状态
                    java.lang.reflect.Field recipesField = recipeManager.getClass().getDeclaredField("recipes");
                    recipesField.setAccessible(true);
                    Map<?, ?> recipesMap = (Map<?, ?>) recipesField.get(recipeManager);
                    System.out.println("recipes字段类型: " + recipesMap.getClass().getName());
                    System.out.println("recipes大小: " + recipesMap.size());

                    java.lang.reflect.Field byNameField = recipeManager.getClass().getDeclaredField("byName");
                    byNameField.setAccessible(true);
                    Map<?, ?> byNameMap = (Map<?, ?>) byNameField.get(recipeManager);
                    System.out.println("byName字段类型: " + byNameMap.getClass().getName());
                    System.out.println("byName大小: " + byNameMap.size());

                } catch (Exception e) {
                    System.out.println("反射检查失败: " + e.getMessage());
                }
            }

            // 5. 尝试添加配方
            if (recipeManager instanceof IRecipeManagerMixin) {
                System.out.println("正在调用addRecipe方法...");
                ((IRecipeManagerMixin) recipeManager).addRecipe(recipe);

                // 6. 添加后再次检查
                boolean added = recipeManager.byKey(recipe.getId()).isPresent();
                System.out.println("添加后配方是否存在: " + added);

                // 7. 检查当前所有配方数量
                int newRecipeCount = recipeManager.getRecipes().size();
                System.out.println("添加后配方总数: " + newRecipeCount);

                if (added) {
                    System.out.println("✓ 配方添加成功");
                    return true;
                } else {
                    System.out.println("✗ 配方添加失败 - 添加后检查不存在");
                    return false;
                }
            } else {
                System.out.println("✗ 无法添加 - RecipeManager不是IRecipeManagerMixin实例");
                return false;
            }

        } catch (Exception e) {
            System.out.println("添加过程中出现异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            System.out.println("=== 结束添加配方调试 ===");
        }
    }

    private static RecipeManager getRecipeManager() {
        System.out.println("正在获取RecipeManager...");

        // 优先尝试客户端
        /*Minecraft client = Minecraft.getInstance();
        if (client != null && client.level != null) {
            System.out.println("从客户端世界获取RecipeManager");
            RecipeManager rm = client.level.getRecipeManager();
            System.out.println("客户端RecipeManager: " + rm);
            return rm;
        }

        System.out.println("客户端世界未就绪，尝试获取服务器...");*/

        // 尝试服务端
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            System.out.println("找到MinecraftServer");
            for (Level level : server.getAllLevels()) {
                if (level != null) {
                    System.out.println("检查世界: " + level.dimension().location());
                    RecipeManager rm = level.getRecipeManager();
                    System.out.println("从服务器世界获取RecipeManager: " + level.dimension().location());
                    return rm;
                }
            }
        }

        System.out.println("未能获取RecipeManager");
        return null;
    }

    public static void printAllRecipes() {
        try {
            RecipeManager recipeManager = getRecipeManager();
            if (recipeManager == null) {
                System.out.println("无法获取RecipeManager来打印配方");
                return;
            }

            System.out.println("=== 当前所有配方 ===");
            int count = 0;
            for (Recipe<?> recipe : recipeManager.getRecipes()) {
                System.out.println(++count + ". " + recipe.getId() + " - " + recipe.getType());
            }
            System.out.println("总计: " + count + " 个配方");
            System.out.println("=== 结束 ===");

        } catch (Exception e) {
            System.out.println("打印配方列表失败: " + e.getMessage());
        }
    }
}