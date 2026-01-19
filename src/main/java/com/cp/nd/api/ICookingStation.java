package com.cp.nd.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.List;

public interface ICookingStation {
    /**
     * 获取烹饪站中的输入物品
     */
    List<ItemStack> getInputItems(BlockEntity blockEntity);

    /**
     * 设置烹饪站的输出
     */
    void setOutput(BlockEntity blockEntity, ItemStack result);

    /**
     * 获取烹饪站的输出槽位
     */
    @Nullable
    ItemStack getOutput(BlockEntity blockEntity);

    /**
     * 检查烹饪站是否正在工作
     */
    boolean isCooking(BlockEntity blockEntity);

    /**
     * 重置烹饪站状态
     */
    void resetCooking(BlockEntity blockEntity);

    /**
     * 获取烹饪站对应的配方
     */
    @Nullable
    Recipe<?> getCurrentRecipe(BlockEntity blockEntity);

    /**
     * 获取烹饪站方块实体类型
     */
    @Nullable
    Class<? extends BlockEntity> getBlockEntityClass();
}