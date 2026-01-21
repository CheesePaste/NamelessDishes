package com.cp.nd.event;

import com.cp.nd.recipe.RecipeRegisterManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "your_mod_id", value = Dist.CLIENT)
public class ClientEventHandler {

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        // 当客户端玩家登录到世界时调用
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            RecipeRegisterManager.getInstance().OnClientPlayerEnter();
        }
    }
}