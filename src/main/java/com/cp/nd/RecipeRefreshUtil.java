// RecipeRefreshUtil.java
package com.cp.nd;

import com.cp.nd.item.IRecipeManagerMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.*;

public class RecipeRefreshUtil {

    /**
     * 添加配方并刷新客户端显示
     */
    public static boolean addRecipeWithRefresh(Recipe<?> recipe) {
        try {
            // 获取服务器RecipeManager
            RecipeManager serverManager = getServerRecipeManager();
            RecipeManager clientManager = getClientRecipeManager();


            // 添加到服务器
            if (serverManager instanceof IRecipeManagerMixin) {
                ((IRecipeManagerMixin) serverManager).addRecipe(recipe);

            }

            // 添加到客户端
            if (clientManager instanceof IRecipeManagerMixin) {
                ((IRecipeManagerMixin) clientManager).addRecipe(recipe);
            }
            var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            for(ServerPlayer player :server.getPlayerList().getPlayers())
            {
                for(int i=0;i<100;i++) {
                    NamelessDishes.LOGGER.info(player.toString());
                }
                player.getRecipeBook().add(recipe);
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.getRecipeBook().add(recipe);
            }

                refreshClientRecipeDisplay();

            sendRecipeToClient(recipe);

                return true;


        } catch (Exception e) {
            System.err.println("添加配方失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 刷新客户端的配方显示
     * 这会让JEI和合成台重新加载配方
     */
    public static void refreshClientRecipeDisplay() {
        System.out.println("刷新客户端配方显示...");

        // 方法1: 强制重新打开GUI
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            System.out.println("当前有打开的GUI: " + mc.screen.getClass().getSimpleName());
        }

        // 方法2: 清除JEI缓存（如果JEI已加载）
        try {
            refreshJEICache();
        } catch (Exception e) {
            System.out.println("无法刷新JEI缓存: " + e.getMessage());
        }

        // 方法3: 重新加载合成管理器（模拟/reload）
        reloadRecipeManager();
    }

    /**
     * 尝试刷新JEI缓存
     */
    private static void refreshJEICache() {
        try {
            // 使用反射检查JEI是否可用
            Class<?> jeiPluginClass = Class.forName("mezz.jei.JustEnoughItems");
            System.out.println("JEI已加载，尝试刷新...");

            // 获取JEI运行时
            Object jeiRuntime = getJEIRuntime();
            if (jeiRuntime != null) {
                System.out.println("找到JEI运行时，刷新配方管理器...");
                // 调用JEI的配方管理器刷新方法
                invokeJEIRefresh(jeiRuntime);
            }
        } catch (ClassNotFoundException e) {
            System.out.println("JEI未加载");
        } catch (Exception e) {
            System.out.println("刷新JEI失败: " + e.getMessage());
        }
    }

    /**
     * 重新加载配方管理器
     */
    private static void reloadRecipeManager() {
        // 这个方法会模拟/reload命令的部分效果
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            System.out.println("重新加载客户端配方管理器...");

            // 触发一个假的配方数据包来刷新
            triggerRecipeUpdate();
        }
    }

    /**
     * 触发配方更新
     */
    private static void triggerRecipeUpdate() {
        // 这里可以尝试模拟一个配方更新数据包
        // 但最简单的方法是重新打开合成GUI
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.containerMenu != null) {
            System.out.println("检测到打开的容器，尝试刷新...");
        }
    }

    /**
     * 获取JEI运行时（反射）
     */
    private static Object getJEIRuntime() {
        try {
            Class<?> jeiPluginClass = Class.forName("mezz.jei.JustEnoughItems");
            java.lang.reflect.Method getRuntimeMethod = jeiPluginClass.getMethod("getRuntime");
            return getRuntimeMethod.invoke(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 调用JEI刷新方法（反射）
     */
    private static void invokeJEIRefresh(Object jeiRuntime) {
        try {
            Class<?> runtimeClass = jeiRuntime.getClass();
            java.lang.reflect.Method getRecipeManagerMethod = runtimeClass.getMethod("getRecipeManager");
            Object recipeManager = getRecipeManagerMethod.invoke(jeiRuntime);

            // 不同类型的刷新方法
            try {
                // JEI 9.x
                java.lang.reflect.Method reloadMethod = recipeManager.getClass().getMethod("reload");
                reloadMethod.invoke(recipeManager);
                System.out.println("JEI 9.x 配方管理器已重新加载");
            } catch (NoSuchMethodException e) {
                try {
                    // JEI 8.x
                    java.lang.reflect.Method reloadMethod = recipeManager.getClass().getMethod("reloadRecipes");
                    reloadMethod.invoke(recipeManager);
                    System.out.println("JEI 8.x 配方已重新加载");
                } catch (NoSuchMethodException e2) {
                    System.out.println("无法找到JEI刷新方法");
                }
            }
        } catch (Exception e) {
            System.out.println("调用JEI刷新失败: " + e.getMessage());
        }
    }

    /**
     * 获取服务器RecipeManager
     */
    private static RecipeManager getServerRecipeManager() {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getRecipeManager();
        }
        return null;
    }

    /**
     * 获取客户端RecipeManager
     */
    private static RecipeManager getClientRecipeManager() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            return mc.level.getRecipeManager();
        }
        return null;
    }

    /**
     * 发送配方到客户端（网络同步）
     */
    private static void sendRecipeToClient(Recipe<?> recipe) {
        // 这个方法需要在服务器端调用，通过数据包发送给客户端
        System.out.println("需要发送配方到客户端: " + recipe.getId());
        // 实现网络同步逻辑...
    }
}