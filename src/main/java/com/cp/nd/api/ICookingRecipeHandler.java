package com.cp.nd.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.List;

public interface ICookingRecipeHandler {
    /**
     * 获取处理器对应的模组ID
     */
    String getModId();

    /**
     * 检查方块是否被此处理器支持
     */
    boolean isSupportedBlock(BlockEntity blockEntity);

    /**
     * 检查物品是否为烹饪容器（如锅、碗）
     */
    boolean isCookingContainer(ItemStack stack);

    /**
     * 获取当前烹饪站中的配方（如果有）
     */
    @Nullable
    Recipe<?> getExistingRecipe(Level level, BlockEntity blockEntity, List<ItemStack> inputs);

    /**
     * 检查是否允许创建无名料理
     */
    boolean allowNamelessCrafting(Level level, BlockEntity blockEntity, List<ItemStack> inputs);

    /**
     * 创建无名料理结果
     */
    ItemStack createNamelessResult(Level level, BlockEntity blockEntity, List<ItemStack> inputs);

    /**
     * 执行烹饪（如果有特殊逻辑）
     */
    default boolean executeCooking(Level level, BlockEntity blockEntity, List<ItemStack> inputs, ItemStack result) {
        return false;
    }

    /**
     * 获取烹饪站接口
     */
    @Nullable
    default ICookingStation getCookingStation(BlockEntity blockEntity) {
        return null;
    }
}