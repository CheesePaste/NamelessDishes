package com.cp.nd.compatibility.base;

import com.cp.nd.api.ICookingRecipeHandler;
import com.cp.nd.api.ICookingStation;
import com.cp.nd.config.NDConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.List;

public class BaseCookingHandler implements ICookingRecipeHandler {

    @Override
    public String getModId() {
        return "base";
    }

    @Override
    public boolean isSupportedBlock(BlockEntity blockEntity) {
        // 基础处理器不处理特定方块，只作为后备
        return false;
    }

    @Override
    public boolean isCookingContainer(ItemStack stack) {
        // 基础处理器不判断容器
        return false;
    }

    @Nullable
    @Override
    public Recipe<?> getExistingRecipe(Level level, BlockEntity blockEntity, List<ItemStack> inputs) {
        // 基础处理器不检查已有配方
        return null;
    }

    @Override
    public boolean allowNamelessCrafting(Level level, BlockEntity blockEntity, List<ItemStack> inputs) {
        // 基础处理器不允许无名料理（需要特定处理器）
        return false;
    }

    @Override
    public ItemStack createNamelessResult(Level level, BlockEntity blockEntity, List<ItemStack> inputs) {
        // 基础处理器不创建结果
        return ItemStack.EMPTY;
    }

    @Nullable
    @Override
    public ICookingStation getCookingStation(BlockEntity blockEntity) {
        return ICookingRecipeHandler.super.getCookingStation(blockEntity);
    }

    // 辅助方法 - 可由子类继承使用
    protected int calculateHunger(List<ItemStack> inputs) {
        if (inputs.isEmpty()) return 0;

        int total = 0;
        int validIngredients = 0;

        for (ItemStack stack : inputs) {
            if (!stack.isEmpty()) {
                // 这里应该根据食物的营养价值计算
                // 暂时使用基础值
                total += 2;
                validIngredients++;
            }
        }

        if (validIngredients == 0) return 0;

        // 平均计算并应用乘数
        int avgHunger = total / validIngredients;
        return Math.max(1, avgHunger * NDConfig.INSTANCE.baseHungerMultiplier.get() / 100);
    }

    protected float calculateSaturation(List<ItemStack> inputs) {
        if (inputs.isEmpty()) return 0;

        float total = 0;
        int validIngredients = 0;

        for (ItemStack stack : inputs) {
            if (!stack.isEmpty()) {
                // 这里应该根据食物的饱和度计算
                // 暂时使用基础值
                total += 0.6f;
                validIngredients++;
            }
        }

        if (validIngredients == 0) return 0;

        // 平均计算并应用乘数
        float avgSaturation = total / validIngredients;
        return avgSaturation * NDConfig.INSTANCE.baseSaturationMultiplier.get().floatValue();
    }

    protected boolean validateIngredients(List<ItemStack> inputs) {
        int validCount = (int) inputs.stream()
                .filter(stack -> !stack.isEmpty())
                .count();

        return validCount >= NDConfig.INSTANCE.minIngredients.get() &&
                validCount <= NDConfig.INSTANCE.maxIngredients.get();
    }
}