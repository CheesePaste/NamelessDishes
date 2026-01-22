// com.cp.nd.util.RecipeMatcher.java
package com.cp.nd.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class RecipeMatcher {
    static final Logger LOGGER = LogManager.getLogger(RecipeMatcher.class);

    public static class MatchResult {
        public final Recipe<?> recipe;
        public final float similarity;
        public final String hint;
        public final List<ItemStack> missingItems;
        public final List<ItemStack> extraItems;
        public final int inputCount;
        public final int recipeCount;

        public MatchResult(Recipe<?> recipe, float similarity, String hint,
                           List<ItemStack> missingItems, List<ItemStack> extraItems,
                           int inputCount, int recipeCount) {
            this.recipe = recipe;
            this.similarity = similarity;
            this.hint = hint;
            this.missingItems = missingItems;
            this.extraItems = extraItems;
            this.inputCount = inputCount;
            this.recipeCount = recipeCount;
        }

        public boolean hasMissingItems() {
            return !missingItems.isEmpty();
        }

        public boolean hasExtraItems() {
            return !extraItems.isEmpty();
        }

        public boolean isCloseMatch() {
            return similarity >= 0.6f;
        }

        public boolean isVeryCloseMatch() {
            return similarity >= 0.8f;
        }
    }

    /**
     * 计算两个物品列表的相似度（改进版Jaccard系数）
     */
    public static float calculateSimilarity(List<ItemStack> inputItems, List<Ingredient> recipeIngredients) {
        if (inputItems.isEmpty() && recipeIngredients.isEmpty()) return 1.0f;
        if (inputItems.isEmpty() || recipeIngredients.isEmpty()) return 0.0f;

        // 规范化输入物品（合并相同物品）
        Map<String, Integer> inputCounts = new HashMap<>();
        for (ItemStack stack : inputItems) {
            if (!stack.isEmpty()) {
                String itemId = stack.getItem().toString();
                inputCounts.put(itemId, inputCounts.getOrDefault(itemId, 0) + stack.getCount());
            }
        }

        // 规范化配方材料（考虑标签）
        Map<String, Set<String>> ingredientOptions = new HashMap<>();
        for (Ingredient ingredient : recipeIngredients) {
            Set<String> options = new HashSet<>();
            for (ItemStack item : ingredient.getItems()) {
                if (!item.isEmpty()) {
                    options.add(item.getItem().toString());
                }
            }
            ingredientOptions.put("ingredient_" + ingredient.hashCode(), options);
        }

        // 计算匹配分数
        int matches = 0;
        int total = recipeIngredients.size();

        // 复制输入用于匹配
        Map<String, Integer> remainingInputs = new HashMap<>(inputCounts);

        for (Set<String> options : ingredientOptions.values()) {
            boolean matched = false;
            // 尝试匹配每个选项
            for (String option : options) {
                if (option.startsWith("tag:")) {
                    // 标签匹配简化：如果输入中有任何物品属于该标签，则匹配
                    // 这里简化处理，实际需要检查标签
                    for (String inputItem : remainingInputs.keySet()) {
                        // 假设一个占位符匹配
                        if (remainingInputs.get(inputItem) > 0) {
                            matched = true;
                            remainingInputs.put(inputItem, remainingInputs.get(inputItem) - 1);
                            break;
                        }
                    }
                } else if (remainingInputs.containsKey(option) && remainingInputs.get(option) > 0) {
                    matched = true;
                    remainingInputs.put(option, remainingInputs.get(option) - 1);
                    break;
                }
            }
            if (matched) {
                matches++;
            }
        }

        // 计算相似度
        float ingredientSimilarity = (float) matches / total;

        // 考虑数量比例
        int totalInputCount = inputItems.stream()
                .mapToInt(ItemStack::getCount)
                .sum();
        int totalRecipeCount = recipeIngredients.size();
        float countRatio = totalRecipeCount > 0 ?
                (float) totalInputCount / totalRecipeCount : 1.0f;
        float countSimilarity = 1.0f - Math.min(1.0f, Math.abs(countRatio - 1.0f));

        // 加权综合相似度
        return ingredientSimilarity * 0.7f + countSimilarity * 0.3f;
    }

    /**
     * 找到最佳匹配的配方
     */
    public static MatchResult findBestMatch(


            List<ItemStack> inputItems,
            Collection<Recipe<?>> allRecipes,
            net.minecraft.world.item.crafting.RecipeType<?> recipeType) {

        if (allRecipes == null || allRecipes.isEmpty() || inputItems == null || inputItems.isEmpty()) {
            return null;
        }

        MatchResult bestMatch = null;
        float bestSimilarity = 0.0f;

        for (Recipe<?> recipe : allRecipes) {
                if (!recipe.getType().equals(recipeType)) {
                    continue;
                }

                List<Ingredient> recipeIngredients = recipe.getIngredients();
                float similarity = calculateSimilarity(inputItems, recipeIngredients);

                if (similarity > bestSimilarity && similarity >= 0.2f) { // 设置最小相似度阈值
                    bestSimilarity = similarity;

                    // 分析差异
                    List<ItemStack> missingItems = findMissingItems(inputItems, recipeIngredients);
                    List<ItemStack> extraItems = findExtraItems(inputItems, recipeIngredients);
                    String hint = generateHint(missingItems, extraItems,
                            inputItems.size(), recipeIngredients.size(), similarity);

                    bestMatch = new MatchResult(recipe, similarity, hint,
                            missingItems, extraItems, inputItems.size(), recipeIngredients.size());
                }

        }

        return bestMatch;
    }

    /**
     * 查找缺失的物品
     */
    private static List<ItemStack> findMissingItems(List<ItemStack> inputs, List<Ingredient> recipeIngredients) {
        List<ItemStack> missing = new ArrayList<>();

        // 复制输入用于匹配
        List<ItemStack> remainingInputs = inputs.stream()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .collect(Collectors.toList());

        for (Ingredient ingredient : recipeIngredients) {
            boolean found = false;

            // 尝试匹配每个输入物品
            for (int i = 0; i < remainingInputs.size(); i++) {
                ItemStack input = remainingInputs.get(i);
                if (!input.isEmpty() && ingredient.test(input)) {
                    found = true;
                    input.shrink(1);
                    if (input.getCount() <= 0) {
                        remainingInputs.remove(i);
                    }
                    break;
                }
            }

            if (!found && ingredient.getItems().length > 0) {
                // 添加第一个可能的物品作为示例
                missing.add(ingredient.getItems()[0].copy());
            }
        }

        return missing;
    }

    /**
     * 查找多余的物品
     */
    private static List<ItemStack> findExtraItems(List<ItemStack> inputs, List<Ingredient> recipeIngredients) {
        List<ItemStack> extra = new ArrayList<>();

        // 复制输入用于匹配
        List<ItemStack> remainingInputs = inputs.stream()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();

        // 标记使用的物品
        boolean[] used = new boolean[remainingInputs.size()];

        for (Ingredient ingredient : recipeIngredients) {
            for (int i = 0; i < remainingInputs.size(); i++) {
                if (!used[i] && ingredient.test(remainingInputs.get(i))) {
                    used[i] = true;
                    remainingInputs.get(i).shrink(1);
                    if (remainingInputs.get(i).getCount() <= 0) {
                        used[i] = false; // 标记为完全使用
                    }
                    break;
                }
            }
        }

        // 收集未使用的物品
        for (int i = 0; i < remainingInputs.size(); i++) {
            if (!used[i] && remainingInputs.get(i).getCount() > 0) {
                ItemStack copy = remainingInputs.get(i).copy();
                copy.setCount(1); // 只添加一个作为示例
                extra.add(copy);
            }
        }

        return extra;
    }

    /**
     * 生成提示
     */
    private static String generateHint(List<ItemStack> missing, List<ItemStack> extra,
                                       int inputCount, int recipeCount, float similarity) {
        Random random = new Random();
        List<String> possibleHints = new ArrayList<>();

        // 基础提示
        if (similarity < 0.3f) {
            possibleHints.addAll(Arrays.asList(
                    "这种组合似乎不太对...",
                    "料理方式需要调整。",
                    "尝试不同的搭配吧！",
                    "这样的搭配似乎不太合适。"
            ));
        } else if (similarity < 0.5f) {
            possibleHints.addAll(Arrays.asList(
                    "有些接近，但还需要调整。",
                    "方向对了，但细节需要改进。",
                    "离成功还有一段距离。",
                    "可以尝试调整材料。"
            ));
        } else if (similarity < 0.7f) {
            possibleHints.addAll(Arrays.asList(
                    "已经很接近了，再调整一下！",
                    "快要成功了，只差一点！",
                    "料理逐渐成形了。",
                    "很好的尝试，继续努力！"
            ));
        } else {
            possibleHints.addAll(Arrays.asList(
                    "非常接近了！",
                    "几乎就要成功了！",
                    "就差最后一步了！",
                    "马上就要完成了！"
            ));
        }

        // 数量提示
        if (inputCount < recipeCount * 0.5) {
            possibleHints.addAll(Arrays.asList(
                    "食材好像加太少了...",
                    "需要更多食材来完成料理。",
                    "材料不足，无法形成完整的料理。",
                    "份量不够，再多加一些吧。"
            ));
        } else if (inputCount > recipeCount * 1.5) {
            possibleHints.addAll(Arrays.asList(
                    "食材好像加太多了...",
                    "材料过多，反而破坏了平衡。",
                    "料理需要更精确的配比。",
                    "或许可以减少一些材料。"
            ));
        }

        // 缺失材料提示
        if (!missing.isEmpty()) {
                String itemName = missing.get(0).getHoverName().getString();
                possibleHints.addAll(Arrays.asList(
                        String.format("试试加入%s？", itemName),
                        String.format("%s似乎不可或缺。", itemName),
                        String.format("缺少%s的感觉。", itemName),
                        String.format("也许需要一点%s。", itemName)
                ));
        }

        // 多余材料提示
        if (!extra.isEmpty()) {
                String itemName = extra.get(0).getHoverName().getString();
                possibleHints.addAll(Arrays.asList(
                        String.format("%s在这里好像不太合适...", itemName),
                        String.format("也许不需要%s？", itemName),
                        String.format("%s似乎有点多余。", itemName),
                        String.format("%s可能不适合这道料理。", itemName)
                ));
        }

        return possibleHints.get(random.nextInt(possibleHints.size()));
    }

    /**
     * 获取所有可能的提示类型
     */
    public static List<String> getAllHintTypes() {
        return Arrays.asList(
                "quantity_few", "quantity_many",
                "material_missing", "material_extra",
                "combination_wrong", "close_match"
        );
    }

    /**
     * 根据匹配结果确定提示类型
     */
    public static HintType determineHintType(MatchResult match) {
        if (match == null) {
            return HintType.GENERAL;
        }

        if (match.isVeryCloseMatch()) {
            return HintType.CLOSE_TO_RECIPE;
        }

        if (match.inputCount < match.recipeCount * 0.5) {
            return HintType.TOO_FEW;
        } else if (match.inputCount > match.recipeCount * 1.5) {
            return HintType.TOO_MANY;
        }

        if (match.hasMissingItems()) {
            return HintType.MISSING_ITEM;
        }

        if (match.hasExtraItems()) {
            return HintType.WRONG_ITEM;
        }

        return HintType.GENERAL;
    }

    public enum HintType {
        TOO_FEW(0xFFAA00, "textures/gui/toast/hint_too_few.png"),
        TOO_MANY(0xFF5555, "textures/gui/toast/hint_too_many.png"),
        WRONG_ITEM(0xFF5555, "textures/gui/toast/hint_wrong.png"),
        MISSING_ITEM(0x55AAFF, "textures/gui/toast/hint_missing.png"),
        GENERAL(0xAAAAAA, "textures/gui/toast/hint_general.png"),
        CLOSE_TO_RECIPE(0x55FF55, "textures/gui/toast/hint_close.png");

        private final int color;
        private final String texturePath;

        HintType(int color, String texturePath) {
            this.color = color;
            this.texturePath = texturePath;
        }

        public int getColor() {
            return color;
        }

        public String getTexturePath() {
            return texturePath;
        }

        public net.minecraft.sounds.SoundEvent getSound() {
            return switch (this) {
                case CLOSE_TO_RECIPE -> net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value();
                case TOO_FEW -> net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BIT.value();
                case TOO_MANY -> net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value();
                case MISSING_ITEM -> net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value();
                default -> net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.value();
            };
        }
    }
}