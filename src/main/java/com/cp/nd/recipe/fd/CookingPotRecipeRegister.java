package com.cp.nd.recipe.fd;

import com.cp.nd.item.AbstractNamelessDishItem;
import com.cp.nd.api.INamelessDishRecipeRegister;
import com.cp.nd.util.RecipeUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import vectorwing.farmersdelight.client.recipebook.CookingPotRecipeBookTab;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 用于将无名料理注册为农夫乐事烹饪锅配方的工具类
 */
public class CookingPotRecipeRegister implements INamelessDishRecipeRegister {

    private static final Logger LOGGER = LogManager.getLogger(CookingPotRecipeRegister.class);

    // 支持的料理方块ID列表
    private static final Set<String> SUPPORTED_BLOCKS = Set.of(
            "farmersdelight:cooking_pot"
    );

    @Override
    public boolean isSupport(String cookingBlockId) {
        return SUPPORTED_BLOCKS.contains(cookingBlockId);
    }

    @Override
    public String getName() {
        return "CookingPotRecipeRegister";
    }

    @Override
    public Set<String> getSupportedBlocks() {
        return SUPPORTED_BLOCKS;
    }

    /**
     * 将配方注册到游戏系统
     */
    @Override
    public void registerToGame(Recipe<?> recipe) {
        if (recipe instanceof CookingPotRecipe cookingPotRecipe) {

        }
    }

    /**
     * 将无名料理ItemStack转换为CookingPotRecipe
     *
     * @param namelessDish 无名料理物品堆叠
     * @param recipeId 配方的资源位置（可选，如果为null则自动生成）
     * @return 对应的CookingPotRecipe，如果输入无效则返回null
     */
    @Override
    @Nullable
    public Recipe<?> createRecipeFromNamelessDish(ItemStack namelessDish, @Nullable ResourceLocation recipeId) {
        List<ItemStack> ingredients = AbstractNamelessDishItem.getIngredients(namelessDish);
        if (ingredients.isEmpty()) {
            LOGGER.warn("Cannot create recipe from nameless dish with no ingredients");
            return null;
        }

        ResourceLocation finalRecipeId = recipeId != null ? recipeId : generateRecipeId(namelessDish, ingredients);
        NonNullList<Ingredient> ingredientList = createIngredientList(ingredients);

        if (ingredientList.isEmpty()) {
            LOGGER.warn("Cannot create recipe with empty ingredient list");
            return null;
        }

        ItemStack container = AbstractNamelessDishItem.hasBowl(namelessDish) ?
                new ItemStack(net.minecraft.world.item.Items.BOWL) : ItemStack.EMPTY;
        int cookTime = ingredients.size() * 100;

        LOGGER.debug("Creating Cooking Pot recipe: {} with {} ingredients",
                finalRecipeId, ingredients.size());

        return new CookingPotRecipe(
                finalRecipeId,
                "nameless_dishes",
                CookingPotRecipeBookTab.MISC,
                ingredientList,
                namelessDish.copy(),
                container,
                0.0f,
                cookTime
        );
    }

    /**
     * 根据无名料理和原料生成唯一的配方ID
     */
    @SuppressWarnings("all")
    private static ResourceLocation generateRecipeId(ItemStack namelessDish, List<ItemStack> ingredients) {
        // 使用原料的哈希值生成唯一ID
        StringBuilder ingredientHash = new StringBuilder();

        // 对原料按物品ID排序以确保相同的原料组合生成相同的哈希值
        List<String> ingredientIds = new ArrayList<>();
        for (ItemStack ingredient : ingredients) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(ingredient.getItem());
            if (id != null) {
                ingredientIds.add(id.toString());
            } else {
                // 如果物品没有注册名，使用内部名称
                ingredientIds.add(ingredient.getItem().getDescriptionId());
            }
        }

        // 排序并构建哈希字符串
        ingredientIds.sort(String::compareTo);
        for (String id : ingredientIds) {
            ingredientHash.append(id).append(":");
        }

        // 添加原料数量的哈希
        ingredientHash.append(ingredients.size());

        // 添加容器信息
        if (AbstractNamelessDishItem.hasBowl(namelessDish)) {
            ingredientHash.append(":with_bowl");
        }

        // 生成UUID哈希以确保ID长度合适
        String hash = UUID.nameUUIDFromBytes(ingredientHash.toString().getBytes()).toString();
        // 移除连字符并取前12个字符
        String shortHash = hash.replace("-", "").substring(0, Math.min(12, hash.length() - 4));

        return new ResourceLocation("nameless_dishes", "autogen/" + shortHash);
    }

    /**
     * 将原料ItemStack列表转换为NonNullList<Ingredient>
     * 注意：每个原料创建一个单独的Ingredient，因为烹饪锅配方需要精确匹配原料数量
     */
    private static NonNullList<Ingredient> createIngredientList(List<ItemStack> ingredients) {
        NonNullList<Ingredient> ingredientList = NonNullList.create();

        for (ItemStack ingredient : ingredients) {
            if (!ingredient.isEmpty()) {
                // 创建单个物品的Ingredient
                ItemStack singleStack = ingredient.copy();
                singleStack.setCount(1);
                ingredientList.add(Ingredient.of(singleStack));
            }
        }

        return ingredientList;
    }
}