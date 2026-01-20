package com.cp.nd.recipe.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for NamelessDishRecipeData
 * Focuses on JSON serialization and data model logic
 */
class NamelessDishRecipeDataTest {

    /**
     * Test basic recipe data creation
     */
    @Test
    void testRecipeDataCreation_Basic() {
        List<NamelessDishRecipeData.IngredientData> ingredients = new ArrayList<>();
        ingredients.add(new NamelessDishRecipeData.IngredientData("minecraft:carrot", 1, null));

        NamelessDishRecipeData recipeData = new NamelessDishRecipeData(
                "test_recipe",
                "farmersdelight:cooking_pot",
                ingredients,
                false
        );

        assertEquals("test_recipe", recipeData.getRecipeId());
        assertEquals("farmersdelight:cooking_pot", recipeData.getCookingBlockId());
        assertFalse(recipeData.isWithBowl());
        assertEquals(1, recipeData.getIngredients().size());
    }

    /**
     * Test recipe data with bowl
     */
    @Test
    void testRecipeDataCreation_WithBowl() {
        List<NamelessDishRecipeData.IngredientData> ingredients = new ArrayList<>();

        NamelessDishRecipeData recipeData = new NamelessDishRecipeData(
                "test_recipe",
                "farmersdelight:cooking_pot",
                ingredients,
                true
        );

        assertTrue(recipeData.isWithBowl());
    }

    /**
     * Test toJson - basic structure
     */
    @Test
    void testToJson_BasicStructure() {
        List<NamelessDishRecipeData.IngredientData> ingredients = new ArrayList<>();
        ingredients.add(new NamelessDishRecipeData.IngredientData("minecraft:carrot", 1, null));

        NamelessDishRecipeData recipeData = new NamelessDishRecipeData(
                "test_recipe",
                "farmersdelight:cooking_pot",
                ingredients,
                false
        );

        JsonObject json = recipeData.toJson();

        assertTrue(json.has("recipe_id"));
        assertTrue(json.has("cooking_block"));
        assertTrue(json.has("ingredients"));

        assertEquals("test_recipe", json.get("recipe_id").getAsString());
        assertEquals("farmersdelight:cooking_pot", json.get("cooking_block").getAsString());
    }

    /**
     * Test toJson - with bowl
     */
    @Test
    void testToJson_WithBowl() {
        List<NamelessDishRecipeData.IngredientData> ingredients = new ArrayList<>();

        NamelessDishRecipeData recipeData = new NamelessDishRecipeData(
                "test_recipe",
                "farmersdelight:cooking_pot",
                ingredients,
                true
        );

        JsonObject json = recipeData.toJson();

        assertTrue(json.has("container"));
        assertEquals("minecraft:bowl", json.get("container").getAsString());
    }

    /**
     * Test toJson - without bowl (no container field)
     */
    @Test
    void testToJson_WithoutBowl() {
        List<NamelessDishRecipeData.IngredientData> ingredients = new ArrayList<>();

        NamelessDishRecipeData recipeData = new NamelessDishRecipeData(
                "test_recipe",
                "farmersdelight:cooking_pot",
                ingredients,
                false
        );

        JsonObject json = recipeData.toJson();

        assertFalse(json.has("container"));
    }

    /**
     * Test toJson - ingredients array
     */
    @Test
    void testToJson_IngredientsArray() {
        List<NamelessDishRecipeData.IngredientData> ingredients = new ArrayList<>();
        ingredients.add(new NamelessDishRecipeData.IngredientData("minecraft:carrot", 2, null));
        ingredients.add(new NamelessDishRecipeData.IngredientData("minecraft:potato", 1, null));

        NamelessDishRecipeData recipeData = new NamelessDishRecipeData(
                "test_recipe",
                "farmersdelight:cooking_pot",
                ingredients,
                false
        );

        JsonObject json = recipeData.toJson();

        assertTrue(json.has("ingredients"));
        var ingredientsArray = json.getAsJsonArray("ingredients");
        assertEquals(2, ingredientsArray.size());

        // Check first ingredient
        JsonObject firstIngredient = ingredientsArray.get(0).getAsJsonObject();
        assertEquals("minecraft:carrot", firstIngredient.get("item").getAsString());
        assertEquals(2, firstIngredient.get("count").getAsInt());
    }

    /**
     * Test toJsonString produces valid JSON
     */
    @Test
    void testToJsonString_ValidJson() {
        List<NamelessDishRecipeData.IngredientData> ingredients = new ArrayList<>();
        ingredients.add(new NamelessDishRecipeData.IngredientData("minecraft:carrot", 1, null));

        NamelessDishRecipeData recipeData = new NamelessDishRecipeData(
                "test_recipe",
                "farmersdelight:cooking_pot",
                ingredients,
                false
        );

        String jsonString = recipeData.toJsonString();

        // Should be parseable JSON
        assertDoesNotThrow(() -> JsonParser.parseString(jsonString));
    }

    /**
     * Test IngredientData creation
     */
    @Test
    void testIngredientData_Creation() {
        NamelessDishRecipeData.IngredientData ingredient =
                new NamelessDishRecipeData.IngredientData("minecraft:carrot", 3, null);

        assertEquals("minecraft:carrot", ingredient.getItemId());
        assertEquals(3, ingredient.getCount());
        assertNull(ingredient.getNbt()); // Note: need to add getter or use reflection
    }

    /**
     * Test IngredientData toJson
     */
    @Test
    void testIngredientData_ToJson() {
        NamelessDishRecipeData.IngredientData ingredient =
                new NamelessDishRecipeData.IngredientData("minecraft:carrot", 2, null);

        JsonObject json = ingredient.toJson();

        assertEquals("minecraft:carrot", json.get("item").getAsString());
        assertEquals(2, json.get("count").getAsInt());
        assertFalse(json.has("nbt"));
    }

    /**
     * Test IngredientData toJson with NBT
     */
    @Test
    void testIngredientData_ToJson_WithNBT() throws Exception {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("TestValue", 123);
        nbt.putString("TestString", "test");

        NamelessDishRecipeData.IngredientData ingredient =
                new NamelessDishRecipeData.IngredientData("minecraft:carrot", 1, nbt);

        JsonObject json = ingredient.toJson();

        assertTrue(json.has("nbt"));
        String nbtString = json.get("nbt").getAsString();
        assertTrue(nbtString.contains("TestValue"));
        assertTrue(nbtString.contains("TestString"));
    }

    /**
     * Test IngredientData fromJson
     */
    @Test
    void testIngredientData_FromJson() {
        String jsonStr = "{\"item\":\"minecraft:carrot\",\"count\":2}";
        JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();

        NamelessDishRecipeData.IngredientData ingredient =
                NamelessDishRecipeData.IngredientData.fromJson(json);

        assertEquals("minecraft:carrot", ingredient.getItemId());
        assertEquals(2, ingredient.getCount());
    }

    /**
     * Test IngredientData fromJson with default count
     */
    @Test
    void testIngredientData_FromJson_DefaultCount() {
        String jsonStr = "{\"item\":\"minecraft:carrot\"}";
        JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();

        NamelessDishRecipeData.IngredientData ingredient =
                NamelessDishRecipeData.IngredientData.fromJson(json);

        assertEquals("minecraft:carrot", ingredient.getItemId());
        assertEquals(1, ingredient.getCount());
    }

    /**
     * Test IngredientData fromJson with NBT
     */
    @Test
    void testIngredientData_FromJson_WithNBT() throws Exception {
        CompoundTag originalNbt = new CompoundTag();
        originalNbt.putInt("TestValue", 123);
        String nbtString = originalNbt.toString();

        String jsonStr = "{\"item\":\"minecraft:carrot\",\"count\":1,\"nbt\":\"" +
                nbtString.replace("\"", "\\\"") + "\"}";
        JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();

        NamelessDishRecipeData.IngredientData ingredient =
                NamelessDishRecipeData.IngredientData.fromJson(json);

        assertNotNull(ingredient.getNbt()); // Note: need to add getter
        // The NBT should be parseable
        assertDoesNotThrow(() -> {
            CompoundTag nbt = ingredient.getNbt(); // Assuming we add this getter
            assertEquals(123, nbt.getInt("TestValue"));
        });
    }

    /**
     * Test fromJson with missing required fields - recipe_id
     */
    @Test
    void testFromJson_MissingRecipeId() {
        String jsonStr = "{\"cooking_block\":\"farmersdelight:cooking_pot\",\"ingredients\":[]}";
        JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();

        assertThrows(Exception.class, () -> {
            NamelessDishRecipeData.fromJson(json);
        });
    }

    /**
     * Test fromJson with missing required fields - cooking_block
     */
    @Test
    void testFromJson_MissingCookingBlock() {
        String jsonStr = "{\"recipe_id\":\"test\",\"ingredients\":[]}";
        JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();

        assertThrows(Exception.class, () -> {
            NamelessDishRecipeData.fromJson(json);
        });
    }

    /**
     * Test fromJson with missing required fields - ingredients
     */
    @Test
    void testFromJson_MissingIngredients() {
        String jsonStr = "{\"recipe_id\":\"test\",\"cooking_block\":\"farmersdelight:cooking_pot\"}";
        JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();

        assertThrows(Exception.class, () -> {
            NamelessDishRecipeData.fromJson(json);
        });
    }

    /**
     * Test setDisplayName and display in JSON
     */
    @Test
    void testSetDisplayName_IncludedInJson() {
        List<NamelessDishRecipeData.IngredientData> ingredients = new ArrayList<>();

        NamelessDishRecipeData recipeData = new NamelessDishRecipeData(
                "test_recipe",
                "farmersdelight:cooking_pot",
                ingredients,
                false
        );

        recipeData.setDisplayName("Test Display Name");

        JsonObject json = recipeData.toJson();

        assertTrue(json.has("display_name"));
        assertEquals("Test Display Name", json.get("display_name").getAsString());
    }

    /**
     * Test toJson without display name (no field in JSON)
     */
    @Test
    void testToJson_NoDisplayName() {
        List<NamelessDishRecipeData.IngredientData> ingredients = new ArrayList<>();

        NamelessDishRecipeData recipeData = new NamelessDishRecipeData(
                "test_recipe",
                "farmersdelight:cooking_pot",
                ingredients,
                false
        );

        JsonObject json = recipeData.toJson();

        assertFalse(json.has("display_name"));
    }

    /**
     * Test empty ingredients list
     */
    @Test
    void testRecipeData_EmptyIngredients() {
        List<NamelessDishRecipeData.IngredientData> ingredients = new ArrayList<>();

        NamelessDishRecipeData recipeData = new NamelessDishRecipeData(
                "test_recipe",
                "farmersdelight:cooking_pot",
                ingredients,
                false
        );

        assertTrue(recipeData.getIngredients().isEmpty());

        JsonObject json = recipeData.toJson();
        var ingredientsArray = json.getAsJsonArray("ingredients");
        assertEquals(0, ingredientsArray.size());
    }

    /**
     * Test multiple ingredients
     */
    @Test
    void testRecipeData_MultipleIngredients() {
        List<NamelessDishRecipeData.IngredientData> ingredients = new ArrayList<>();
        ingredients.add(new NamelessDishRecipeData.IngredientData("minecraft:carrot", 2, null));
        ingredients.add(new NamelessDishRecipeData.IngredientData("minecraft:potato", 3, null));
        ingredients.add(new NamelessDishRecipeData.IngredientData("minecraft:bread", 1, null));

        NamelessDishRecipeData recipeData = new NamelessDishRecipeData(
                "test_recipe",
                "farmersdelight:cooking_pot",
                ingredients,
                true
        );

        assertEquals(3, recipeData.getIngredients().size());
    }

    /**
     * Test recipe ID with special characters
     */
    @Test
    void testRecipeId_SpecialCharacters() {
        List<NamelessDishRecipeData.IngredientData> ingredients = new ArrayList<>();

        NamelessDishRecipeData recipeData = new NamelessDishRecipeData(
                "autogen_12345678",
                "farmersdelight:cooking_pot",
                ingredients,
                false
        );

        assertEquals("autogen_12345678", recipeData.getRecipeId());

        JsonObject json = recipeData.toJson();
        String jsonString = json.toString();
        assertTrue(jsonString.contains("autogen_12345678"));
    }
}
