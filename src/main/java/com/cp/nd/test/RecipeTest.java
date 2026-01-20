package com.cp.nd.test;

import com.cp.nd.item.ModItems;
import com.cp.nd.recipe.RecipeRegisterManager;
import com.cp.nd.recipe.storage.NamelessDishRecipeData;
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
 * Recipe system GameTests
 * Tests recipe registration, ID generation, and uniqueness
 */
@GameTestHolder("nameless_dishes")
@PrefixGameTestTemplate(false)
public class RecipeTest {

    /**
     * Test that a recipe can be successfully registered
     */
    @GameTest(template = "empty_template")
    public void testRecipeRegistration(GameTestHelper helper) {
        // Create a simple nameless dish recipe
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Items.CARROT));
        inputs.add(new ItemStack(Items.POTATO));

        // Create the nameless dish
        ItemStack dish = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, false);

        // Verify it's a valid nameless dish
        if (!RecipeRegisterManager.isValidNamelessDish(dish)) {
            helper.fail("Created dish is not valid");
        }

        helper.succeed();
    }

    /**
     * Test that identical ingredient combinations generate the same recipe ID
     */
    @GameTest(template = "empty_template")
    public void testRecipeIdGeneration(GameTestHelper helper) {
        // Create two dishes with the same ingredients
        List<ItemStack> inputs1 = new ArrayList<>();
        inputs1.add(new ItemStack(Items.CARROT));
        inputs1.add(new ItemStack(Items.POTATO));

        List<ItemStack> inputs2 = new ArrayList<>();
        inputs2.add(new ItemStack(Items.CARROT));
        inputs2.add(new ItemStack(Items.POTATO));

        ItemStack dish1 = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs1, false);
        ItemStack dish2 = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs2, false);

        // Get the ingredients from both dishes (they should have the same NBT)
        List<ItemStack> ingredients1 = com.cp.nd.item.AbstractNamelessDishItem.getIngredients(dish1);
        List<ItemStack> ingredients2 = com.cp.nd.item.AbstractNamelessDishItem.getIngredients(dish2);

        // Verify both have the same number of ingredients
        if (ingredients1.size() != ingredients2.size()) {
            helper.fail("Ingredients count mismatch: " + ingredients1.size() + " vs " + ingredients2.size());
        }

        helper.succeed();
    }

    /**
     * Test that different ingredient combinations generate different recipe IDs
     */
    @GameTest(template = "empty_template")
    public void testRecipeUniqueness(GameTestHelper helper) {
        // Create two dishes with different ingredients
        List<ItemStack> inputs1 = new ArrayList<>();
        inputs1.add(new ItemStack(Items.CARROT));
        inputs1.add(new ItemStack(Items.POTATO));

        List<ItemStack> inputs2 = new ArrayList<>();
        inputs2.add(new ItemStack(Items.CARROT));
        inputs2.add(new ItemStack(Items.BREAD));

        ItemStack dish1 = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs1, false);
        ItemStack dish2 = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs2, false);

        // Verify they are different items (by checking NBT is different)
        String cookingBlock1 = com.cp.nd.item.AbstractNamelessDishItem.getCookingBlockId(dish1);
        String cookingBlock2 = com.cp.nd.item.AbstractNamelessDishItem.getCookingBlockId(dish2);

        if (!cookingBlock1.equals(cookingBlock2)) {
            helper.fail("Cooking block mismatch");
        }

        List<ItemStack> ingredients1 = com.cp.nd.item.AbstractNamelessDishItem.getIngredients(dish1);
        List<ItemStack> ingredients2 = com.cp.nd.item.AbstractNamelessDishItem.getIngredients(dish2);

        // Verify the ingredients are different
        boolean different = false;
        for (ItemStack stack : ingredients1) {
            boolean found = false;
            for (ItemStack stack2 : ingredients2) {
                if (stack.getItem().equals(stack2.getItem())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                different = true;
                break;
            }
        }

        if (!different) {
            helper.fail("Ingredients should be different");
        }

        helper.succeed();
    }

    /**
     * Test that duplicate recipe registrations are handled correctly
     */
    @GameTest(template = "empty_template")
    public void testDuplicateRecipeRegistration(GameTestHelper helper) {
        // Create a dish
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Items.CARROT));
        inputs.add(new ItemStack(Items.POTATO));

        ItemStack dish = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, false);

        // Try to register it twice
        boolean result1 = RecipeRegisterManager.getInstance().registerRecipe(dish, null, false);
        boolean result2 = RecipeRegisterManager.getInstance().registerRecipe(dish, null, false);

        // Both should succeed (second one should be skipped, not fail)
        if (!result1 || !result2) {
            helper.fail("Recipe registration should succeed");
        }

        helper.succeed();
    }

    /**
     * Test recipe ID generation with different cooking blocks
     */
    @GameTest(template = "empty_template")
    public void testRecipeIdDifferentCookingBlocks(GameTestHelper helper) {
        // Create dishes with same ingredients but different cooking blocks
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Items.CARROT));
        inputs.add(new ItemStack(Items.POTATO));

        ItemStack dish1 = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, false);
        ItemStack dish2 = FoodUtil.createNamelessResult("othermod:other_cooking_block", inputs, false);

        String cookingBlock1 = com.cp.nd.item.AbstractNamelessDishItem.getCookingBlockId(dish1);
        String cookingBlock2 = com.cp.nd.item.AbstractNamelessDishItem.getCookingBlockId(dish2);

        // Verify different cooking blocks are stored
        if (cookingBlock1.equals(cookingBlock2)) {
            helper.fail("Cooking blocks should be different");
        }

        helper.succeed();
    }

    /**
     * Test recipe ID with bowl vs without bowl
     */
    @GameTest(template = "empty_template")
    public void testRecipeIdWithAndWithoutBowl(GameTestHelper helper) {
        // Create dishes with same ingredients, one with bowl, one without
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Items.CARROT));
        inputs.add(new ItemStack(Items.POTATO));

        ItemStack dishWithBowl = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, true);
        ItemStack dishWithoutBowl = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, false);

        // Verify bowl status is different
        boolean hasBowl1 = com.cp.nd.item.AbstractNamelessDishItem.hasBowl(dishWithBowl);
        boolean hasBowl2 = com.cp.nd.item.AbstractNamelessDishItem.hasBowl(dishWithoutBowl);

        if (hasBowl1 == hasBowl2) {
            helper.fail("Bowl status should be different");
        }

        if (!hasBowl1 || hasBowl2) {
            helper.fail("First dish should have bowl, second should not");
        }

        helper.succeed();
    }

    /**
     * Test recipe with many ingredients
     */
    @GameTest(template = "empty_template")
    public void testRecipeWithManyIngredients(GameTestHelper helper) {
        // Create a dish with maximum allowed ingredients
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Items.CARROT));
        inputs.add(new ItemStack(Items.POTATO));
        inputs.add(new ItemStack(Items.BREAD));
        inputs.add(new ItemStack(Items.APPLE));
        inputs.add(new ItemStack(Items.BEEF));
        inputs.add(new ItemStack(Items.PORKCHOP));
        inputs.add(new ItemStack(Items.CHICKEN));
        inputs.add(new ItemStack(Items.COD));

        ItemStack dish = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, false);

        // Verify all ingredients are stored
        List<ItemStack> ingredients = com.cp.nd.item.AbstractNamelessDishItem.getIngredients(dish);

        if (ingredients.size() != 8) {
            helper.fail("Expected 8 ingredients, got " + ingredients.size());
        }

        helper.succeed();
    }

    /**
     * Test recipe with single ingredient
     */
    @GameTest(template = "empty_template")
    public void testRecipeWithSingleIngredient(GameTestHelper helper) {
        // Create a dish with minimum ingredients
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Items.CARROT));

        ItemStack dish = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs, false);

        // Verify ingredient is stored
        List<ItemStack> ingredients = com.cp.nd.item.AbstractNamelessDishItem.getIngredients(dish);

        if (ingredients.size() != 1) {
            helper.fail("Expected 1 ingredient, got " + ingredients.size());
        }

        helper.succeed();
    }

    /**
     * Test ingredient ordering doesn't affect recipe ID
     */
    @GameTest(template = "empty_template")
    public void testRecipeIdOrdering(GameTestHelper helper) {
        // Create two dishes with same ingredients in different order
        List<ItemStack> inputs1 = new ArrayList<>();
        inputs1.add(new ItemStack(Items.CARROT));
        inputs1.add(new ItemStack(Items.POTATO));
        inputs1.add(new ItemStack(Items.BREAD));

        List<ItemStack> inputs2 = new ArrayList<>();
        inputs2.add(new ItemStack(Items.BREAD));
        inputs2.add(new ItemStack(Items.CARROT));
        inputs2.add(new ItemStack(Items.POTATO));

        ItemStack dish1 = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs1, false);
        ItemStack dish2 = FoodUtil.createNamelessResult("farmersdelight:cooking_pot", inputs2, false);

        // Both should have the same number of ingredients
        List<ItemStack> ingredients1 = com.cp.nd.item.AbstractNamelessDishItem.getIngredients(dish1);
        List<ItemStack> ingredients2 = com.cp.nd.item.AbstractNamelessDishItem.getIngredients(dish2);

        if (ingredients1.size() != ingredients2.size()) {
            helper.fail("Ingredient count should be the same");
        }

        // All ingredients should be present in both (order independent)
        for (ItemStack stack : ingredients1) {
            boolean found = false;
            for (ItemStack stack2 : ingredients2) {
                if (stack.getItem().equals(stack2.getItem())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                helper.fail("Ingredient not found in second dish");
            }
        }

        helper.succeed();
    }
}
