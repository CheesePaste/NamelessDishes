package com.cp.nd.config;

import com.cp.nd.NamelessDishes;
import com.cp.nd.util.ModDetectionUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


//这个类完全由AI生成，没有人为检验过代码是否符合预期
public class CompatibilityConfig {
    private static final Map<String, ModCompatibility> modCompatibilityMap = new HashMap<>();

    public static class ModCompatibility {
        private final String modId;
        private final boolean enabled;
        private final Set<String> cookingBlocks;
        private final Class<?> handlerClass;

        public ModCompatibility(String modId, boolean enabled, Set<String> cookingBlocks, Class<?> handlerClass) {
            this.modId = modId;
            this.enabled = enabled;
            this.cookingBlocks = cookingBlocks;
            this.handlerClass = handlerClass;
        }

        public String getModId() { return modId; }
        public boolean isEnabled() { return enabled; }
        public Set<String> getCookingBlocks() { return cookingBlocks; }
        public Class<?> getHandlerClass() { return handlerClass; }
    }

    static {
        // 注册已知的烹饪模组
        registerMod("farmersdelight", "com.cp.nd.compatibility.fd.FarmersDelightHandler");
    }

    private static void registerMod(String modId, String handlerClass) {
        boolean isModLoaded = ModDetectionUtil.isModLoaded(modId);
        boolean isEnabled = NDConfig.INSTANCE.enabledMods.get().contains(modId) && isModLoaded;

        Set<String> blocks = getBlocksForMod(modId);

        modCompatibilityMap.put(modId, new ModCompatibility(
                modId,
                isEnabled,
                blocks,
                getHandlerClass(handlerClass)
        ));
    }

    private static Set<String> getBlocksForMod(String modId) {
        return switch (modId) {
            case "farmersdelight" -> Set.copyOf(NDConfig.INSTANCE.farmersDelightBlocks.get());
            default -> Set.of();
        };
    }

    private static Class<?> getHandlerClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            NamelessDishes.LOGGER.warn("无法找到处理器类: {}", className);
            return null;
        }
    }

    public static Map<String, ModCompatibility> getCompatibleMods() {
        return new HashMap<>(modCompatibilityMap);
    }

    public static boolean isModCompatible(String modId) {
        ModCompatibility compat = modCompatibilityMap.get(modId);
        return compat != null && compat.isEnabled();
    }

    public static void reload() {
        modCompatibilityMap.clear();
        // 重新注册
        registerMod("farmersdelight", "com.cp.nd.compatibility.fd.FarmersDelightHandler");
    }
}