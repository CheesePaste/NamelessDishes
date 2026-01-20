package com.cp.nd.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


public abstract class AbstractNamelessDishItem extends Item {

    public static final String FOOD_LEVEL_KEY = "FoodLevel";
    public static final String SATURATION_KEY = "Saturation";
    public static final String INGREDIENTS_KEY = "Ingredients";
    public static final String COOKING_BLOCK_KEY = "CookingBlock";

    public AbstractNamelessDishItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip,@NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        CompoundTag tag = stack.getTag();
        if (tag != null) {
            // 显示原料列表
            if (tag.contains(INGREDIENTS_KEY, Tag.TAG_LIST)) {
                ListTag ingredients = tag.getList(INGREDIENTS_KEY, Tag.TAG_COMPOUND);
                if (!ingredients.isEmpty()) {
                    tooltip.add(Component.translatable("tooltip.nameless_dishes.ingredients")
                            .withStyle(ChatFormatting.DARK_GRAY));

                    for (int i = 0; i < ingredients.size(); i++) {
                        CompoundTag ingredientTag = ingredients.getCompound(i);
                        ItemStack ingredientStack = ItemStack.of(ingredientTag);
                        if (!ingredientStack.isEmpty()) {
                            tooltip.add(Component.literal("  ").append(
                                            ingredientStack.getHoverName())
                                    .withStyle(ChatFormatting.DARK_GREEN));
                        }
                    }
                }
            }
        }
    }

    @Override
    public FoodProperties getFoodProperties() {
        // 动态获取食物属性
        return null; // 在ItemStack实例中处理
    }
    @Override
    public boolean isEdible() {
        return true;
    }

    @Override
    public FoodProperties getFoodProperties(ItemStack stack, LivingEntity entity) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(FOOD_LEVEL_KEY)) {
            int foodLevel = tag.getInt(FOOD_LEVEL_KEY);
            float saturation = tag.getFloat(SATURATION_KEY);

            return new FoodProperties.Builder()
                    .nutrition(foodLevel)
                    .saturationMod(saturation)
                    .alwaysEat()
                    .build();
        }
        return super.getFoodProperties(stack, entity);
    }

    public static ItemStack createDish(Item dishItem, int foodLevel, float saturation,
                                       List<ItemStack> ingredients, boolean withBowl,
                                       @Nullable String cookingBlockId) { // 新增参数
        ItemStack stack = new ItemStack(dishItem);
        setDishData(stack, foodLevel, saturation, ingredients, withBowl, cookingBlockId);
        return stack;
    }

    public static void setDishData(ItemStack stack, int foodLevel, float saturation,
                                   List<ItemStack> ingredients, boolean withBowl,
                                   @Nullable String cookingBlockId) { // 新增参数
        CompoundTag tag = stack.getOrCreateTag();

        // 设置基础属性
        tag.putInt(FOOD_LEVEL_KEY, foodLevel);
        tag.putFloat(SATURATION_KEY, saturation);
        tag.putBoolean("WithBowl", withBowl);

        // 保存料理方块信息
        if (cookingBlockId != null && !cookingBlockId.isEmpty()) {
            tag.putString(COOKING_BLOCK_KEY, cookingBlockId);
        }

        // 保存原料列表
        ListTag ingredientsList = new ListTag();
        for (ItemStack ingredient : ingredients) {
            if (!ingredient.isEmpty()) {
                CompoundTag ingredientTag = new CompoundTag();
                ItemStack singleItem = ingredient.copy();
                singleItem.setCount(1);
                singleItem.save(ingredientTag);
                ingredientsList.add(ingredientTag);
            }
        }
        tag.put(INGREDIENTS_KEY, ingredientsList);
    }


    @Nullable
    public static String getCookingBlockId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(COOKING_BLOCK_KEY) ?
                tag.getString(COOKING_BLOCK_KEY) : null;
    }


    public static boolean isMadeByCookingBlock(ItemStack stack, String cookingBlockId) {
        String storedId = getCookingBlockId(stack);
        return storedId != null && storedId.equals(cookingBlockId);
    }

    public static int getFoodLevel(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt(FOOD_LEVEL_KEY) : 0;
    }

    public static float getSaturation(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getFloat(SATURATION_KEY) : 0.0f;
    }

    public static List<ItemStack> getIngredients(ItemStack stack) {
        List<ItemStack> ingredients = new ArrayList<>();
        CompoundTag tag = stack.getTag();

        if (tag != null && tag.contains(INGREDIENTS_KEY, Tag.TAG_LIST)) {
            ListTag ingredientsList = tag.getList(INGREDIENTS_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < ingredientsList.size(); i++) {
                CompoundTag ingredientTag = ingredientsList.getCompound(i);
                ingredients.add(ItemStack.of(ingredientTag));
            }
        }

        return ingredients;
    }

    public static boolean hasBowl(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean("WithBowl");
    }

    public ItemStack getContainerItem()
    {
        return ItemStack.EMPTY;
    }
}