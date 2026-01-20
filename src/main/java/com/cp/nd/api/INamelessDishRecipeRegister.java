package com.cp.nd.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * 无名料理配方注册器接口
 * 详见CookingPotRecipeRegister和RecipeRegisterManager
 * 需要每次游戏启动使用RecipeRegisterManager.registerRecipeRegisterDynamic()传入，来发挥作用
 */
public interface INamelessDishRecipeRegister {
    /**
     * 注册无名料理配方
     * @param namelessDish 无名料理物品堆叠
     * @param recipeId 配方资源位置（可选）
     * @return 注册是否成功
     */
    boolean register(ItemStack namelessDish, @Nullable ResourceLocation recipeId);

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
}