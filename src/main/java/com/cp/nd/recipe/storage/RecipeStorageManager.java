package com.cp.nd.recipe.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 配方文件存储管理器
 * 负责将配方保存到文件系统和从文件系统加载
 */
public class RecipeStorageManager {
    private static final Logger LOGGER = LogManager.getLogger(RecipeStorageManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 单例实例
    private static final RecipeStorageManager INSTANCE = new RecipeStorageManager();

    // 基础存储路径
    private final Path baseStoragePath;

    // 缓存已加载的配方
    private final Map<String, List<NamelessDishRecipeData>> recipeCache = new HashMap<>();
    private final Map<String, Path> blockStoragePaths = new HashMap<>();

    private RecipeStorageManager() {
        // 存储路径：./config/nameless_dishes/recipes/
        this.baseStoragePath = FMLPaths.CONFIGDIR.get()
                .resolve("nameless_dishes")
                .resolve("recipes");

        // 确保基础目录存在
        createDirectoryIfNotExists(baseStoragePath);
    }

    public static RecipeStorageManager getInstance() {
        return INSTANCE;
    }

    /**
     * 保存配方到文件
     */
    public boolean saveRecipe(NamelessDishRecipeData recipeData) {
        try {
            // 获取料理方块对应的存储路径
            Path blockPath = getBlockStoragePath(recipeData.getCookingBlockId());
            if (blockPath == null) {
                LOGGER.error("Failed to get storage path for block: {}", recipeData.getCookingBlockId());
                return false;
            }

            // 生成文件名：recipe_id.json
            String fileName = recipeData.getRecipeId() + ".json";
            Path recipeFile = blockPath.resolve(fileName);

            // 检查文件是否已存在（如果存在则添加时间戳）
            if (Files.exists(recipeFile)) {
                LOGGER.warn("Recipe file already exists, saving as: {}", fileName);
                return false;
            }

            // 写入JSON文件
            String jsonContent = recipeData.toJsonString();
            Files.writeString(recipeFile, jsonContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            LOGGER.info("Saved recipe to file: {}", recipeFile);

            // 更新缓存
            recipeCache.computeIfAbsent(recipeData.getCookingBlockId(), k -> new ArrayList<>())
                    .add(recipeData);

            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to save recipe to file", e);
            return false;
        } catch (Exception e) {
            LOGGER.error("Unexpected error saving recipe", e);
            return false;
        }
    }

    /**
     * 批量保存配方
     */
    public int batchSaveRecipes(Collection<NamelessDishRecipeData> recipes) {
        int successCount = 0;
        for (NamelessDishRecipeData recipe : recipes) {
            if (saveRecipe(recipe)) {
                successCount++;
            }
        }
        LOGGER.info("Batch save completed: {}/{} recipes saved successfully", successCount, recipes.size());
        return successCount;
    }

    /**
     * 从文件加载指定料理方块的所有配方
     */
    public List<NamelessDishRecipeData> loadRecipes(String cookingBlockId) {
        // 检查缓存
        if (recipeCache.containsKey(cookingBlockId)) {
            return new ArrayList<>(recipeCache.get(cookingBlockId));
        }

        List<NamelessDishRecipeData> recipes = new ArrayList<>();
        try {
            Path blockPath = getBlockStoragePath(cookingBlockId);
            if (blockPath == null || !Files.exists(blockPath)) {
                return recipes;
            }

            // 遍历目录中的所有JSON文件
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(blockPath, "*.json")) {
                for (Path recipeFile : stream) {
                    try {
                        String jsonContent = Files.readString(recipeFile);
                        JsonObject json = JsonParser.parseString(jsonContent).getAsJsonObject();

                        NamelessDishRecipeData recipe = NamelessDishRecipeData.fromJson(json);
                        recipes.add(recipe);
                        LOGGER.debug("Loaded recipe from file: {}", recipeFile.getFileName());
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse recipe file: {}", recipeFile, e);
                    }
                }
            }

            // 更新缓存
            if (!recipes.isEmpty()) {
                recipeCache.put(cookingBlockId, new ArrayList<>(recipes));
                LOGGER.info("Loaded {} recipes for block: {}", recipes.size(), cookingBlockId);
            }

        } catch (IOException e) {
            LOGGER.error("Failed to load recipes for block: {}", cookingBlockId, e);
        }

        return recipes;
    }

    /**
     * 加载单个配方文件
     */
    @Nullable
    public NamelessDishRecipeData loadRecipe(String cookingBlockId, String recipeId) {
        try {
            Path blockPath = getBlockStoragePath(cookingBlockId);
            if (blockPath == null || !Files.exists(blockPath)) {
                return null;
            }

            // 尝试查找文件
            Path recipeFile = blockPath.resolve(recipeId + ".json");
            if (!Files.exists(recipeFile)) {
                // 如果没找到，尝试查找带时间戳的文件
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(blockPath, recipeId + "_*.json")) {
                    for (Path file : stream) {
                        recipeFile = file;
                        break;
                    }
                }
            }

            if (Files.exists(recipeFile)) {
                String jsonContent = Files.readString(recipeFile);
                JsonObject json = JsonParser.parseString(jsonContent).getAsJsonObject();
                return NamelessDishRecipeData.fromJson(json);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load recipe: {} for block: {}", recipeId, cookingBlockId, e);
        }

        return null;
    }
    /**
     * 加载所有已保存的配方（所有料理方块）
     */
    public Map<String, List<NamelessDishRecipeData>> loadAllRecipes() {
        Map<String, List<NamelessDishRecipeData>> allRecipes = new HashMap<>();

        try {
            // 遍历基础目录下的所有子目录（每个子目录对应一个料理方块）
            if (!Files.exists(baseStoragePath)) {
                return allRecipes;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseStoragePath)) {
                for (Path blockPath : stream) {
                    if (Files.isDirectory(blockPath)) {
                        // 从目录名解析料理方块ID
                        String blockDirName = blockPath.getFileName().toString();
                        String cookingBlockId = convertDirNameToBlockId(blockDirName);

                        if (cookingBlockId != null) {
                            List<NamelessDishRecipeData> recipes = loadRecipes(cookingBlockId);
                            allRecipes.put(cookingBlockId, recipes);
                        }
                    }
                }
            }

            LOGGER.info("Loaded recipes for {} cooking blocks", allRecipes.size());

        } catch (IOException e) {
            LOGGER.error("Failed to load all recipes", e);
        }

        return allRecipes;
    }

    /**
     * 删除指定配方
     */
    public boolean deleteRecipe(String cookingBlockId, String recipeId) {
        try {
            Path blockPath = getBlockStoragePath(cookingBlockId);
            if (blockPath == null || !Files.exists(blockPath)) {
                return false;
            }

            // 查找并删除文件
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(blockPath, recipeId + ".json")) {
                for (Path recipeFile : stream) {
                    Files.delete(recipeFile);
                    LOGGER.info("Deleted recipe file: {}", recipeFile);

                    // 从缓存中移除
                    if (recipeCache.containsKey(cookingBlockId)) {
                        recipeCache.get(cookingBlockId).removeIf(recipe ->
                                recipe.getRecipeId().equals(recipeId));
                    }

                    return true;
                }
            }

            // 如果没找到，尝试删除带时间戳的文件
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(blockPath, recipeId + "_*.json")) {
                for (Path recipeFile : stream) {
                    Files.delete(recipeFile);
                    LOGGER.info("Deleted recipe file: {}", recipeFile);

                    // 从缓存中移除
                    if (recipeCache.containsKey(cookingBlockId)) {
                        recipeCache.get(cookingBlockId).removeIf(recipe ->
                                recipe.getRecipeId().equals(recipeId));
                    }

                    return true;
                }
            }

        } catch (IOException e) {
            LOGGER.error("Failed to delete recipe: {}", recipeId, e);
        }

        return false;
    }

    /**
     * 获取料理方块的存储路径
     */
    @Nullable
    public Path getBlockStoragePath(String cookingBlockId) {
        // 检查缓存
        if (blockStoragePaths.containsKey(cookingBlockId)) {
            return blockStoragePaths.get(cookingBlockId);
        }

        // 将blockId转换为安全的目录名
        // 格式：namespace_path -> namespace_path
        // 例如：farmersdelight:cooking_pot -> farmersdelight_cooking_pot
        String dirName = convertBlockIdToDirName(cookingBlockId);
        if (dirName == null) {
            return null;
        }

        Path blockPath = baseStoragePath.resolve(dirName);

        // 创建目录
        if (!createDirectoryIfNotExists(blockPath)) {
            return null;
        }

        blockStoragePaths.put(cookingBlockId, blockPath);
        return blockPath;
    }

    /**
     * 将料理方块ID转换为目录名
     */
    private String convertBlockIdToDirName(String blockId) {
        if (blockId == null || blockId.isEmpty()) {
            return null;
        }

        // 替换冒号为下划线
        return blockId.replace(":", "_");
    }

    /**
     * 将目录名转换为料理方块ID
     */
    @Nullable
    private String convertDirNameToBlockId(String dirName) {
        if (dirName == null || dirName.isEmpty()) {
            return null;
        }

        // 将下划线替换回冒号
        // 注意：如果目录名中没有下划线，可能不是有效的方块ID
        if (!dirName.contains("_")) {
            return null;
        }

        // 第一个下划线之前的部分是modid，之后是路径
        int firstUnderscore = dirName.indexOf("_");
        String modid = dirName.substring(0, firstUnderscore);
        String path = dirName.substring(firstUnderscore + 1);

        return modid + ":" + path;
    }

    /**
     * 创建目录（如果不存在）
     */
    private boolean createDirectoryIfNotExists(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                LOGGER.debug("Created directory: {}", path);
            }
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to create directory: {}", path, e);
            return false;
        }
    }


    /**
     * 清空缓存
     */
    public void clearCache() {
        recipeCache.clear();
        blockStoragePaths.clear();
        LOGGER.debug("Cleared recipe cache");
    }

}