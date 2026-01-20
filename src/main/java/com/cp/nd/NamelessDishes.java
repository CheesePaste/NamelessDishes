package com.cp.nd;

import com.cp.nd.config.NDConfig;
import com.cp.nd.item.ModItems;
import com.cp.nd.recipe.RecipeRegisterManager;
import com.cp.nd.test.IntegrationTest;
import com.cp.nd.test.ItemTest;
import com.cp.nd.test.NBTStackingBugTest;
import com.cp.nd.test.RecipeTest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterGameTestsEvent;
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

    //这里有两个get从1.21.1开始弃用，1.20.1可以正常使用，但是因为警告每次运行都会拉到这个类，所以我先SuppressWarning了
    @SuppressWarnings("all")
    public NamelessDishes() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, NDConfig.SPEC, "namelessdishes-common.toml");

        // 注册事件
        modEventBus.addListener(this::commonSetup);

        // 注册GameTest
        modEventBus.addListener(this::registerGameTests);

        // 注册Forge事件总线
        MinecraftForge.EVENT_BUS.register(this);

        ModItems.register(modEventBus);


    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 获取注册管理器
        RecipeRegisterManager manager = RecipeRegisterManager.getInstance();

        // 初始化管理器
        manager.initialize(event);
        LOGGER.info("Nameless Dishes 框架初始化...");
    }

    /**
     * Register GameTest classes
     */
    @SuppressWarnings("all")
    private void registerGameTests(RegisterGameTestsEvent event) {
        event.register(RecipeTest.class);
        event.register(ItemTest.class);
        event.register(IntegrationTest.class);
        event.register(NBTStackingBugTest.class);  // NBT堆叠Bug修复验证测试
        LOGGER.info("GameTests registered");
    }

}