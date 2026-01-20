package com.cp.nd.recipe;

import com.cp.nd.api.INamelessDishRecipeRegister;
import com.cp.nd.item.AbstractNamelessDishItem;
import com.cp.nd.recipe.fd.CookingPotRecipeRegister;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 无名料理配方注册管理器
 * 负责管理和分发配方注册任务
 */
public class RecipeRegisterManager {
    private static final Logger LOGGER = LogManager.getLogger(RecipeRegisterManager.class);

    // 单例实例
    private static final RecipeRegisterManager INSTANCE = new RecipeRegisterManager();

    // 注册器映射
    private final Map<String, INamelessDishRecipeRegister> registerByBlockId = new ConcurrentHashMap<>();
    private final List<INamelessDishRecipeRegister> dynamicRegisters = new ArrayList<>();

    private RecipeRegisterManager() {
        // 注册默认的注册器
        registerDefaultRegisters();
    }

    public static RecipeRegisterManager getInstance() {
        return INSTANCE;
    }

    /**
     * 注册默认的注册器
     */
    private void registerDefaultRegisters() {
        registerRecipeRegister(new CookingPotRecipeRegister());
    }

    /**
     * 注册新的配方注册器
     */
    public void registerRecipeRegister(INamelessDishRecipeRegister register) {
            for (String blockId : register.getSupportedBlocks()) {
                registerByBlockId.put(blockId, register);
                LOGGER.debug("Registered {} for block: {}", register.getName(), blockId);
            }
    }



    /// 这里便于其他模组兼容，不用硬编码到类里面
    public void registerRecipeRegisterDynamic(INamelessDishRecipeRegister register) {
            // 动态注册器，在注册时检查支持性
            dynamicRegisters.add(register);
            LOGGER.debug("Registered dynamic register: {}", register.getName());
        for (String blockId : register.getSupportedBlocks()) {
            registerByBlockId.put(blockId, register);
            LOGGER.debug("Dynamic Registered {} for block: {}", register.getName(), blockId);
        }
    }

    /**
     * 根据料理方块ID查找注册器
     */
    @Nullable
    public INamelessDishRecipeRegister findRegister(String cookingBlockId) {
        return registerByBlockId.get(cookingBlockId);
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
            LOGGER.warn("Item {} is not a NamelessDish", ForgeRegistries.ITEMS.getKey(stack.getItem()));
            return false;
        }

        if (stack.getTag() != null && (!stack.hasTag() || !stack.getTag().contains(AbstractNamelessDishItem.INGREDIENTS_KEY))) {
            LOGGER.warn("NamelessDish {} is missing required NBT data", ForgeRegistries.ITEMS.getKey(stack.getItem()));
            return false;
        }

        if (AbstractNamelessDishItem.getIngredients(stack).isEmpty()) {
            LOGGER.warn("NamelessDish {} has no ingredients", ForgeRegistries.ITEMS.getKey(stack.getItem()));
            return false;
        }

        return true;
    }

    /**
     * 注册无名料理到对应的配方系统
     */
    public boolean registerRecipe(ItemStack namelessDish, @Nullable ResourceLocation recipeId) {
        if (!isValidNamelessDish(namelessDish)) {
            LOGGER.error("Invalid nameless dish provided for recipe registration");
            return false;
        }

        String cookingBlockId = AbstractNamelessDishItem.getCookingBlockId(namelessDish);
        INamelessDishRecipeRegister register = findRegister(cookingBlockId);

        if (register == null) {
            LOGGER.warn("No suitable recipe register found for cooking block: {}", cookingBlockId);
            return false;
        }

        LOGGER.debug("Registering nameless dish {} using {}",
                ForgeRegistries.ITEMS.getKey(namelessDish.getItem()),
                register.getName());

        return register.register(namelessDish, recipeId);
    }

    /**
     * 批量注册无名料理配方
     */
    public int batchRegisterRecipes(Iterable<ItemStack> namelessDishes) {
        int successCount = 0;
        int totalCount = 0;

        for (ItemStack dish : namelessDishes) {
            totalCount++;
            if (registerRecipe(dish, null)) {
                successCount++;
            }
        }

        LOGGER.info("Batch registration completed: {}/{} recipes registered successfully",
                successCount, totalCount);
        return successCount;
    }

    /**
     * 获取所有已注册的注册器信息（用于调试）
     */
    public Map<String, String> getRegisterInfo() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("Static registers count", String.valueOf(registerByBlockId.size()));
        info.put("Dynamic registers count", String.valueOf(dynamicRegisters.size()));

        for (Map.Entry<String, INamelessDishRecipeRegister> entry : registerByBlockId.entrySet()) {
            info.put("Block: " + entry.getKey(), "Register: " + entry.getValue().getName());
        }

        for (INamelessDishRecipeRegister register : dynamicRegisters) {
            info.put("Dynamic register", register.getName());
        }

        return info;
    }
}