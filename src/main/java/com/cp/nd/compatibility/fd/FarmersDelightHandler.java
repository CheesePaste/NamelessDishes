/*package com.cp.nd.compatibility.fd;

import com.cp.nd.api.ICookingRecipeHandler;
import com.cp.nd.api.ICookingStation;
import com.cp.nd.config.NDConfig;
import com.cp.nd.util.ModDetectionUtil;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FarmersDelightHandler implements ICookingRecipeHandler {

    private static final int MEAL_DISPLAY_SLOT = 6;
    private static final int CONTAINER_SLOT = 7;
    private static final int OUTPUT_SLOT = 8;
    private static final int INPUT_SLOT_START = 0;
    private static final int INPUT_SLOT_END = 5;

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

    @Nullable
    @Override
    public Recipe<?> getExistingRecipe(Level level, BlockEntity blockEntity, List<ItemStack> inputs) {
        if (!(blockEntity instanceof CookingPotBlockEntity cookingPot) || level == null) {
            return null;
        }

        try {
            // 使用农夫乐事的方式检查配方
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
        if (!NDConfig.INSTANCE.enabledMods.get().contains(("farmersdelight"))) {
            return false;
        }

        // 检查输入物品数量是否在允许范围内
        int ingredientCount = getIngredientCount(inputs);
        if (ingredientCount < NDConfig.INSTANCE.minIngredients.get() ||
                ingredientCount > NDConfig.INSTANCE.maxIngredients.get()) {
            return false;
        }

        // 检查烹饪锅是否被加热
        if (cookingPot.isHeated() && NDConfig.INSTANCE.requireCooking.get()) {
            // 需要加热但未加热
            if (!cookingPot.isHeated(level, cookingPot.getBlockPos())) {
                return false;
            }
        }

        // 检查是否有现有配方
        Recipe<?> existingRecipe = getExistingRecipe(level, blockEntity, inputs);
        if (existingRecipe != null) {
            // 已有已知配方，不允许创建无名料理
            return false;
        }

        // 检查容器是否有效
        ItemStack containerStack = getContainerItem(cookingPot);
        if (NDConfig.INSTANCE.requireCooking.get() && containerStack.isEmpty()) {
            // 需要容器但没有容器
            return false;
        }

        return true;
    }

    @Nonnull
    @Override
    public ItemStack createNamelessResult(Level level, BlockEntity blockEntity, List<ItemStack> inputs) {
        if (!(blockEntity instanceof CookingPotBlockEntity)) {
            return ItemStack.EMPTY;
        }

        // 计算饱食度和饱和度
        float totalHunger = 0;
        float totalSaturation = 0;
        List<String> ingredientTypes = new ArrayList<>();

        for (ItemStack input : inputs) {
            if (!input.isEmpty()) {
                // 获取食物属性（如果有）
                if (input.isEdible()) {
                    totalHunger += input.getItem().getFoodProperties().getNutrition();
                    totalSaturation += input.getItem().getFoodProperties().getSaturationModifier();
                }

                // 记录食材类型用于效果计算
                ingredientTypes.add(input.getItem().toString());
            }
        }

        // 应用配置乘数
        double baseHunger = totalHunger * (NDConfig.INSTANCE.baseHungerMultiplier.get() / 100.0f);
        double baseSaturation = (totalSaturation * NDConfig.INSTANCE.baseSaturationMultiplier.get());

        // 确保最小值
        baseHunger = Math.max(baseHunger, 1.0f);
        baseSaturation = Math.max(baseSaturation, 0.1f);

        // TODO: 这里应该创建自定义的无名料理物品
        // 暂时使用一个简单的食物作为占位符
        ItemStack result = new ItemStack(Items.BAKED_POTATO);
        // 设置饱食度和饱和度的NBT标签
        // result.getOrCreateTag().putFloat("NamelessHunger", baseHunger);
        // result.getOrCreateTag().putFloat("NamelessSaturation", baseSaturation);

        return result;
    }

    @Override
    public boolean executeCooking(Level level, BlockEntity blockEntity, List<ItemStack> inputs, ItemStack result) {
        if (!(blockEntity instanceof CookingPotBlockEntity cookingPot) || result.isEmpty()) {
            return false;
        }

        // 获取容器物品
        ItemStack container = getContainerItem(cookingPot);
        if (container.isEmpty()) {
            // 如果没有容器，使用默认碗
            container = new ItemStack(Items.BOWL);
        }

        try {
            // 设置结果到显示槽
            cookingPot.getInventory().setStackInSlot(MEAL_DISPLAY_SLOT, result.copy());

            // 设置容器
            cookingPot.getInventory().setStackInSlot(CONTAINER_SLOT, container);

            // 清空输入槽
            for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
                cookingPot.getInventory().setStackInSlot(i, ItemStack.EMPTY);
            }

            // 标记为已改变以更新客户端
            cookingPot.setChanged();

            return true;
        } catch (Exception e) {
            com.cp.nd.NamelessDishes.LOGGER.error("Error executing cooking in FarmersDelight cooking pot", e);
            return false;
        }
    }

    @Nullable
    @Override
    public ICookingStation getCookingStation(BlockEntity blockEntity) {
        if (!(blockEntity instanceof CookingPotBlockEntity cookingPot)) {
            return null;
        }

        return new FarmersDelightCookingStation(cookingPot);
    }

    // 辅助方法

    private List<ItemStack> getCurrentInputs(CookingPotBlockEntity cookingPot) {
        List<ItemStack> inputs = new ArrayList<>();

        for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
            ItemStack stack = cookingPot.getInventory().getStackInSlot(i);
            if (!stack.isEmpty()) {
                inputs.add(stack);
            }
        }

        return inputs;
    }

    private int getIngredientCount(List<ItemStack> inputs) {
        int count = 0;
        for (ItemStack stack : inputs) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private ItemStack getContainerItem(CookingPotBlockEntity cookingPot) {
        return cookingPot.getInventory().getStackInSlot(CONTAINER_SLOT);
    }

    // 农夫乐事烹饪站适配器
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
            // 检查烹饪时间是否大于0
            // 使用反射获取私有字段cookTime
            try {
                var field = CookingPotBlockEntity.class.getDeclaredField("cookTime");
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
                var field = CookingPotBlockEntity.class.getDeclaredField("cookTime");
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
        public @org.jetbrains.annotations.Nullable Class<? extends BlockEntity> getBlockEntityClass() {
            return CookingPotBlockEntity.class;
        }

        // 获取烹饪时间（用于事件监听）
        public int getCookTime(CookingPotBlockEntity cookingPot) {
            try {
                // 使用反射获取私有字段
                java.lang.reflect.Field field = CookingPotBlockEntity.class.getDeclaredField("cookTime");
                field.setAccessible(true);
                return (int) field.get(cookingPot);
            } catch (Exception e) {
                return 0;
            }
        }

        // 设置烹饪时间（用于事件监听）
        public void setCookTime(CookingPotBlockEntity cookingPot, int cookTime) {
            try {
                java.lang.reflect.Field field = CookingPotBlockEntity.class.getDeclaredField("cookTime");
                field.setAccessible(true);
                field.set(cookingPot, cookTime);
            } catch (Exception e) {
                // 忽略错误
            }
        }
    }
}

 */