package com.cp.nd.compatibility.fd;

import com.cp.nd.NamelessDishes;
import com.cp.nd.api.ICookingRecipeHandler;
import com.cp.nd.api.ICookingStation;
import com.cp.nd.config.NDConfig;
import com.cp.nd.item.AbstractNamelessDishItem;
import com.cp.nd.item.ModItems;
import com.cp.nd.mixin.fd.CookingPotBlockEntityAccessor;
import com.cp.nd.util.ModDetectionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.common.registry.ModRecipeTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FarmersDelightHandler implements ICookingRecipeHandler {

    private static final int MEAL_DISPLAY_SLOT = 6;
    private static final int CONTAINER_SLOT = 7;
    private static final int OUTPUT_SLOT = 8;
    private static final int INPUT_SLOT_START = 0;
    private static final int INPUT_SLOT_END = 5;

    // 反射字段缓存
    private static Field cookTimeField;
    private static Field cookTimeTotalField;
    private static boolean fieldsInitialized = false;

    @Override
    public String getModId() {
        return "farmersdelight";
    }

    @Override
    public boolean isSupportedBlock(BlockEntity blockEntity) {
        return blockEntity instanceof CookingPotBlockEntity;
    }

    @Override
    public boolean isCookingContainer(ItemStack stack) {
        // 农夫乐事中，碗、盘子等可以作为容器
        return stack.is(Items.BOWL) ||
                stack.is(Items.POTION) ||
                stack.is(Items.GLASS_BOTTLE) ||
                stack.is(Items.BUCKET);
    }

    /// 获得配方
    @Nullable
    @Override
    public Recipe<?> getExistingRecipe(Level level, BlockEntity blockEntity, List<ItemStack> inputs) {
        if (!(blockEntity instanceof CookingPotBlockEntity cookingPot) || level == null) {
            return null;
        }

        try {
            // 创建 RecipeWrapper 来包装烹饪锅的库存
            RecipeWrapper inventoryWrapper = new RecipeWrapper(cookingPot.getInventory());

            // 查找匹配的配方
            Optional<CookingPotRecipe> recipe = level.getRecipeManager()
                    .getRecipeFor(ModRecipeTypes.COOKING.get(), inventoryWrapper, level);

            return recipe.orElse(null);
        } catch (Exception e) {
            com.cp.nd.NamelessDishes.LOGGER.error("Error checking existing recipe in FarmersDelight cooking pot", e);
            return null;
        }
    }
    /// 检测配置是否允许无名料理合成

    @Override
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

        // 检查输入物品数量是否在允许范围内
        int ingredientCount = getIngredientCount(inputs);
        if (ingredientCount < NDConfig.INSTANCE.minIngredients.get() ||
                ingredientCount > NDConfig.INSTANCE.maxIngredients.get()) {
            return false;
        }

        // 检查烹饪锅是否被加热
        if (NDConfig.INSTANCE.requireCooking.get()) {
            if (!cookingPot.isHeated(level, cookingPot.getBlockPos())) {
                return false;
            }
        }

        // 检查是否有有效的输入
        boolean hasInput = false;
        for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
            if (!cookingPot.getInventory().getStackInSlot(i).isEmpty()) {
                hasInput = true;
                break;
            }
        }

        //判断原版配方政治1合成逻辑已挪入对应mixin
        /*try {
            Optional<CookingPotRecipe> existingRecipe = cookingPot.getLevel().getRecipeManager()
                    .getRecipeFor(ModRecipeTypes.COOKING.get(),
                            new RecipeWrapper(cookingPot.getInventory()),
                            cookingPot.getLevel());
            if (existingRecipe.isPresent()) {
                // 如果有原版配方正在处理，不允许创建无名料理
                // 除非配方已经完成（cookTime >= cookTimeTotal）
                int cookTime = getCookTime(cookingPot);
                int cookTimeTotal = 200;//
                if (cookTime < cookTimeTotal) {
                    return false; // 原版配方正在烹饪中
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }*/

        return hasInput;
    }

    /// 生成无名料理，这个类负责无名料理属性和原料等信息的搭建
    @Nonnull
    @Override
    public ItemStack createNamelessResult(Level level, BlockEntity blockEntity, List<ItemStack> inputs) {

        // 计算饱食度和饱和度
        float totalHunger = 0;
        float totalSaturation = 0;
        List<ItemStack> ingredientTypes = new ArrayList<>();

        for (ItemStack input : inputs) {
            if (!input.isEmpty()) {
                // 获取食物属性（如果有）
                if (input.isEdible()) {
                    FoodProperties foodProperties = input.getFoodProperties(null);
                    if (foodProperties != null) {
                        totalHunger += foodProperties.getNutrition();
                        totalSaturation += foodProperties.getSaturationModifier();
                    }
                }

                // 记录食材类型用于效果计算
                ingredientTypes.add(input);
            }
        }


        // 应用配置乘数
        double baseHunger = totalHunger * (NDConfig.INSTANCE.baseHungerMultiplier.get() / 100.0f);
        double baseSaturation = (totalSaturation * NDConfig.INSTANCE.baseSaturationMultiplier.get());


        return createNamelessItemStack(baseHunger, baseSaturation, ingredientTypes);
    }

    /// 创建无名料理物品
    private ItemStack createNamelessItemStack(double hunger, double saturation, List<ItemStack> ingredients) {
        ItemStack result = AbstractNamelessDishItem.createDish(
                ModItems.NAMELESS_DISH_WITH_BOWL.get(),
                (int) hunger, (float) saturation, ingredients, true);

        result.getOrCreateTag().putBoolean("IsNamelessDish", true);

        return result;
    }

    /// 进行烹饪操作。这个类负责生成无名料理，放入显示槽（提示“需要碗”的槽），显示槽消耗碗进入输出槽的逻辑在对应mixin
    @Override
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

    @Nullable
    @Override
    public ICookingStation getCookingStation(BlockEntity blockEntity) {
        if (!(blockEntity instanceof CookingPotBlockEntity cookingPot)) {
            return null;
        }

        return new FarmersDelightCookingStation(cookingPot);
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



    /// 农夫乐事烹饪站适配器，基本没用，以后这块可以删了
    private static class FarmersDelightCookingStation implements ICookingStation {
        private final CookingPotBlockEntity cookingPot;

        public FarmersDelightCookingStation(CookingPotBlockEntity cookingPot) {
            this.cookingPot = cookingPot;
        }

        @Override
        public List<ItemStack> getInputItems(BlockEntity blockEntity) {
            List<ItemStack> inputs = new ArrayList<>();

            for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
                ItemStack stack = cookingPot.getInventory().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    inputs.add(stack.copy());
                }
            }

            return inputs;
        }

        @Override
        public void setOutput(BlockEntity blockEntity, ItemStack result) {
            if (result.isEmpty()) return;

            // 优先尝试放到输出槽
            ItemStack currentOutput = cookingPot.getInventory().getStackInSlot(OUTPUT_SLOT);
            if (currentOutput.isEmpty()) {
                cookingPot.getInventory().setStackInSlot(OUTPUT_SLOT, result.copy());
            } else if (ItemStack.isSameItemSameTags(currentOutput, result)) {
                int canAdd = Math.min(result.getCount(), currentOutput.getMaxStackSize() - currentOutput.getCount());
                if (canAdd > 0) {
                    currentOutput.grow(canAdd);
                    result.shrink(canAdd);
                }

                // 如果还有剩余，放到显示槽
                if (!result.isEmpty()) {
                    cookingPot.getInventory().setStackInSlot(MEAL_DISPLAY_SLOT, result.copy());
                }
            } else {
                // 不同物品，放到显示槽
                cookingPot.getInventory().setStackInSlot(MEAL_DISPLAY_SLOT, result.copy());
            }

            cookingPot.setChanged();
        }

        @Nullable
        @Override
        public ItemStack getOutput(BlockEntity blockEntity) {
            ItemStack output = cookingPot.getInventory().getStackInSlot(OUTPUT_SLOT);
            if (!output.isEmpty()) {
                return output.copy();
            }

            // 如果没有输出，检查显示槽
            return cookingPot.getInventory().getStackInSlot(MEAL_DISPLAY_SLOT).copy();
        }

        @Override
        public boolean isCooking(BlockEntity blockEntity) {
            // 使用反射获取烹饪时间
            try {
                Field field = CookingPotBlockEntity.class.getDeclaredField("cookTime");
                field.setAccessible(true);
                return field.getInt(cookingPot) > 0;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public void resetCooking(BlockEntity blockEntity) {
            // 重置烹饪时间
            try {
                Field field = CookingPotBlockEntity.class.getDeclaredField("cookTime");
                field.setAccessible(true);
                field.setInt(cookingPot, 0);

                field = CookingPotBlockEntity.class.getDeclaredField("cookTimeTotal");
                field.setAccessible(true);
                field.setInt(cookingPot, 0);
            } catch (Exception e) {
                // 忽略错误
            }

            cookingPot.setChanged();
        }

        @Nullable
        @Override
        public Recipe<?> getCurrentRecipe(BlockEntity blockEntity) {
            if (cookingPot.getLevel() == null) return null;

            // 使用农夫乐事的方法获取当前配方
            try {
                var method = CookingPotBlockEntity.class.getDeclaredMethod(
                        "getMatchingRecipe", RecipeWrapper.class);
                method.setAccessible(true);

                RecipeWrapper wrapper = new RecipeWrapper(cookingPot.getInventory());
                Optional<CookingPotRecipe> recipe = (Optional<CookingPotRecipe>) method.invoke(cookingPot, wrapper);

                return recipe.orElse(null);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public Class<? extends BlockEntity> getBlockEntityClass() {
            return CookingPotBlockEntity.class;
        }
    }
}