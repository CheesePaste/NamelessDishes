package com.cp.nd.recipe;

import com.cp.nd.NamelessDishes;
import com.cp.nd.api.INamelessDishRecipeRegister;
import com.cp.nd.item.AbstractNamelessDishItem;
import com.cp.nd.recipe.fd.CookingPotRecipeRegister;
import com.cp.nd.recipe.storage.NamelessDishRecipeData;
import com.cp.nd.recipe.storage.RecipeStorageManager;
import com.cp.nd.util.FoodUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 无名料理配方注册管理器
 * 负责管理和分发配方注册任务，并集成配方存储功能
 */
public class RecipeRegisterManager {
    private static final Logger LOGGER = LogManager.getLogger(RecipeRegisterManager.class);

    // 单例实例
    private static final RecipeRegisterManager INSTANCE = new RecipeRegisterManager();

    // 注册器映射
    private final Map<String, INamelessDishRecipeRegister> registerByBlockId = new ConcurrentHashMap<>();
    private final List<INamelessDishRecipeRegister> dynamicRegisters = new ArrayList<>();

    // 配方存储管理器
    private final RecipeStorageManager storageManager;

    // 缓存已注册的配方ID，避免重复注册
    private final Set<String> registeredRecipeIds = ConcurrentHashMap.newKeySet();

    // 等待注册的配方队列（用于异步处理）
    private final Queue<RecipeRegistrationTask> pendingRegistrations = new LinkedList<>();

    // 控制变量
    private boolean isInitialized = false;

    /**
     * 配方注册任务类
     */
    private static class RecipeRegistrationTask {
        final ItemStack namelessDish;
        final ResourceLocation recipeId;
        final boolean saveToFile;

        RecipeRegistrationTask(ItemStack namelessDish, @Nullable ResourceLocation recipeId, boolean saveToFile) {
            this.namelessDish = namelessDish.copy();
            this.recipeId = recipeId;
            this.saveToFile = saveToFile;
        }
    }

    private RecipeRegisterManager() {
        this.storageManager = RecipeStorageManager.getInstance();

        // 注册默认的注册器
        registerDefaultRegisters();

        LOGGER.info("RecipeRegisterManager initialized");
    }

    public static RecipeRegisterManager getInstance() {
        return INSTANCE;
    }

    /**
     * 初始化管理器（在游戏加载时调用）
     */
    public void initialize(FMLCommonSetupEvent event) {
        if (isInitialized) {
            LOGGER.warn("RecipeRegisterManager already initialized");
            return;
        }

        event.enqueueWork(() -> {
            loadAndRegisterSavedRecipes();

            // 处理等待注册的配方
            processPendingRegistrations();

            isInitialized = true;
            LOGGER.info("RecipeRegisterManager setup completed");
        });
    }

    /**
     * 注册默认的注册器
     */
    private void registerDefaultRegisters() {
        registerRecipeRegister(new CookingPotRecipeRegister());
        LOGGER.debug("Default recipe registers registered");
    }

    /**
     * 注册新的配方注册器
     */
    public void registerRecipeRegister(INamelessDishRecipeRegister register) {
        Objects.requireNonNull(register, "Recipe register cannot be null");

        synchronized (this) {
            for (String blockId : register.getSupportedBlocks()) {
                registerByBlockId.put(blockId, register);
                LOGGER.debug("Registered {} for block: {}", register.getName(), blockId);
            }
        }
    }

    /**
     * 注册动态配方注册器
     */
    public void registerRecipeRegisterDynamic(INamelessDishRecipeRegister register) {
        Objects.requireNonNull(register, "Recipe register cannot be null");

        synchronized (this) {
            dynamicRegisters.add(register);
            for (String blockId : register.getSupportedBlocks()) {
                registerByBlockId.put(blockId, register);
                LOGGER.debug("Dynamic Registered {} for block: {}", register.getName(), blockId);
            }
            LOGGER.info("Registered dynamic recipe register: {}", register.getName());
        }
    }

    /**
     * 根据料理方块ID查找注册器
     */
    @Nullable
    public INamelessDishRecipeRegister findRegister(String cookingBlockId) {
        // 先检查静态注册器
        INamelessDishRecipeRegister register = registerByBlockId.get(cookingBlockId);

        // 如果没有找到，检查动态注册器
        if (register == null) {
            synchronized (this) {
                for (INamelessDishRecipeRegister dynamicRegister : dynamicRegisters) {
                    if (dynamicRegister.isSupport(cookingBlockId)) {
                        register = dynamicRegister;
                        registerByBlockId.put(cookingBlockId, register);
                        LOGGER.debug("Dynamic register {} supports block: {}",
                                register.getName(), cookingBlockId);
                        break;
                    }
                }
            }
        }

        return register;
    }

    /**
     * 验证无名料理是否有效
     */
    public static boolean isValidNamelessDish(ItemStack stack) {
        if (stack.isEmpty()) {
            LOGGER.warn("Empty stack provided for recipe registration");
            return false;
        }

        if (!(stack.getItem() instanceof AbstractNamelessDishItem)) {
            LOGGER.warn("Item {} is not a NamelessDish",
                    ForgeRegistries.ITEMS.getKey(stack.getItem()));
            return false;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(AbstractNamelessDishItem.INGREDIENTS_KEY)) {
            LOGGER.warn("NamelessDish {} is missing required NBT data",
                    ForgeRegistries.ITEMS.getKey(stack.getItem()));
            return false;
        }

        List<ItemStack> ingredients = AbstractNamelessDishItem.getIngredients(stack);
        if (ingredients.isEmpty()) {
            LOGGER.warn("NamelessDish {} has no ingredients",
                    ForgeRegistries.ITEMS.getKey(stack.getItem()));
            return false;
        }

        return true;
    }

    /**
     * 注册无名料理配方（可控制是否保存到文件）
     */
    public boolean registerRecipe(ItemStack namelessDish, @Nullable ResourceLocation recipeId, boolean saveToFile) {
        // 如果管理器未初始化，加入待处理队列
        if (!isInitialized) {
            pendingRegistrations.offer(new RecipeRegistrationTask(namelessDish, recipeId, saveToFile));
            LOGGER.debug("Recipe registration queued, waiting for initialization");
            return true;
        }

        return processRecipeRegistration(namelessDish, recipeId, saveToFile);
    }

    /**
     * 处理单个配方注册
     */
    private boolean processRecipeRegistration(ItemStack namelessDish, @Nullable ResourceLocation recipeId, boolean saveToFile) {
        if (!isValidNamelessDish(namelessDish)) {
            LOGGER.error("Invalid nameless dish provided for recipe registration");
            return false;
        }

        String cookingBlockId = AbstractNamelessDishItem.getCookingBlockId(namelessDish);
        if (cookingBlockId == null) {
            LOGGER.error("NamelessDish has no cooking block information");
            return false;
        }

        INamelessDishRecipeRegister register = findRegister(cookingBlockId);
        if (register == null) {
            LOGGER.warn("No suitable recipe register found for cooking block: {}", cookingBlockId);
            return false;
        }

        // 生成最终的配方ID
        ResourceLocation finalRecipeId = recipeId != null ? recipeId : generateRecipeId(namelessDish);

        // 检查是否已注册
        String recipeKey = finalRecipeId.toString();
        if (registeredRecipeIds.contains(recipeKey)) {
            LOGGER.debug("Recipe {} already registered, skipping", finalRecipeId);
            return true; // 视为成功，避免重复注册
        }

        LOGGER.debug("Registering nameless dish {} using {}",
                ForgeRegistries.ITEMS.getKey(namelessDish.getItem()),
                register.getName());

        // 1. 创建配方对象
        Recipe<?> recipe = register.createRecipeFromNamelessDish(namelessDish, finalRecipeId);
        if (recipe == null) {
            LOGGER.error("Failed to create recipe from nameless dish");
            return false;
        }

        // 2. 注册配方到游戏
        try {
            register.registerToGame(recipe);
            registeredRecipeIds.add(recipeKey);
            LOGGER.info("Successfully registered recipe: {}", finalRecipeId);

            // 3. 保存到文件系统
            if (saveToFile) {
                saveRecipeToFile(namelessDish, finalRecipeId);
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to register recipe to game: {}", finalRecipeId, e);
            return false;
        }
    }

    /**
     * 注册并保存配方到文件
     */
    public void registerAndSaveRecipe(ItemStack namelessDish, @Nullable ResourceLocation recipeId) {
        if (registerRecipe(namelessDish, recipeId, true)) {
            if (recipeId == null) {
                generateRecipeId(namelessDish);
            }
        }
    }

    /**
     * 批量注册无名料理配方（可控制是否保存到文件）
     */
    public int batchRegisterRecipes(Iterable<ItemStack> namelessDishes, boolean saveToFile) {
        int successCount = 0;
        int totalCount = 0;

        for (ItemStack dish : namelessDishes) {
            totalCount++;
            if (registerRecipe(dish, null, saveToFile)) {
                successCount++;
            }
        }

        LOGGER.info("Batch registration completed: {}/{} recipes registered successfully",
                successCount, totalCount);
        return successCount;
    }

    /**
     * 保存配方到文件
     */
    private void saveRecipeToFile(ItemStack namelessDish, ResourceLocation recipeId) {
        try {
            NamelessDishRecipeData recipeData = NamelessDishRecipeData.fromItemStack(
                    namelessDish,
                    recipeId.toString()
            );

            boolean saved = storageManager.saveRecipe(recipeData);

            if (saved) {
                LOGGER.debug("Successfully saved recipe {} to file", recipeId);
            } else {
                LOGGER.warn("Failed to save recipe {} to file", recipeId);
            }
        } catch (Exception e) {
            LOGGER.error("Error saving recipe to file: {}", recipeId, e);
        }
    }

    /**
     * 加载并注册已保存的配方
     */
    public void loadAndRegisterSavedRecipes() {
        LOGGER.info("Loading saved recipes from storage...");

        // 加载所有保存的配方
        Map<String, List<NamelessDishRecipeData>> allRecipes = storageManager.loadAllRecipes();
        int totalLoaded = 0;
        int totalRegistered = 0;

        for (Map.Entry<String, List<NamelessDishRecipeData>> entry : allRecipes.entrySet()) {
            String cookingBlockId = entry.getKey();
            List<NamelessDishRecipeData> recipes = entry.getValue();

            LOGGER.info("Found {} recipes for cooking block: {}", recipes.size(), cookingBlockId);
            totalLoaded += recipes.size();

            // 获取对应的注册器
            INamelessDishRecipeRegister register = findRegister(cookingBlockId);
            if (register == null) {
                LOGGER.warn("No register found for cooking block: {}, skipping {} recipes",
                        cookingBlockId, recipes.size());
                continue;
            }

            // 注册每个配方
            for (NamelessDishRecipeData recipeData : recipes) {
                if (registerRecipeFromData(recipeData, register)) {
                    totalRegistered++;
                }
            }
        }

        LOGGER.info("Recipe loading completed: {} loaded, {} registered", totalLoaded, totalRegistered);
    }

    /**
     * 从配方数据注册配方
     */
    private boolean registerRecipeFromData(NamelessDishRecipeData recipeData,
                                           INamelessDishRecipeRegister register) {
        try {
            String recipeId = recipeData.getRecipeId();

            // 检查是否已注册
            if (registeredRecipeIds.contains(recipeId)) {
                LOGGER.debug("Recipe {} already registered, skipping", recipeId);
                return true;
            }

            // 根据配方数据创建ItemStack
            ItemStack dishStack = createDishStackFromData(recipeData);
            if (dishStack.isEmpty()) {
                LOGGER.error("Failed to create dish stack from recipe: {}", recipeId);
                return false;
            }

            // 使用注册器创建并注册配方
            @SuppressWarnings("all")
            ResourceLocation finalRecipeId = new ResourceLocation("nameless_dishes", recipeId);

            // 1. 创建配方对象
            Recipe<?> recipe = register.createRecipeFromNamelessDish(dishStack, finalRecipeId);
            if (recipe == null) {
                LOGGER.error("Failed to create recipe from data: {}", recipeId);
                return false;
            }

            // 2. 注册到游戏
            register.registerToGame(recipe);

            // 添加到已注册集合
            registeredRecipeIds.add(recipeId);
            LOGGER.info("Successfully registered recipe from file: {}", recipeId);
            return true;

        } catch (Exception e) {
            LOGGER.error("Error registering recipe from data: {}", recipeData.getRecipeId(), e);
            return false;
        }
    }

    /**
     * 从配方数据创建ItemStack
     */
    @SuppressWarnings("all")
    private ItemStack createDishStackFromData(NamelessDishRecipeData recipeData) {
        try {
            // 获取原料列表
            List<ItemStack> ingredients = new ArrayList<>();
            for (NamelessDishRecipeData.IngredientData ingredientData : recipeData.getIngredients()) {
                ItemStack ingredientStack = ingredientData.createItemStack();
                if (!ingredientStack.isEmpty()) {
                    ingredients.add(ingredientStack);
                }
            }

            if (ingredients.isEmpty()) {
                LOGGER.error("No valid ingredients found for recipe: {}", recipeData.getRecipeId());
                return ItemStack.EMPTY;
            }
            return FoodUtil.createNamelessResult(recipeData.getCookingBlockId(),ingredients,recipeData.isWithBowl());


        } catch (Exception e) {
            LOGGER.error("Failed to create dish stack from recipe data", e);
            return ItemStack.EMPTY;
        }
    }

    /**
     * 处理待注册的配方队列
     */
    private void processPendingRegistrations() {
        LOGGER.debug("Processing {} pending recipe registrations", pendingRegistrations.size());

        int processed = 0;
        int succeeded = 0;

        while (!pendingRegistrations.isEmpty()) {
            RecipeRegistrationTask task = pendingRegistrations.poll();
            processed++;

            if (processRecipeRegistration(task.namelessDish, task.recipeId, task.saveToFile)) {
                succeeded++;
            }
        }

        LOGGER.debug("Processed {} pending registrations, {} succeeded",
                processed, succeeded);
    }

    /**
     * 生成配方ID
     */
    @SuppressWarnings("all")
    private ResourceLocation generateRecipeId(ItemStack namelessDish) {
        List<ItemStack> ingredients = AbstractNamelessDishItem.getIngredients(namelessDish);
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
        String cookingBlockId = AbstractNamelessDishItem.getCookingBlockId(namelessDish);
        if (cookingBlockId != null) {
            hashBuilder.append(cookingBlockId.replace(":", "_"));
        }

        // 添加容器信息
        if (AbstractNamelessDishItem.hasBowl(namelessDish)) {
            hashBuilder.append("_bowl");
        }

        // 生成UUID并取前8位
        String hash = UUID.nameUUIDFromBytes(hashBuilder.toString().getBytes()).toString()
                .replace("-", "")
                .substring(0, 8);

        return new ResourceLocation(NamelessDishes.MOD_ID, "autogen_" + hash);
    }

    /**
     * 从文件删除配方
     */
    public boolean deleteRecipeFromStorage(String cookingBlockId, String recipeId) {
        boolean deleted = storageManager.deleteRecipe(cookingBlockId, recipeId);

        if (deleted) {
            registeredRecipeIds.remove(recipeId);
            LOGGER.info("Deleted recipe from storage: {}", recipeId);
        }

        return deleted;
    }
    /**
     * 获取存储管理器（用于高级操作）
     */
    public RecipeStorageManager getStorageManager() {
        return storageManager;
    }
    /**
     * 清空所有已注册的配方（用于重新加载）
     */
    public void clearAllRegistrations() {
        synchronized (this) {
            registeredRecipeIds.clear();
            pendingRegistrations.clear();
            LOGGER.info("Cleared all recipe registrations");
        }
    }

    /**
     * 重新加载所有配方
     */
    public void reloadAllRecipes() {
        LOGGER.info("Reloading all recipes...");

        clearAllRegistrations();
        storageManager.clearCache();

        // 重新加载并注册
        loadAndRegisterSavedRecipes();

        LOGGER.info("Recipe reload completed");
    }

}