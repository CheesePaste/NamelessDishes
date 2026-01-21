package com.cp.nd.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * 无名料理配方注册器接口
 * 详见CookingPotRecipeRegister和RecipeRegisterManager
 * 如果是外部模组兼容，需要每次游戏启动使用RecipeRegisterManager.registerRecipeRegisterDynamic()传入，来发挥作用
 * 内部直接硬编码入registerRecipeRegister
 */
public interface INamelessDishRecipeRegister {
    Set<Recipe<?>> unRegisteredRecipes=new HashSet<>();
    /**
     * 是否支持该料理方块ID
     * @param cookingBlockId 料理方块ID
     * @return 是否支持
     */
    boolean isSupport(String cookingBlockId);
    /**
     * 获取注册器名称（用于日志）
     * @return 注册器名称
     */
    String getName();

    Set<String> getSupportedBlocks();

    /**
     * 从无名料理创建配方对象
     * @param namelessDish 无名料理物品堆叠
     * @param recipeId 配方资源位置（可选）
     * @return 创建的配方对象，如果创建失败返回null
     *
     */
    @Nullable
    Recipe<?> createRecipeFromNamelessDish(ItemStack namelessDish, @Nullable ResourceLocation recipeId);


    /**
     * 将未注册配方注册进入游戏。将会在玩家进入后调用
     */
    void registerToGame();

    default void addToSet(Recipe<?> recipe)
    {
        unRegisteredRecipes.add(recipe);
    }

}