package com.cp.nd.util;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.*;
import java.util.stream.Collectors;

//这个类完全由AI生成，没有人为检验过代码是否符合预期
public class ModDetectionUtil {
    private static final Map<String, Boolean> modCache = new HashMap<>();
    private static final Set<String> cookingMods = new HashSet<>(Arrays.asList(
            "farmersdelight"        // 农夫乐事
    ));

    public static boolean isModLoaded(String modId) {
        return modCache.computeIfAbsent(modId, id -> ModList.get().isLoaded(id));
    }

    public static Set<String> getLoadedCookingMods() {
        return cookingMods.stream()
                .filter(ModDetectionUtil::isModLoaded)
                .collect(Collectors.toSet());
    }

    public static List<IModInfo> getAllLoadedMods() {
        return ModList.get().getMods();
    }

    public static String getModName(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId);
    }

    public static String getModVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("Unknown");
    }

    public static boolean hasCookingBlocks(String modId) {
        // 检查模组是否包含烹饪方块
        // 这里可以扩展为更复杂的检测逻辑
        return cookingMods.contains(modId);
    }
}