package com.cp.nd.compatibility;

import com.cp.nd.NamelessDishes;
import com.cp.nd.api.ICookingRecipeHandler;
import com.cp.nd.compatibility.base.BaseCookingHandler;
import com.cp.nd.config.CompatibilityConfig;
import com.cp.nd.util.ModDetectionUtil;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModCompatibilityManager {
    private static ModCompatibilityManager instance;

    private final Map<String, ICookingRecipeHandler> activeHandlers = new ConcurrentHashMap<>();
    private final Map<String, Boolean> modStatus = new ConcurrentHashMap<>();
    private final BaseCookingHandler baseHandler = new BaseCookingHandler();

    private ModCompatibilityManager() {}

    public static ModCompatibilityManager getInstance() {
        if (instance == null) {
            instance = new ModCompatibilityManager();
        }
        return instance;
    }

    public void initialize() {
        NamelessDishes.LOGGER.info("初始化模组兼容性管理器...");

        // 清空现有处理器
        activeHandlers.clear();
        modStatus.clear();

        // 获取兼容的模组配置
        Map<String, CompatibilityConfig.ModCompatibility> compatibleMods =
                CompatibilityConfig.getCompatibleMods();

        // 初始化每个兼容的模组
        for (CompatibilityConfig.ModCompatibility compat : compatibleMods.values()) {
            if (compat.isEnabled()) {
                initializeHandler(compat);
            }
        }

        // 记录状态
        logStatus();
    }

    private void initializeHandler(CompatibilityConfig.ModCompatibility compat) {
        try {
            if (compat.getHandlerClass() != null) {
                ICookingRecipeHandler handler =
                        (ICookingRecipeHandler) compat.getHandlerClass().getDeclaredConstructor().newInstance();
                activeHandlers.put(compat.getModId(), handler);
                modStatus.put(compat.getModId(), true);
                NamelessDishes.LOGGER.info("已启用 {} 的兼容处理器", compat.getModId());
            }
        } catch (InstantiationException | IllegalAccessException e) {
            NamelessDishes.LOGGER.error("无法实例化处理器: {}", compat.getHandlerClass(), e);
            modStatus.put(compat.getModId(), false);
        } catch (InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public ICookingRecipeHandler getHandlerForBlock(BlockEntity blockEntity) {
        if (blockEntity == null) return null;

        // 首先检查特定的模组处理器
        for (ICookingRecipeHandler handler : activeHandlers.values()) {
            if (handler.isSupportedBlock(blockEntity)) {
                return handler;
            }
        }

        // 如果没有匹配的处理器，使用基础处理器
        return baseHandler;
    }

    public ICookingRecipeHandler getHandlerForMod(String modId) {
        return activeHandlers.get(modId);
    }

    public boolean isModActive(String modId) {
        return Boolean.TRUE.equals(modStatus.get(modId));
    }

    public Set<String> getActiveMods() {
        return modStatus.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }

    public Set<String> getDetectedMods() {
        return ModDetectionUtil.getLoadedCookingMods();
    }

    public Set<String> getAllRegisteredMods() {
        return new HashSet<>(activeHandlers.keySet());
    }

    public void registerHandler(String modId, ICookingRecipeHandler handler) {
        if (handler != null) {
            activeHandlers.put(modId, handler);
            modStatus.put(modId, true);
            NamelessDishes.LOGGER.info("已手动注册处理器: {}", modId);
        }
    }

    public void reload() {
        CompatibilityConfig.reload();
        initialize();
    }

    private void logStatus() {
        NamelessDishes.LOGGER.info("=== 模组兼容性状态 ===");
        NamelessDishes.LOGGER.info("激活的处理器: {}", activeHandlers.keySet());
        NamelessDishes.LOGGER.info("检测到的烹饪模组: {}", getDetectedMods());
        NamelessDishes.LOGGER.info("======================");
    }
}