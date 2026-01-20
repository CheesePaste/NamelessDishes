package com.cp.nd.recipe.storage;

import com.cp.nd.item.AbstractNamelessDishItem;
import com.google.gson.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 无名料理配方数据模型
 */
public class NamelessDishRecipeData {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    // 配方基本信息
    private final String recipeId;
    private final String cookingBlockId;
    private final String outputItemId;
    private final CompoundTag outputNbt;
    private final int foodLevel;
    private final float saturation;
    private final boolean withBowl;

    // 原料列表
    private final List<IngredientData> ingredients;

    // 元数据
    private final String creationTime;
    private final String lastModified;
    private String displayName;

    public NamelessDishRecipeData(String recipeId, String cookingBlockId,
                                  ItemStack output, List<IngredientData> ingredients,
                                  int foodLevel, float saturation, boolean withBowl) {
        this.recipeId = recipeId;
        this.cookingBlockId = cookingBlockId;
        this.outputItemId = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(output.getItem())).toString();
        this.outputNbt = output.getTag() != null ? output.getTag().copy() : new CompoundTag();
        this.ingredients = ingredients;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.withBowl = withBowl;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String time = LocalDateTime.now().format(formatter);
        this.creationTime = time;
        this.lastModified = time;
    }

    /**
     * 从ItemStack创建配方数据
     */
    public static NamelessDishRecipeData fromItemStack(ItemStack dishStack, @Nullable String recipeId) {
        if (!(dishStack.getItem() instanceof AbstractNamelessDishItem)) {
            throw new IllegalArgumentException("Item is not a NamelessDish");
        }

        // 获取基础信息
        String cookingBlockId = AbstractNamelessDishItem.getCookingBlockId(dishStack);
        if (cookingBlockId == null) {
            throw new IllegalStateException("Dish has no cooking block information");
        }

        // 生成配方ID
        String finalRecipeId = recipeId != null ? recipeId : generateRecipeId(dishStack);

        // 获取原料数据
        List<IngredientData> ingredients = new ArrayList<>();
        List<ItemStack> ingredientStacks = AbstractNamelessDishItem.getIngredients(dishStack);
        for (ItemStack ingredient : ingredientStacks) {
            ingredients.add(IngredientData.fromItemStack(ingredient));
        }

        return new NamelessDishRecipeData(
                finalRecipeId,
                cookingBlockId,
                dishStack,
                ingredients,
                AbstractNamelessDishItem.getFoodLevel(dishStack),
                AbstractNamelessDishItem.getSaturation(dishStack),
                AbstractNamelessDishItem.hasBowl(dishStack)
        );
    }

    /**
     * 生成唯一的配方ID
     */
    private static String generateRecipeId(ItemStack dishStack) {
        // 使用原料哈希生成ID
        List<ItemStack> ingredients = AbstractNamelessDishItem.getIngredients(dishStack);
        StringBuilder hashBuilder = new StringBuilder();

        // 对原料排序以确保相同的原料组合生成相同的ID
        List<String> ingredientIds = new ArrayList<>();
        for (ItemStack ingredient : ingredients) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(ingredient.getItem());
            if (id != null) {
                ingredientIds.add(id.toString());
            }
        }

        ingredientIds.sort(String::compareTo);
        for (String id : ingredientIds) {
            hashBuilder.append(id).append("_");
        }

        // 添加料理方块信息
        String cookingBlockId = AbstractNamelessDishItem.getCookingBlockId(dishStack);
        if (cookingBlockId != null) {
            hashBuilder.append(cookingBlockId.replace(":", "_"));
        }

        // 添加容器信息
        if (AbstractNamelessDishItem.hasBowl(dishStack)) {
            hashBuilder.append("_bowl");
        }

        // 生成UUID并取前8位
        String hash = UUID.nameUUIDFromBytes(hashBuilder.toString().getBytes()).toString();
        return "autogen_" + hash.substring(0, 8);
    }

    /**
     * 转换为JSON对象
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();

        // 配方基本信息
        json.addProperty("recipe_id", recipeId);
        json.addProperty("cooking_block", cookingBlockId);

        // 输出物品
        JsonObject output = new JsonObject();
        output.addProperty("item", outputItemId);
        output.addProperty("count", 1);
        if (outputNbt != null && !outputNbt.isEmpty()) {
            output.addProperty("nbt", outputNbt.toString());
        }
        json.add("output", output);

        // 食物属性
        JsonObject foodProperties = new JsonObject();
        foodProperties.addProperty("food_level", foodLevel);
        foodProperties.addProperty("saturation", saturation);
        json.add("food_properties", foodProperties);

        // 原料列表
        JsonArray ingredientsArray = new JsonArray();
        for (IngredientData ingredient : ingredients) {
            ingredientsArray.add(ingredient.toJson());
        }
        json.add("ingredients", ingredientsArray);

        // 容器信息
        if (withBowl) {
            json.addProperty("container", "minecraft:bowl");
        }

        // 元数据
        json.addProperty("creation_time", creationTime);
        json.addProperty("last_modified", lastModified);
        if (displayName != null) {
            json.addProperty("display_name", displayName);
        }

        return json;
    }

    /**
     * 序列化为JSON字符串
     */
    public String toJsonString() {
        return GSON.toJson(toJson());
    }

    /**
     * 从JSON解析
     */
    public static NamelessDishRecipeData fromJson(JsonObject json) {
        try {
            // 解析基本信息
            String recipeId = json.get("recipe_id").getAsString();
            String cookingBlockId = json.get("cooking_block").getAsString();

            // 解析输出物品
            JsonObject outputJson = json.getAsJsonObject("output");
            String outputItemId = outputJson.get("item").getAsString();
            int count = outputJson.has("count") ? outputJson.get("count").getAsInt() : 1;

            CompoundTag outputNbt = null;
            if (outputJson.has("nbt")) {
                try {
                    outputNbt = TagParser.parseTag(outputJson.get("nbt").getAsString());
                } catch (Exception e) {
                    throw new JsonParseException("Failed to parse output NBT", e);
                }
            }

            // 解析食物属性
            JsonObject foodProps = json.getAsJsonObject("food_properties");
            int foodLevel = foodProps.get("food_level").getAsInt();
            float saturation = foodProps.get("saturation").getAsFloat();

            // 解析容器信息
            boolean withBowl = false;
            if (json.has("container")) {
                String container = json.get("container").getAsString();
                withBowl = container.equals("minecraft:bowl");
            }

            // 解析原料列表
            List<IngredientData> ingredients = new ArrayList<>();
            JsonArray ingredientsArray = json.getAsJsonArray("ingredients");
            for (JsonElement element : ingredientsArray) {
                ingredients.add(IngredientData.fromJson(element.getAsJsonObject()));
            }

            // 创建输出ItemStack
            @SuppressWarnings("all")
            ItemStack outputStack = new ItemStack(
                    Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(outputItemId))),
                    count
            );
            if (outputNbt != null && !outputNbt.isEmpty()) {
                outputStack.setTag(outputNbt);
            }

            // 创建并返回配方数据
            NamelessDishRecipeData recipeData = new NamelessDishRecipeData(
                    recipeId,
                    cookingBlockId,
                    outputStack,
                    ingredients,
                    foodLevel,
                    saturation,
                    withBowl
            );

            // 设置显示名称
            if (json.has("display_name")) {
                recipeData.setDisplayName(json.get("display_name").getAsString());
            }

            return recipeData;

        } catch (Exception e) {
            throw new JsonParseException("Failed to parse recipe JSON", e);
        }
    }

    // 添加setter方法用于显示名称
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    // 添加getter方法
    public String getDisplayName() {
        return displayName;
    }

    // 添加获取输出ItemStack的方法
    public ItemStack createOutputStack() {
        @SuppressWarnings("all")
        ItemStack stack = new ItemStack(
                Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(outputItemId))),
                1
        );
        if (outputNbt != null && !outputNbt.isEmpty()) {
            stack.setTag(outputNbt.copy());
        }
        return stack;
    }

    // Getter方法
    public String getRecipeId() { return recipeId; }
    public String getCookingBlockId() { return cookingBlockId; }
    public String getOutputItemId() { return outputItemId; }
    public CompoundTag getOutputNbt() { return outputNbt; }
    public List<IngredientData> getIngredients() { return ingredients; }
    public int getFoodLevel() { return foodLevel; }
    public float getSaturation() { return saturation; }
    public boolean isWithBowl() { return withBowl; }

    /**
     * 原料数据类
     */
    public static class IngredientData {
        private final String itemId;
        private final int count;
        @Nullable
        private final CompoundTag nbt;

        public IngredientData(String itemId, int count, @Nullable CompoundTag nbt) {
            this.itemId = itemId;
            this.count = count;
            this.nbt = nbt;
        }

        public static IngredientData fromItemStack(ItemStack stack) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null) {
                throw new IllegalArgumentException("Item not registered");
            }

            return new IngredientData(
                    id.toString(),
                    stack.getCount(),
                    stack.getTag() != null ? stack.getTag().copy() : null
            );
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("item", itemId);
            json.addProperty("count", count);
            if (nbt != null && !nbt.isEmpty()) {
                json.addProperty("nbt", nbt.toString());
            }
            return json;
        }
        public static IngredientData fromJson(JsonObject json) {
            String itemId = json.get("item").getAsString();
            int count = json.has("count") ? json.get("count").getAsInt() : 1;

            CompoundTag nbt = null;
            if (json.has("nbt")) {
                try {
                    nbt = TagParser.parseTag(json.get("nbt").getAsString());
                } catch (Exception e) {
                    throw new JsonParseException("Failed to parse ingredient NBT", e);
                }
            }

            return new IngredientData(itemId, count, nbt);
        }

        // 添加转换为ItemStack的方法
        public ItemStack createItemStack() {
            @SuppressWarnings("all")
            ItemStack stack = new ItemStack(
                    Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId))),
                    count
            );
            if (nbt != null && !nbt.isEmpty()) {
                stack.setTag(nbt.copy());
            }
            return stack;
        }


        public String getItemId() { return itemId; }
        public int getCount() { return count; }
        @Nullable
        public CompoundTag getNbt() { return nbt; }
    }
}