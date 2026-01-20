// RecipeUtil.java
package com.cp.nd.util;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import com.cp.nd.item.IRecipeManagerMixin;

import java.util.Optional;

public class RecipeUtil {

    /**
     * 动态添加配方
     * @param recipe 要添加的配方对象
     * @return 是否添加成功
     */
    public static boolean addRecipe(Recipe<?> recipe) {
        if (recipe == null) {
            return false;
        }

        ResourceLocation recipeId = recipe.getId();

        try {
            // 获取当前的RecipeManager
            RecipeManager recipeManager = getRecipeManager();
            if (recipeManager == null) {
                return false;
            }

            // 检查是否已存在相同ID的配方
            if (recipeManager.byKey(recipeId).isPresent()) {
                // 可选：可以在这里选择覆盖或跳过
                // return false; // 如果已存在则跳过
            }

            // 使用接口调用添加配方
            if (recipeManager instanceof IRecipeManagerMixin) {
                ((IRecipeManagerMixin) recipeManager).addRecipe(recipe);
                return true;
            }

            return false;
        } catch (Exception e) {
            // 记录错误
            return false;
        }
    }

    /**
     * 动态移除配方
     * @param recipeId 配方ID
     * @return 是否移除成功
     */
    public static boolean removeRecipe(ResourceLocation recipeId) {
        try {
            RecipeManager recipeManager = getRecipeManager();
            if (recipeManager == null) {
                return false;
            }

            if (recipeManager instanceof IRecipeManagerMixin) {
                return ((IRecipeManagerMixin) recipeManager).removeRecipe(recipeId);
            }

            return false;
        } catch (Exception e) {
            // YourMod.LOGGER.error("Failed to remove recipe: " + recipeId, e);
            return false;
        }
    }

    /**
     * 检查配方是否存在
     * @param recipeId 配方ID
     * @return 是否存在
     */
    public static boolean hasRecipe(ResourceLocation recipeId) {
        try {
            RecipeManager recipeManager = getRecipeManager();
            return recipeManager != null && recipeManager.byKey(recipeId).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取RecipeManager
     * 这个方法需要根据你的调用环境（客户端/服务端）进行调整
     */
    private static RecipeManager getRecipeManager() {
        // 优先使用客户端世界的RecipeManager
        /*if (Minecraft.getInstance().level != null) {
            return Minecraft.getInstance().level.getRecipeManager();
        }*/

        // 如果客户端没有世界，尝试获取服务器世界的RecipeManager
        return getServerRecipeManager().orElse(null);
    }

    /**
     * 服务器端获取RecipeManager
     */
    public static Optional<RecipeManager> getServerRecipeManager() {
        MinecraftServer server = getMinecraftServer();
        if (server != null) {
            // 尝试从所有世界获取RecipeManager
            for (Level level : server.getAllLevels()) {
                level.getRecipeManager();
                return Optional.of(level.getRecipeManager());
            }
        }
        return Optional.empty();
    }

    private static MinecraftServer getMinecraftServer() {
        // 使用Forge API获取当前服务器
        return net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    /**
     * 安全的添加方法，如果存在则替换
     */
    public static boolean addOrReplaceRecipe(Recipe<?> recipe) {
        ResourceLocation recipeId = recipe.getId();
        if (recipeId == null) {
            return false;
        }

        // 如果存在则先移除
        if (hasRecipe(recipeId)) {
            removeRecipe(recipeId);
        }

        // 添加新配方
        return addRecipe(recipe);
    }

    /**
     * 批量添加配方
     */
    public static void addRecipes(Iterable<Recipe<?>> recipes) {
        for (Recipe<?> recipe : recipes) {
            addRecipe(recipe);
        }
    }
}