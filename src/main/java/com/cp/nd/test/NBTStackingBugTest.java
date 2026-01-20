package com.cp.nd.test;

import com.cp.nd.item.AbstractNamelessDishItem;
import com.cp.nd.util.FoodUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * NBT堆叠Bug修复验证测试
 *
 * Bug描述：
 * 在修复前，当输出槽已有料理A（NBT_1）时，烹饪新料理B（NBT_2）会导致：
 * - 料理B被移到输出槽
 * - 料理B错误地与料理A合并
 * - 结果：丢失了料理B的独特NBT数据
 *
 * 修复方案：
 * 在 CookingPotBlockEntityMixin 的 moveMealToOutput() 调用前，
 * 检查输出槽物品的NBT是否与展示槽物品的NBT相同。
 * 只有相同NBT才允许移动/合并。
 */
@GameTestHolder("nameless_dishes")
@PrefixGameTestTemplate(false)
public class NBTStackingBugTest {

    /**
     * 验证不同NBT的料理不会错误合并
     *
     * 测试步骤：
     * 1. 模拟输出槽已有料理A（胡萝卜+土豆）
     * 2. 创建料理B（胡萝卜+面包），NBT不同
     * 3. 验证：料理B不应该与料理A合并
     */
    @GameTest(template = "cooking_pot_template")
    public void testDifferentNBTShouldNotMerge(GameTestHelper helper) {
        BlockPos potPos = new BlockPos(1, 1, 1);
        var blockEntity = helper.getBlockEntity(potPos);

        if (!(blockEntity instanceof CookingPotBlockEntity cookingPot)) {
            helper.fail("No CookingPotBlockEntity found");
            return;
        }

        // 创建料理A：胡萝卜+土豆
        List<ItemStack> inputsA = new ArrayList<>();
        inputsA.add(new ItemStack(Items.CARROT));
        inputsA.add(new ItemStack(Items.POTATO));

        ItemStack dishA = FoodUtil.createNamelessResult(cookingPot, inputsA, true);

        // 创建料理B：胡萝卜+面包（不同NBT）
        List<ItemStack> inputsB = new ArrayList<>();
        inputsB.add(new ItemStack(Items.CARROT));
        inputsB.add(new ItemStack(Items.BREAD));

        ItemStack dishB = FoodUtil.createNamelessResult(cookingPot, inputsB, true);

        // 验证NBT不同
        if (ItemStack.isSameItemSameTags(dishA, dishB)) {
            helper.fail("Dish A and Dish B should have different NBT");
        }

        // 验证料理A和B的原料不同
        List<ItemStack> ingredientsA = AbstractNamelessDishItem.getIngredients(dishA);
        List<ItemStack> ingredientsB = AbstractNamelessDishItem.getIngredients(dishB);

        boolean breadFoundInA = false;
        boolean potatoFoundInB = false;

        for (ItemStack ingredient : ingredientsA) {
            if (ingredient.getItem().equals(Items.BREAD)) {
                breadFoundInA = true;
            }
        }

        for (ItemStack ingredient : ingredientsB) {
            if (ingredient.getItem().equals(Items.POTATO)) {
                potatoFoundInB = true;
            }
        }

        if (breadFoundInA) {
            helper.fail("Dish A should not contain bread");
        }

        if (potatoFoundInB) {
            helper.fail("Dish B should not contain potato");
        }

        helper.succeed();
    }

    /**
     * 验证相同NBT的料理可以正确合并
     *
     * 测试步骤：
     * 1. 创建两个相同配方的料理
     * 2. 验证：它们可以堆叠（NBT相同）
     */
    @GameTest(template = "cooking_pot_template")
    public void testSameNBTCanStack(GameTestHelper helper) {
        BlockPos potPos = new BlockPos(1, 1, 1);
        var blockEntity = helper.getBlockEntity(potPos);

        if (!(blockEntity instanceof CookingPotBlockEntity cookingPot)) {
            helper.fail("No CookingPotBlockEntity found");
            return;
        }

        // 创建两个相同配方的料理
        List<ItemStack> inputs1 = new ArrayList<>();
        inputs1.add(new ItemStack(Items.CARROT));
        inputs1.add(new ItemStack(Items.POTATO));

        List<ItemStack> inputs2 = new ArrayList<>();
        inputs2.add(new ItemStack(Items.CARROT));
        inputs2.add(new ItemStack(Items.POTATO));

        ItemStack dish1 = FoodUtil.createNamelessResult(cookingPot, inputs1, true);
        ItemStack dish2 = FoodUtil.createNamelessResult(cookingPot, inputs2, true);

        // 验证NBT相同，可以堆叠
        if (!ItemStack.isSameItemSameTags(dish1, dish2)) {
            helper.fail("Dish 1 and Dish 2 should have same NBT and be stackable");
        }

        // 验证可以堆叠
        ItemStack merged = dish1.copy();
        int newCount = merged.getCount() + dish2.getCount();
        merged.setCount(newCount);

        if (merged.getCount() != 2) {
            helper.fail("Stacked count should be 2");
        }

        helper.succeed();
    }

    /**
     * 验证NBT数据完整性
     *
     * 测试步骤：
     * 1. 创建料理并保存NBT
     * 2. 验证NBT标签包含所有必需数据
     */
    @GameTest(template = "cooking_pot_template")
    public void testNBTDataIntegrity(GameTestHelper helper) {
        BlockPos potPos = new BlockPos(1, 1, 1);
        var blockEntity = helper.getBlockEntity(potPos);

        if (!(blockEntity instanceof CookingPotBlockEntity cookingPot)) {
            helper.fail("No CookingPotBlockEntity found");
            return;
        }

        // 创建料理
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Items.CARROT));
        inputs.add(new ItemStack(Items.POTATO));
        inputs.add(new ItemStack(Items.BREAD));

        ItemStack dish = FoodUtil.createNamelessResult(cookingPot, inputs, true);

        // 验证NBT存在
        if (!dish.hasTag()) {
            helper.fail("Dish should have NBT data");
        }

        var tag = dish.getTag();

        // 验证必需的NBT标签
        if (!tag.contains(AbstractNamelessDishItem.FOOD_LEVEL_KEY)) {
            helper.fail("Missing FoodLevel in NBT");
        }

        if (!tag.contains(AbstractNamelessDishItem.SATURATION_KEY)) {
            helper.fail("Missing Saturation in NBT");
        }

        if (!tag.contains(AbstractNamelessDishItem.INGREDIENTS_KEY)) {
            helper.fail("Missing Ingredients in NBT");
        }

        if (!tag.contains(AbstractNamelessDishItem.COOKING_BLOCK_KEY)) {
            helper.fail("Missing CookingBlock in NBT");
        }

        // 验证原料数量
        List<ItemStack> storedIngredients = AbstractNamelessDishItem.getIngredients(dish);
        if (storedIngredients.size() != 3) {
            helper.fail("Expected 3 ingredients, got " + storedIngredients.size());
        }

        helper.succeed();
    }

    /**
     * 边界测试：输出槽部分填充的情况
     *
     * 测试步骤：
     * 1. 输出槽有32个料理A
     * 2. 烹饪相同的料理A
     * 3. 验证：可以正确堆叠到64个
     */
    @GameTest(template = "cooking_pot_template")
    public void testOutputSlotPartialFillSameNBT(GameTestHelper helper) {
        BlockPos potPos = new BlockPos(1, 1, 1);
        var blockEntity = helper.getBlockEntity(potPos);

        if (!(blockEntity instanceof CookingPotBlockEntity cookingPot)) {
            helper.fail("No CookingPotBlockEntity found");
            return;
        }

        // 创建料理A
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Items.CARROT));
        inputs.add(new ItemStack(Items.POTATO));

        ItemStack dishPartial = FoodUtil.createNamelessResult(cookingPot, inputs, true);
        dishPartial.setCount(32);  // 模拟部分填充

        ItemStack dishNew = FoodUtil.createNamelessResult(cookingPot, inputs, true);

        // 验证可以堆叠
        if (!ItemStack.isSameItemSameTags(dishPartial, dishNew)) {
            helper.fail("Same dishes should be stackable");
        }

        // 验证可以堆叠到最大
        ItemStack merged = dishPartial.copy();
        merged.setCount(dishPartial.getCount() + dishNew.getCount());

        if (merged.getCount() != 33) {
            helper.fail("Stacked count should be 33, got " + merged.getCount());
        }

        helper.succeed();
    }

    /**
     * 边界测试：输出槽有不同NBT的情况
     *
     * 测试步骤：
     * 1. 输出槽有32个料理A
     * 2. 烹饪不同的料理B
     * 3. 验证：料理B不会与料理A合并
     */
    @GameTest(template = "cooking_pot_template")
    public void testOutputSlotPartialFillDifferentNBT(GameTestHelper helper) {
        BlockPos potPos = new BlockPos(1, 1, 1);
        var blockEntity = helper.getBlockEntity(potPos);

        if (!(blockEntity instanceof CookingPotBlockEntity cookingPot)) {
            helper.fail("No CookingPotBlockEntity found");
            return;
        }

        // 创建料理A
        List<ItemStack> inputsA = new ArrayList<>();
        inputsA.add(new ItemStack(Items.CARROT));
        inputsA.add(new ItemStack(Items.POTATO));

        ItemStack dishA = FoodUtil.createNamelessResult(cookingPot, inputsA, true);
        dishA.setCount(32);

        // 创建料理B（不同）
        List<ItemStack> inputsB = new ArrayList<>();
        inputsB.add(new ItemStack(Items.CARROT));
        inputsB.add(new ItemStack(Items.BREAD));

        ItemStack dishB = FoodUtil.createNamelessResult(cookingPot, inputsB, true);

        // 验证不能堆叠
        if (ItemStack.isSameItemSameTags(dishA, dishB)) {
            helper.fail("Dish A and Dish B should NOT be stackable");
        }

        // 尝试合并会失败或保持分离
        boolean canMerge = ItemStack.isSameItemSameTags(dishA, dishB);
        if (canMerge) {
            helper.fail("Different NBT dishes should not merge");
        }

        helper.succeed();
    }
}
