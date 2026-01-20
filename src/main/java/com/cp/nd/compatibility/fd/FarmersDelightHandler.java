package com.cp.nd.compatibility.fd;

import com.cp.nd.NamelessDishes;
import com.cp.nd.config.NDConfig;
import com.cp.nd.item.AbstractNamelessDishItem;
import com.cp.nd.item.ModItems;
import com.cp.nd.util.ModDetectionUtil;
import net.minecraft.core.BlockPos;
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

public class FarmersDelightHandler {

    private static final int MEAL_DISPLAY_SLOT = 6;
    private static final int CONTAINER_SLOT = 7;
    private static final int OUTPUT_SLOT = 8;
    private static final int INPUT_SLOT_START = 0;
    private static final int INPUT_SLOT_END = 5;

    /// 检测配置是否允许无名料理合成
    public boolean allowNamelessCrafting(Level level, BlockEntity blockEntity, List<ItemStack> inputs) {
        if (!(blockEntity instanceof CookingPotBlockEntity cookingPot)) {
            return false;
        }

        // 检查农夫乐事模组是否启用
        if (!ModDetectionUtil.isModLoaded("farmersdelight")) {
            return false;
        }

        // 检查配置是否启用
        if (!NDConfig.INSTANCE.enabledMods.get().contains("farmersdelight")) {
            return false;
        }
        if(!NDConfig.INSTANCE.cookingBlocks.get().contains("farmersdelight:cooking_pot"))
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

        // 检查是否有有效的输入
        boolean hasInput = false;
        for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
            if (!cookingPot.getInventory().getStackInSlot(i).isEmpty()) {
                hasInput = true;
                break;
            }
        }


        return hasInput;
    }

    /// 创建无名料理物品
    private ItemStack createNamelessItemStack(double hunger, double saturation,
                                              List<ItemStack> ingredients,
                                              String cookingBlockId) { // 新增参数
        ItemStack result = AbstractNamelessDishItem.createDish(
                ModItems.NAMELESS_DISH_WITH_BOWL.get(),
                (int) hunger, (float) saturation, ingredients, true, cookingBlockId); // 传入料理方块ID

        result.getOrCreateTag().putBoolean("IsNamelessDish", true);

        return result;
    }

    /// 生成无名料理
    @Nonnull
    public ItemStack createNamelessResult(Level level, BlockEntity blockEntity, List<ItemStack> inputs) {

        // 计算饱食度和饱和度
        float totalHunger = 0;
        float totalSaturation = 0;
        List<ItemStack> ingredientTypes = new ArrayList<>();

        for (ItemStack input : inputs) {
            if (!input.isEmpty()) {
                if (input.isEdible()) {
                    FoodProperties foodProperties = input.getFoodProperties(null);
                    if (foodProperties != null) {
                        totalHunger += foodProperties.getNutrition();
                        totalSaturation += foodProperties.getSaturationModifier();
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

        return createNamelessItemStack(baseHunger, baseSaturation, ingredientTypes, cookingBlockId);
    }

    /// 进行烹饪操作。这个类负责生成无名料理，放入显示槽（提示“需要碗”的槽），显示槽消耗碗进入输出槽的逻辑在对应mixin
    public boolean executeCooking(Level level, BlockEntity blockEntity, List<ItemStack> inputs, ItemStack result) {
        if (!(blockEntity instanceof CookingPotBlockEntity cookingPot) || result.isEmpty()) {
            return false;
        }

        try {

            ItemStack displaySlot = cookingPot.getInventory().getStackInSlot(MEAL_DISPLAY_SLOT);

            // 生成最终的无名料理（设置为1个）
            ItemStack finalResult = result.copy();
            finalResult.setCount(1);

            // 检查是否可以放置到目标槽位
            boolean canPlace = false;
            int targetSlot = -1;
            ItemStack targetStack = null;
                if (displaySlot.isEmpty()) {
                    canPlace = true;
                    targetSlot = MEAL_DISPLAY_SLOT;
                } else if (ItemStack.isSameItemSameTags(displaySlot, finalResult)) {
                    // 相同物品，检查是否可以堆叠
                    if (displaySlot.getCount() < displaySlot.getMaxStackSize()) {
                        canPlace = true;
                        targetSlot = MEAL_DISPLAY_SLOT;
                        targetStack = displaySlot;
                    }
                }



            if (!canPlace) {
                // 没有合适的槽位放置，烹饪失败
                return false;
            }

            // 消耗输入物品（每个输入槽只消耗1个）
            for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
                ItemStack stack = cookingPot.getInventory().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    stack.shrink(1);

                    // 处理剩余物品（如桶->空桶）
                    if (stack.hasCraftingRemainingItem()) {
                        ItemStack remaining = stack.getCraftingRemainingItem().copy();
                        handleRemainingItem(level, cookingPot, remaining);
                    }

                    // 如果槽位变空，直接设置空堆叠
                    if (stack.isEmpty()) {
                        cookingPot.getInventory().setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
            }

            // 放置料理到目标槽位
            if (targetStack == null) {
                // 目标槽位为空，直接放置
                cookingPot.getInventory().setStackInSlot(targetSlot, finalResult);
            } else {
                // 目标槽位有相同物品，堆叠
                targetStack.grow(1);
            }


            // 标记为已改变以更新客户端
            cookingPot.setChanged();

            // 触发容器更新
            if (cookingPot.getLevel() != null) {
                cookingPot.getLevel().sendBlockUpdated(
                        cookingPot.getBlockPos(),
                        cookingPot.getBlockState(),
                        cookingPot.getBlockState(),
                        3
                );
            }

            return true;
        } catch (Exception e) {
            NamelessDishes.LOGGER.error("Error executing cooking in FarmersDelight cooking pot", e);
            return false;
        }
    }

    /// 辅助方法：处理剩余物品
    private void handleRemainingItem(Level level, CookingPotBlockEntity cookingPot, ItemStack remainder) {
        if (level == null || remainder.isEmpty()) {
            return;
        }

        // 首先尝试放入输入槽（如果有空位）
        for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
            ItemStack slotStack = cookingPot.getInventory().getStackInSlot(i);
            if (slotStack.isEmpty()) {
                cookingPot.getInventory().setStackInSlot(i, remainder.copy());
                cookingPot.setChanged();
                return;
            }
        }

        // 否则弹出
        ejectRemainingItem(level, cookingPot, remainder);
    }
    /// 弹出物品，因为懒得翻了，所以没有用农夫乐事原版弹出逻辑而是弹出到上面
    private void ejectRemainingItem(Level level, CookingPotBlockEntity cookingPot, ItemStack remainder) {
        if (level == null || remainder.isEmpty() || level.isClientSide()) {
            return;
        }

        BlockPos pos = cookingPot.getBlockPos();
        net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                level,
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                remainder.copy()
        );
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }


    /// 辅助方法

    private int getIngredientCount(List<ItemStack> inputs) {
        int count = 0;
        for (ItemStack stack : inputs) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }




}