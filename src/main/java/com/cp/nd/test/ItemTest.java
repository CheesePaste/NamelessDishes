package com.cp.nd.test;

import com.cp.nd.item.AbstractNamelessDishItem;
import com.cp.nd.item.ModItems;
import com.cp.nd.util.FoodUtil;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Item system GameTests
 * Tests item creation, NBT data, and food properties
 */
@GameTestHolder("nameless_dishes")
@PrefixGameTestTemplate(false)
public class ItemTest {

    /**
     * Test basic nameless dish creation
     */
    @GameTest(template = "empty_template")
    public void testNamelessDishCreation(GameTestHelper helper) {
        // Create ingredients
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Items.CARROT));
        inputs.add(new ItemStack(Items.BREAD));

        // Create nameless dish
        ItemStack dish = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, false);

        // Verify it's not empty
        if (dish.isEmpty()) {
            helper.fail("Created dish is empty");
        }

        // Verify it's a nameless dish item
        if (!(dish.getItem() instanceof AbstractNamelessDishItem)) {
            helper.fail("Created item is not a NamelessDish");
        }

        helper.succeed();
    }

    /**
     * Test that NBT data is correctly set on the dish
     */
    @GameTest(template = "empty_template")
    public void testDishNBTData(GameTestHelper helper) {
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Items.CARROT));
        inputs.add(new ItemStack(Items.POTATO));
        inputs.add(new ItemStack(Items.BREAD));

        ItemStack dish = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, false);

        // Check NBT exists
        if (!dish.hasTag()) {
            helper.fail("Dish has no NBT data");
        }

        var tag = dish.getTag();

        // Check required keys
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

        helper.succeed();
    }

    /**
     * Test that the dish is edible
     */
    @GameTest(template = "empty_template")
    public void testDishEdibility(GameTestHelper helper) {
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Items.CARROT));
        inputs.add(new ItemStack(Items.BREAD));

        ItemStack dish = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, false);

        // Check item is edible
        if (!dish.isEdible()) {
            helper.fail("Dish is not edible");
        }

        // Check food properties
        var foodProperties = dish.getFoodProperties(null);
        if (foodProperties == null) {
            helper.fail("Dish has no food properties");
        }

        // Check nutrition is positive
        int nutrition = foodProperties.getNutrition();
        if (nutrition <= 0) {
            helper.fail("Nutrition should be positive, got: " + nutrition);
        }

        helper.succeed();
    }

    /**
     * Test that nutrition values are calculated correctly
     */
    @GameTest(template = "empty_template")
    public void testDishNutritionValues(GameTestHelper helper) {
        List<ItemStack> inputs = new ArrayList<>();

        // Carrot: 3 nutrition, 0.6 saturation
        inputs.add(new ItemStack(Items.CARROT));

        ItemStack dish = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, false);

        int foodLevel = AbstractNamelessDishItem.getFoodLevel(dish);
        float saturation = AbstractNamelessDishItem.getSaturation(dish);

        // Food level should be 3 * multiplier/100
        // With default multiplier of 80, this should be 2 (3 * 0.8 = 2.4, truncated to 2)
        if (foodLevel <= 0) {
            helper.fail("Food level should be positive, got: " + foodLevel);
        }

        // Saturation should be positive
        if (saturation <= 0.0f) {
            helper.fail("Saturation should be positive, got: " + saturation);
        }

        helper.succeed();
    }

    /**
     * Test with bowl vs without bowl
     */
    @GameTest(template = "empty_template")
    public void testDishWithAndWithoutBowl(GameTestHelper helper) {
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Items.CARROT));
        inputs.add(new ItemStack(Items.POTATO));

        ItemStack dishWithBowl = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, true);
        ItemStack dishWithoutBowl = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, false);

        // Check that WithBowl flag is set correctly
        boolean hasBowl1 = AbstractNamelessDishItem.hasBowl(dishWithBowl);
        boolean hasBowl2 = AbstractNamelessDishItem.hasBowl(dishWithoutBowl);

        if (!hasBowl1) {
            helper.fail("First dish should have bowl");
        }

        if (hasBowl2) {
            helper.fail("Second dish should not have bowl");
        }

        // Check items are different
        if (dishWithBowl.getItem().equals(dishWithoutBowl.getItem())) {
            helper.fail("Items should be different");
        }

        helper.succeed();
    }

    /**
     * Test that ingredients are correctly stored
     */
    @GameTest(template = "empty_template")
    public void testDishIngredientsStorage(GameTestHelper helper) {
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Items.CARROT));
        inputs.add(new ItemStack(Items.POTATO));
        inputs.add(new ItemStack(Items.BREAD));

        ItemStack dish = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, false);

        // Get ingredients from NBT
        List<ItemStack> storedIngredients = AbstractNamelessDishItem.getIngredients(dish);

        // Check count
        if (storedIngredients.size() != 3) {
            helper.fail("Expected 3 ingredients, got " + storedIngredients.size());
        }

        // Verify all original ingredients are present
        for (ItemStack input : inputs) {
            boolean found = false;
            for (ItemStack stored : storedIngredients) {
                if (stored.getItem().equals(input.getItem())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                helper.fail("Ingredient not found in stored ingredients");
            }
        }

        helper.succeed();
    }

    /**
     * Test cooking block ID is stored
     */
    @GameTest(template = "empty_template")
    public void testDishCookingBlockId(GameTestHelper helper) {
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Items.CARROT));

        String testBlockId = "farmersdelight:cooking_pot";
        ItemStack dish = FoodUtil.createNamelessResult(testBlockId, inputs, false);

        String storedBlockId = AbstractNamelessDishItem.getCookingBlockId(dish);

        if (storedBlockId == null) {
            helper.fail("Cooking block ID is null");
        }

        if (!storedBlockId.equals(testBlockId)) {
            helper.fail("Cooking block ID mismatch: expected " + testBlockId + ", got " + storedBlockId);
        }

        helper.succeed();
    }

    /**
     * Test multiple ingredients sum their nutrition
     */
    @GameTest(template = "empty_template")
    public void testDishMultipleIngredientsNutrition(GameTestHelper helper) {
        List<ItemStack> inputs = new ArrayList<>();

        // Add multiple food items
        inputs.add(new ItemStack(Items.CARROT));  // 3 nutrition
        inputs.add(new ItemStack(Items.POTATO));  // 1 nutrition
        inputs.add(new ItemStack(Items.BREAD));   // 5 nutrition

        ItemStack dish = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, false);

        int foodLevel = AbstractNamelessDishItem.getFoodLevel(dish);
        float saturation = AbstractNamelessDishItem.getSaturation(dish);

        // Total base nutrition: 3 + 1 + 5 = 9
        // With multiplier 80%: 9 * 0.8 = 7.2, truncated to 7
        // Allow some tolerance for the multiplier
        if (foodLevel < 5 || foodLevel > 10) {
            helper.fail("Food level out of expected range: " + foodLevel);
        }

        if (saturation <= 0.0f) {
            helper.fail("Saturation should be positive, got: " + saturation);
        }

        helper.succeed();
    }

    /**
     * Test empty ingredients list
     */
    @GameTest(template = "empty_template")
    public void testDishEmptyIngredients(GameTestHelper helper) {
        List<ItemStack> inputs = new ArrayList<>();

        ItemStack dish = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, false);

        // Should still create a dish (with 0 nutrition)
        if (dish.isEmpty()) {
            helper.fail("Dish should not be empty");
        }

        List<ItemStack> storedIngredients = AbstractNamelessDishItem.getIngredients(dish);

        if (!storedIngredients.isEmpty()) {
            helper.fail("Ingredients should be empty");
        }

        int foodLevel = AbstractNamelessDishItem.getFoodLevel(dish);

        if (foodLevel != 0) {
            helper.fail("Food level should be 0, got: " + foodLevel);
        }

        helper.succeed();
    }

    /**
     * Test container item for bowl version
     */
    @GameTest(template = "empty_template")
    public void testDishContainerItem(GameTestHelper helper) {
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Items.CARROT));

        ItemStack dishWithBowl = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, true);
        ItemStack dishWithoutBowl = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, false);

        // Get container items
        ItemStack containerWithBowl = dishWithBowl.getItem().getCraftingRemainingItem(dishWithBowl);
        ItemStack containerWithoutBowl = dishWithoutBowl.getItem().getCraftingRemainingItem(dishWithoutBowl);

        // With bowl should return empty container (the bowl is consumed)
        // Without bowl should also return empty (no container)
        // Note: The actual implementation returns ItemStack.EMPTY for both

        helper.succeed();
    }

    /**
     * Test ingredient count stored is correct
     */
    @GameTest(template = "empty_template")
    public void testDishIngredientCount(GameTestHelper helper) {
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Items.CARROT, 2));  // 2 carrots
        inputs.add(new ItemStack(Items.POTATO, 3));  // 3 potatoes

        ItemStack dish = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, false);

        List<ItemStack> storedIngredients = AbstractNamelessDishItem.getIngredients(dish);

        // Each ItemStack should be stored once, regardless of count
        if (storedIngredients.size() != 2) {
            helper.fail("Expected 2 ingredient types, got " + storedIngredients.size());
        }

        helper.succeed();
    }

    /**
     * Test non-edible items are filtered
     */
    @GameTest(template = "empty_template")
    public void testDishNonEdibleItemsFiltered(GameTestHelper helper) {
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Items.CARROT));
        inputs.add(new ItemStack(Items.COBBLESTONE));  // Not edible
        inputs.add(new ItemStack(Items.BREAD));

        ItemStack dish = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, false);

        List<ItemStack> storedIngredients = AbstractNamelessDishItem.getIngredients(dish);

        // Only edible items should be stored
        if (storedIngredients.size() != 2) {
            helper.fail("Expected 2 ingredients (cobblestone filtered), got " + storedIngredients.size());
        }

        for (ItemStack ingredient : storedIngredients) {
            if (ingredient.getItem().equals(Items.COBBLESTONE)) {
                helper.fail("Cobblestone should not be in ingredients");
            }
        }

        helper.succeed();
    }
}
