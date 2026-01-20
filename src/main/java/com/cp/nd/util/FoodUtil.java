package com.cp.nd.util;

import com.cp.nd.config.NDConfig;
import com.cp.nd.item.AbstractNamelessDishItem;
import com.cp.nd.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class FoodUtil {
    /// 生成无名料理
    @Nonnull
    public static ItemStack createNamelessResult(BlockEntity blockEntity, List<ItemStack> inputs, boolean withbowl) {

        // 计算饱食度和饱和度
        float totalHunger = 0;
        float totalSaturation = 0;
        List<ItemStack> ingredientTypes = new ArrayList<>();

        for (ItemStack input : inputs) {
            if (!input.isEmpty()) {
                if (input.isEdible()) {
                    FoodProperties foodProperties = input.getFoodProperties(null);
                    if (foodProperties != null) {
                        totalHunger += foodProperties.getNutrition()*input.getCount();
                        totalSaturation += foodProperties.getSaturationModifier()*input.getCount();
                    }
                }
                ingredientTypes.add(input);
            }
        }

        // 应用配置乘数
        double baseHunger = totalHunger * (NDConfig.INSTANCE.baseHungerMultiplier.get() / 100.0f);
        double baseSaturation = (totalSaturation * NDConfig.INSTANCE.baseSaturationMultiplier.get());

        // 获取料理方块注册名
        String cookingBlockId = null;
        if (blockEntity != null) {
            // 获取BlockEntityType的注册名
            ResourceLocation blockEntityTypeId = BlockEntityType.getKey(blockEntity.getType());
            if (blockEntityTypeId != null) {
                cookingBlockId = blockEntityTypeId.toString(); // 例如: "farmersdelight:cooking_pot"
            }
        }

        if(withbowl) {
            return AbstractNamelessDishItem.createDish(
                    ModItems.NAMELESS_DISH_WITH_BOWL.get(),
                    (int) baseHunger, (float) baseSaturation, ingredientTypes, true, cookingBlockId);
        }
        else
        {
            return AbstractNamelessDishItem.createDish(
                    ModItems.NAMELESS_DISH.get(),
                    (int) baseHunger, (float) baseSaturation, ingredientTypes, false, cookingBlockId);
        }
    }

    /// 生成无名料理
    @Nonnull
    public static ItemStack createNamelessResult(String cookingBlockId, List<ItemStack> inputs, boolean withbowl) {

        // 计算饱食度和饱和度
        float totalHunger = 0;
        float totalSaturation = 0;
        List<ItemStack> ingredientTypes = new ArrayList<>();

        for (ItemStack input : inputs) {
            if (!input.isEmpty()) {
                if (input.isEdible()) {
                    FoodProperties foodProperties = input.getFoodProperties(null);
                    if (foodProperties != null) {
                        totalHunger += foodProperties.getNutrition()*input.getCount();
                        totalSaturation += foodProperties.getSaturationModifier()*input.getCount();
                    }
                }
                ingredientTypes.add(input);
            }
        }

        // 应用配置乘数
        double baseHunger = totalHunger * (NDConfig.INSTANCE.baseHungerMultiplier.get() / 100.0f);
        double baseSaturation = (totalSaturation * NDConfig.INSTANCE.baseSaturationMultiplier.get());


        if(withbowl) {
            return AbstractNamelessDishItem.createDish(
                    ModItems.NAMELESS_DISH_WITH_BOWL.get(),
                    (int) baseHunger, (float) baseSaturation, ingredientTypes, true, cookingBlockId);
        }
        else
        {
            return AbstractNamelessDishItem.createDish(
                    ModItems.NAMELESS_DISH.get(),
                    (int) baseHunger, (float) baseSaturation, ingredientTypes, false, cookingBlockId);
        }
    }



    /// 检测配置是否允许无名料理合成
    public static boolean allowNamelessCrafting(Level level, BlockEntity blockEntity, List<ItemStack> inputs,String modname,String cookingBlockName) {
        if (!(blockEntity instanceof CookingPotBlockEntity cookingPot)) {
            return false;
        }

        // 检查农夫乐事模组是否启用
        if (!ModDetectionUtil.isModLoaded(modname)) {
            return false;
        }

        // 检查配置是否启用
        if (!NDConfig.INSTANCE.enabledMods.get().contains(modname)) {
            return false;
        }
        if(!NDConfig.INSTANCE.cookingBlocks.get().contains(cookingBlockName))
        {
            return false;
        }

        // 检查输入物品数量是否在允许范围内
        int ingredientCount = getIngredientCount(inputs);
        if (ingredientCount < NDConfig.INSTANCE.minIngredients.get() ||
                ingredientCount > NDConfig.INSTANCE.maxIngredients.get()) {
            return false;
        }

        if (!cookingPot.isHeated(level, cookingPot.getBlockPos())) {
            return false;
        }



        return true;
    }


    /// 辅助方法

    private static int getIngredientCount(List<ItemStack> inputs) {
        int count = 0;
        for (ItemStack stack : inputs) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
