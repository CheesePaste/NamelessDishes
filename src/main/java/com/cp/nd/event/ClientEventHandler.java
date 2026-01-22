package com.cp.nd.event;

import com.cp.nd.NamelessDishes;
import com.cp.nd.api.INamelessDishRecipeRegister;
import com.cp.nd.recipe.RecipeRegisterManager;
import com.cp.nd.recipe.RecipeUnlockManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NamelessDishes.MOD_ID, value = Dist.CLIENT)
public class ClientEventHandler {

    private static int tickCounter = 0;
    private static boolean hasProcessed = false;

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // 重置标记，允许在新登录时处理
        hasProcessed = false;
        tickCounter = 0;
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (Minecraft.getInstance().player != null && !hasProcessed) {
            tickCounter++;

            // 等待几个tick确保配方书已加载
            if (tickCounter >= 10) {
                for(INamelessDishRecipeRegister register : RecipeRegisterManager.getInstance().getRegisters()) {
                    RecipeUnlockManager.manager.HideDefaultRecipe(register.getStandardRecipe().getType());
                }
                RecipeRegisterManager.getInstance().OnClientPlayerEnter();
                hasProcessed = true;
            }
        }
    }
}