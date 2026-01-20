package com.cp.nd.recipe.storage;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RecipeStorageManager
 * Focuses on path conversion logic that doesn't require file I/O
 */
class RecipeStorageManagerTest {

    /**
     * Test convertBlockIdToDirName - standard case
     */
    @Test
    void testConvertBlockIdToDirName_StandardCase() throws Exception {
        RecipeStorageManager manager = RecipeStorageManager.getInstance();
        Method method = RecipeStorageManager.class.getDeclaredMethod(
                "convertBlockIdToDirName", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(manager, "farmersdelight:cooking_pot");

        assertEquals("farmersdelight_cooking_pot", result);
    }

    /**
     * Test convertBlockIdToDirName - null input
     */
    @Test
    void testConvertBlockIdToDirName_NullInput() throws Exception {
        RecipeStorageManager manager = RecipeStorageManager.getInstance();
        Method method = RecipeStorageManager.class.getDeclaredMethod(
                "convertBlockIdToDirName", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(manager, (String) null);

        assertNull(result);
    }

    /**
     * Test convertBlockIdToDirName - empty input
     */
    @Test
    void testConvertBlockIdToDirName_EmptyInput() throws Exception {
        RecipeStorageManager manager = RecipeStorageManager.getInstance();
        Method method = RecipeStorageManager.class.getDeclaredMethod(
                "convertBlockIdToDirName", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(manager, "");

        assertNull(result);
    }

    /**
     * Test convertBlockIdToDirName - multiple colons
     */
    @Test
    void testConvertBlockIdToDirName_MultipleColons() throws Exception {
        RecipeStorageManager manager = RecipeStorageManager.getInstance();
        Method method = RecipeStorageManager.class.getDeclaredMethod(
                "convertBlockIdToDirName", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(manager, "mod:block:sub:block");

        assertEquals("mod_block:sub:block", result);
    }

    /**
     * Test convertDirNameToBlockId - standard case
     */
    @Test
    void testConvertDirNameToBlockId_StandardCase() throws Exception {
        RecipeStorageManager manager = RecipeStorageManager.getInstance();
        Method method = RecipeStorageManager.class.getDeclaredMethod(
                "convertDirNameToBlockId", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(manager, "farmersdelight_cooking_pot");

        assertEquals("farmersdelight:cooking_pot", result);
    }

    /**
     * Test convertDirNameToBlockId - null input
     */
    @Test
    void testConvertDirNameToBlockId_NullInput() throws Exception {
        RecipeStorageManager manager = RecipeStorageManager.getInstance();
        Method method = RecipeStorageManager.class.getDeclaredMethod(
                "convertDirNameToBlockId", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(manager, (String) null);

        assertNull(result);
    }

    /**
     * Test convertDirNameToBlockId - empty input
     */
    @Test
    void testConvertDirNameToBlockId_EmptyInput() throws Exception {
        RecipeStorageManager manager = RecipeStorageManager.getInstance();
        Method method = RecipeStorageManager.class.getDeclaredMethod(
                "convertDirNameToBlockId", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(manager, "");

        assertNull(result);
    }

    /**
     * Test convertDirNameToBlockId - no underscore (invalid format)
     */
    @Test
    void testConvertDirNameToBlockId_NoUnderscore() throws Exception {
        RecipeStorageManager manager = RecipeStorageManager.getInstance();
        Method method = RecipeStorageManager.class.getDeclaredMethod(
                "convertDirNameToBlockId", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(manager, "invalidformat");

        assertNull(result);
    }

    /**
     * Test convertDirNameToBlockId - multiple underscores
     */
    @Test
    void testConvertDirNameToBlockId_MultipleUnderscores() throws Exception {
        RecipeStorageManager manager = RecipeStorageManager.getInstance();
        Method method = RecipeStorageManager.class.getDeclaredMethod(
                "convertDirNameToBlockId", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(manager, "mod_block_sub_block");

        assertEquals("mod:block_sub_block", result);
    }

    /**
     * Test round-trip conversion
     */
    @Test
    void testConvertBlockIdToDirName_RoundTrip() throws Exception {
        RecipeStorageManager manager = RecipeStorageManager.getInstance();
        Method toDirMethod = RecipeStorageManager.class.getDeclaredMethod(
                "convertBlockIdToDirName", String.class);
        Method toBlockMethod = RecipeStorageManager.class.getDeclaredMethod(
                "convertDirNameToBlockId", String.class);
        toDirMethod.setAccessible(true);
        toBlockMethod.setAccessible(true);

        String original = "farmersdelight:cooking_pot";
        String dirName = (String) toDirMethod.invoke(manager, original);
        String convertedBack = (String) toBlockMethod.invoke(manager, dirName);

        assertEquals(original, convertedBack);
    }

    /**
     * Test round-trip conversion with complex path
     */
    @Test
    void testConvertBlockIdToDirName_RoundTripComplex() throws Exception {
        RecipeStorageManager manager = RecipeStorageManager.getInstance();
        Method toDirMethod = RecipeStorageManager.class.getDeclaredMethod(
                "convertBlockIdToDirName", String.class);
        Method toBlockMethod = RecipeStorageManager.class.getDeclaredMethod(
                "convertDirNameToBlockId", String.class);
        toDirMethod.setAccessible(true);
        toBlockMethod.setAccessible(true);

        String original = "mymod:complex_block_path";
        String dirName = (String) toDirMethod.invoke(manager, original);
        String convertedBack = (String) toBlockMethod.invoke(manager, dirName);

        assertEquals(original, convertedBack);
    }

    /**
     * Test getInstance returns same instance
     */
    @Test
    void testGetInstance_Singleton() {
        RecipeStorageManager instance1 = RecipeStorageManager.getInstance();
        RecipeStorageManager instance2 = RecipeStorageManager.getInstance();

        assertSame(instance1, instance2);
    }

    /**
     * Test getBlockStoragePath caches results
     */
    @Test
    void testGetBlockStoragePath_Caching() throws Exception {
        RecipeStorageManager manager = RecipeStorageManager.getInstance();

        // First call
        Path path1 = manager.getBlockStoragePath("test:BLOCK");
        // Second call should return cached path
        Path path2 = manager.getBlockStoragePath("test:BLOCK");

        assertSame(path1, path2);
    }

    /**
     * Test clearCache
     */
    @Test
    void testClearCache() {
        RecipeStorageManager manager = RecipeStorageManager.getInstance();

        // Cache a path
        manager.clearCache();

        // After clearing, should be able to get new paths
        // This test just verifies the method doesn't throw
        assertDoesNotThrow(() -> manager.clearCache());
    }

    /**
     * Test block ID with only namespace (no path)
     */
    @Test
    void testConvertBlockIdToDirName_NamespaceOnly() throws Exception {
        RecipeStorageManager manager = RecipeStorageManager.getInstance();
        Method method = RecipeStorageManager.class.getDeclaredMethod(
                "convertBlockIdToDirName", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(manager, "minecraft:");

        assertEquals("minecraft_", result);
    }

    /**
     * Test dir name with namespace only
     */
    @Test
    void testConvertDirNameToBlockId_NamespaceOnly() throws Exception {
        RecipeStorageManager manager = RecipeStorageManager.getInstance();
        Method method = RecipeStorageManager.class.getDeclaredMethod(
                "convertDirNameToBlockId", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(manager, "minecraft_");

        assertEquals("minecraft:", result);
    }

    /**
     * Test convertBlockIdToDirName - numbers and special characters
     */
    @Test
    void testConvertBlockIdToDirName_SpecialCharacters() throws Exception {
        RecipeStorageManager manager = RecipeStorageManager.getInstance();
        Method method = RecipeStorageManager.class.getDeclaredMethod(
                "convertBlockIdToDirName", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(manager, "mod123:block_456");

        assertEquals("mod123_block_456", result);
    }
}
