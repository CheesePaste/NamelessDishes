package com.cp.nd.recipe;

import com.cp.nd.NamelessDishes;
import com.cp.nd.api.INamelessDishRecipeRegister;
import com.cp.nd.item.AbstractNamelessDishItem;
import com.cp.nd.recipe.fd.CookingPotRecipeRegister;
import com.cp.nd.recipe.storage.NamelessDishRecipeData;
import com.cp.nd.recipe.storage.RecipeStorageManager;
import com.cp.nd.util.FoodUtil;
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

import static com.cp.nd.util.FoodUtil.createNamelessResult;
import static com.cp.nd.util.FoodUtil.isValidNamelessDish;

/**
 * 无名料理配方注册管理器
 * 负责管理和分发配方注册任务，并集成配方存储功能
 */
public class RecipeRegisterManager {
    static final Logger LOGGER = LogManager.getLogger(RecipeRegisterManager.class);

    // 单例实例
    private static final RecipeRegisterManager INSTANCE = new RecipeRegisterManager();

    // 注册器映射
    private final Map<String, INamelessDishRecipeRegister> registerByBlockId = new ConcurrentHashMap<>();
    private final List<INamelessDishRecipeRegister> dynamicRegisters = new ArrayList<>();

    // 配方存储管理器
    private final RecipeStorageManager storageManager;

    // 缓存已注册的配方ID，避免重复注册
    private final Set<String> registeredRecipeIds = ConcurrentHashMap.newKeySet();


    // 控制变量
    private boolean isInitialized = false;


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
    @SuppressWarnings("all")
    public void initialize(FMLCommonSetupEvent event) {
        if(isInitialized)return;
        Map<String, List<NamelessDishRecipeData>> recipedatas=storageManager.loadAllRecipes();
        for(List<NamelessDishRecipeData> list:recipedatas.values())
        {
            if(list==null||list.isEmpty())
            {
                continue;
            }

            for(NamelessDishRecipeData data : list)
            {
                ItemStack namelessDish= createDishStackFromData(data);
                if(!processRecipeRegistration(namelessDish,new ResourceLocation(data.getRecipeId())))
                {
                    LOGGER.error("配方生成失败",data.getRecipeId(),data.getCookingBlockId());
                }

            }
        }
        isInitialized=true;
    }

    public void OnClientPlayerEnter()
    {
        if(!registerToGame())
        {
            LOGGER.error("配方注册进游戏失败");
        }
    }
    public List<INamelessDishRecipeRegister> getRegisters()
    {
        List<INamelessDishRecipeRegister> registers=dynamicRegisters;
        for(INamelessDishRecipeRegister r : registerByBlockId.values())
        {
            if(!registers.contains(r))
            {
                registers.add(r);
            }
        }
        return registers;
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
     * 处理单个配方注册
     * 注意：这里只处理配方注册，需要等世界加载完成即玩家进入游戏后才能调用registerToGame注册未注册配方
     */
    private boolean processRecipeRegistration(ItemStack namelessDish, @Nullable ResourceLocation recipeId) {
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

        if (recipeId != null && registeredRecipeIds.contains(recipeId.toString())) {
            return true;
        }

        Recipe<?> recipe = register.createRecipeFromNamelessDish(namelessDish, finalRecipeId);
        if (recipe == null) {
            LOGGER.error("Failed to create recipe from nameless dish");
            return false;
        }
        register.addToSet(recipe);
        return  true;
    }

    /**
     * 将指定注册器的待注册配方注册进入游戏
     * @param register 指定配方注册器
     */
    public boolean registerToGame(INamelessDishRecipeRegister register)
    {
        try {
            register.registerToGame();
            return true;
        }
        catch (Exception e) {
        LOGGER.error("Failed to register recipe to game:",e);
        return false;
    }
    }

    /**
     * 将所有配方注册器的待注册配方注册进入游戏
     */
    public boolean registerToGame()
    {
        try {
            for (INamelessDishRecipeRegister register : registerByBlockId.values()) {
                register.registerToGame();
            }
            for (INamelessDishRecipeRegister register : dynamicRegisters) {
                register.registerToGame();
            }
            return true;
        }
        catch (Exception e) {
            LOGGER.error("Failed to register recipe to game:",e);
            return false;
        }

    }

    /**
     * 保存无名料理配方至文件，自定义配方名
     * @param namelessDish 无名料理
     * @param recipeId 配方名称
     */
    public void saveToFile(ItemStack namelessDish,ResourceLocation recipeId)
    {
        storageManager.saveRecipeToFile(namelessDish, recipeId);
    }
    /**
     * 保存无名料理配方至文件，使用自己生成的配方名
     * @param namelessDish 无名料理
     */
    public void saveToFile(ItemStack namelessDish)
    {
        ResourceLocation recipeId=generateRecipeId(namelessDish);
        storageManager.saveRecipeToFile(namelessDish, recipeId);
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

        // 生成UUID并取前8位
        String hash = UUID.nameUUIDFromBytes(hashBuilder.toString().getBytes()).toString()
                .replace("-", "")
                .substring(0, 8);

        return new ResourceLocation(NamelessDishes.MOD_ID, "autogen_" + hash);
    }

    /**
     * 从配方数据创建ItemStack
     */
    @SuppressWarnings("all")
    public static ItemStack createDishStackFromData(NamelessDishRecipeData recipeData) {
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
            return createNamelessResult(recipeData.getCookingBlockId(),ingredients,recipeData.getContainerId());


        } catch (Exception e) {
            LOGGER.error("Failed to create dish stack from recipe data", e);
            return ItemStack.EMPTY;
        }
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

}