package com.cp.nd.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NDConfig {
    public static final ForgeConfigSpec SPEC;
    public static final NDConfig INSTANCE;

    static {
        Pair<NDConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(NDConfig::new);
        SPEC = specPair.getRight();
        INSTANCE = specPair.getLeft();
    }

    // 通用设置
    public final ForgeConfigSpec.BooleanValue enableFramework;
    public final ForgeConfigSpec.BooleanValue debugMode;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> enabledMods;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> cookingBlocks;

    // 无名料理设置
    public final ForgeConfigSpec.DoubleValue baseSaturationMultiplier;
    public final ForgeConfigSpec.IntValue baseHungerMultiplier;
    public final ForgeConfigSpec.BooleanValue requireCooking;
    public final ForgeConfigSpec.IntValue minIngredients;
    public final ForgeConfigSpec.IntValue maxIngredients;

    // 模组特定设置
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> farmersDelightBlocks;

    private NDConfig(ForgeConfigSpec.Builder builder) {
        builder.push("general");
        enableFramework = builder
                .comment("启用无名料理框架")
                .define("enableFramework", true);
        debugMode = builder
                .comment("启用调试模式")
                .define("debugMode", false);
        enabledMods = builder
                .comment("启用的模组兼容列表")
                .defineList("enabledMods",
                        Arrays.asList("farmersdelight", "cookingforblockheads"),
                        obj -> obj instanceof String);
        cookingBlocks = builder
                .comment("通用烹饪方块列表（格式：modid:blockid）")
                .defineList("cookingBlocks",
                        new ArrayList<>(),
                        obj -> obj instanceof String);
        builder.pop();

        builder.push("nameless_dishes");
        baseSaturationMultiplier = builder
                .comment("基础饱食度饱和度乘数")
                .defineInRange("baseSaturationMultiplier", 0.6, 0.1, 2.0);
        baseHungerMultiplier = builder
                .comment("基础饥饿值乘数")
                .defineInRange("baseHungerMultiplier", 80, 10, 200);
        requireCooking = builder
                .comment("是否需要烹饪（否则可以合成）")
                .define("requireCooking", true);
        minIngredients = builder
                .comment("最少食材数量")
                .defineInRange("minIngredients", 1, 1, 9);
        maxIngredients = builder
                .comment("最多食材数量")
                .defineInRange("maxIngredients", 9, 1, 9);
        builder.pop();

        builder.push("mod_specific");
        builder.push("farmersdelight");
        farmersDelightBlocks = builder
                .comment("农夫乐事兼容的烹饪方块")
                .defineList("farmersDelightBlocks",
                        Arrays.asList("farmersdelight:cooking_pot", "farmersdelight:skillet"),
                        obj -> obj instanceof String);
        builder.pop();
        builder.pop();
    }

}