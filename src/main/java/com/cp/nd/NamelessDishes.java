package com.cp.nd;

import com.cp.nd.config.NDConfig;
import com.cp.nd.compatibility.ModCompatibilityManager;
import com.cp.nd.item.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(NamelessDishes.MOD_ID)
public class NamelessDishes {
    public static final String MOD_ID = "nameless_dishes";
    public static final Logger LOGGER = LogManager.getLogger();

    private static ModCompatibilityManager compatibilityManager;

    public NamelessDishes() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, NDConfig.SPEC, "namelessdishes-common.toml");

        // 注册事件
        modEventBus.addListener(this::commonSetup);

        // 注册Forge事件总线
        MinecraftForge.EVENT_BUS.register(this);

        ModItems.register(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Nameless Dishes 框架初始化...");

        // 初始化兼容性管理器
        compatibilityManager = ModCompatibilityManager.getInstance();
        compatibilityManager.initialize();

        LOGGER.info("检测到兼容模组: {}", compatibilityManager.getDetectedMods());
    }

    public static ModCompatibilityManager getCompatibilityManager() {
        return compatibilityManager;
    }
}