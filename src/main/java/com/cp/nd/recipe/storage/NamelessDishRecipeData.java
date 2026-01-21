package com.cp.nd.recipe.storage;

import com.cp.nd.item.AbstractNamelessDishItem;
import com.cp.nd.util.FoodUtil;
import com.google.gson.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
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
    private final String containerId;

    // 原料列表
    private final List<IngredientData> ingredients;

    //提前给以后命名无名料理做预留
    private String displayName;

    public NamelessDishRecipeData(String recipeId, String cookingBlockId,
                                  List<IngredientData> ingredients, String containerId) {
        this.recipeId = recipeId;
        this.cookingBlockId = cookingBlockId;
        this.ingredients = ingredients;
        this.containerId=containerId;

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
                ingredients,
                ((AbstractNamelessDishItem) dishStack.getItem()).getContainerItem().getItem().toString()
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



        // 原料列表
        JsonArray ingredientsArray = new JsonArray();
        for (IngredientData ingredient : ingredients) {
            ingredientsArray.add(ingredient.toJson());
        }
        json.add("ingredients", ingredientsArray);

        json.addProperty("container", containerId);

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

            String containerId= json.get("container").getAsString();

            // 解析原料列表
            List<IngredientData> ingredients = new ArrayList<>();
            JsonArray ingredientsArray = json.getAsJsonArray("ingredients");
            for (JsonElement element : ingredientsArray) {
                ingredients.add(IngredientData.fromJson(element.getAsJsonObject()));
            }

            // 创建输出ItemStack
            List<ItemStack> itemStacks=new ArrayList<>();
            for(IngredientData data : ingredients)
            {
                itemStacks.add(data.createItemStack());
            }
            @SuppressWarnings("all")
            ItemStack outputStack = FoodUtil.createNamelessResult(cookingBlockId,itemStacks,containerId);


            // 创建并返回配方数据
            NamelessDishRecipeData recipeData = new NamelessDishRecipeData(
                    recipeId,
                    cookingBlockId,
                    ingredients,
                    containerId
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



    // Getter方法
    public String getRecipeId() { return recipeId; }
    public String getCookingBlockId() { return cookingBlockId; }
    public List<IngredientData> getIngredients() { return ingredients; }
    public String getContainerId(){return containerId;}

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


    }
}