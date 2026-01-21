package com.cp.nd.util;

import com.cp.nd.NamelessDishes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class NDJeiPlugin implements IModPlugin {

    public static IJeiRuntime jeiRuntime;

    @Override
    @SuppressWarnings("all")
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(NamelessDishes.MOD_ID, "jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime runtime) {
        jeiRuntime = runtime;
    }

    @Override
    public void onRuntimeUnavailable() {
        // JEI运行时不可用时（如玩家关闭了JEI界面）
        jeiRuntime = null;
    }
}