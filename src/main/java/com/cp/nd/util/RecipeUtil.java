// RecipeUtil.java
package com.cp.nd.util;

import com.cp.nd.recipe.RecipeUnlockManager;
import com.cp.nd.recipe.fd.CookingPotRecipeRegister;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import com.cp.nd.recipe.IRecipeManagerMixin;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

import java.util.*;

public class RecipeUtil {

    /**
     * 操作模式枚举
     */
    private enum OperationMode {
        ADD,
        REMOVE,
        HIDE,
        SHOW
    }

    /**
     * 动态添加配方
     * @param recipe 要添加的配方对象
     * @return 是否添加成功
     */
    public static boolean addRecipe(Recipe<?> recipe) {
        return processRecipe(recipe, null, OperationMode.ADD);
    }

    /**
     * 动态添加配方
     * @param recipe 要添加的配方对象
     * @param type 配方在jei中对应类型
     * @return 是否添加成功
     */
    public static boolean addRecipe(Recipe<?> recipe, RecipeType<Object> type) {
        return processRecipe(recipe, type, OperationMode.ADD);
    }

    /**
     * 动态移除配方
     * @param recipeId 配方ID
     * @return 是否移除成功
     */
    public static boolean removeRecipe(ResourceLocation recipeId) {
        try {
            RecipeManager recipeManager = getClientRecipeManager();
            if (recipeManager == null) {
                return false;
            }

            if (recipeManager instanceof IRecipeManagerMixin) {
                return ((IRecipeManagerMixin) recipeManager).removeRecipe(recipeId);
            }

            return false;
        } catch (Exception e) {
            System.err.println("移除配方失败: " + recipeId + " - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查配方是否存在
     * @param recipeId 配方ID
     * @return 是否存在
     */
    public static boolean hasRecipe(ResourceLocation recipeId) {
        try {
            RecipeManager recipeManager = getClientRecipeManager();
            return recipeManager != null && recipeManager.byKey(recipeId).isPresent();
        } catch (Exception e) {
            System.err.println("检查配方失败: " + recipeId + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * 批量添加配方
     * @param recipes 要加入的配方
     */
    public static void addRecipes(Iterable<Recipe<?>> recipes) {
        processRecipes(recipes, null, OperationMode.ADD);
    }

    /**
     * 批量添加配方
     * @param recipes 要加入的配方
     * @param type JEI配方类型
     */
    public static void addRecipes(Iterable<Recipe<?>> recipes, RecipeType<Object> type) {
        processRecipes(recipes, type, OperationMode.ADD);
    }

    /**
     * 动态添加隐藏配方
     * @param recipe 要添加的配方对象
     * @return 是否添加成功
     */
    public static boolean addHideRecipe(Recipe<?> recipe) {
        return addRecipeToManagers(recipe);
    }

    /**
     * 批量添加隐藏配方
     * @param recipes 要加入的配方
     */
    public static void addHideRecipes(Iterable<Recipe<?>> recipes) {
        addRecipesToManagers(recipes);
    }

    /**
     * 显示配方
     * @param recipe 要显示的配方
     * @return 是否成功
     */
    public static boolean show(Recipe<?> recipe) {
        return processRecipe(recipe, null, OperationMode.SHOW);
    }

    /**
     * 显示配方
     * @param recipe 要显示的配方
     * @param type JEI配方类型
     * @return 是否成功
     */
    public static boolean show(Recipe<?> recipe, RecipeType<Object> type) {
        return processRecipe(recipe, type, OperationMode.SHOW);
    }

    /**
     * 批量显示配方
     * @param recipes 要显示的配方
     */
    public static void show(Iterable<Recipe<?>> recipes) {
        processRecipes(recipes, null, OperationMode.SHOW);
    }

    /**
     * 批量显示配方
     * @param recipes 要显示的配方
     * @param type JEI配方类型
     */
    public static void show(Iterable<Recipe<?>> recipes, RecipeType<Object> type) {
        processRecipes(recipes, type, OperationMode.SHOW);
    }

    /**
     * 隐藏配方
     * @param recipe 要隐藏的配方
     * @return 是否成功
     */
    public static boolean hide(Recipe<?> recipe) {
        return processRecipe(recipe, null, OperationMode.HIDE);
    }

    /**
     * 隐藏配方
     * @param recipe 要隐藏的配方
     * @param type JEI配方类型
     * @return 是否成功
     */
    public static boolean hide(Recipe<?> recipe, RecipeType<Object> type) {
        return processRecipe(recipe, type, OperationMode.HIDE);
    }

    /**
     * 批量隐藏配方
     * @param recipes 要隐藏的配方
     */
    public static void hide(Iterable<Recipe<?>> recipes) {
        processRecipes(recipes, null, OperationMode.HIDE);
    }

    /**
     * 批量隐藏配方
     * @param recipes 要隐藏的配方
     * @param type JEI配方类型
     */
    public static void hide(Iterable<Recipe<?>> recipes, RecipeType<Object> type) {
        processRecipes(recipes, type, OperationMode.HIDE);
    }

    /* ========== 私有方法 ========== */

    /**
     * 处理单个配方
     */
    private static boolean processRecipe(Recipe<?> recipe, RecipeType<Object> jeiType, OperationMode mode) {
        if (recipe == null) {
            return false;
        }

        try {
            switch (mode) {
                case ADD:
                    if (!addRecipeToManagers(recipe)) return false;
                    // 继续执行后面的逻辑
                case SHOW:
                    updatePlayerRecipeBook(recipe, true);
                    //updateJEI(recipe, jeiType, false);
                    return true;

                case HIDE:
                    updatePlayerRecipeBook(recipe, false);
                    //updateJEI(recipe, jeiType, true);
                    return true;

                default:
                    return false;
            }
        } catch (Exception e) {
            logError("处理配方失败", mode, e);
            return false;
        }
    }

    /**
     * 处理批量配方
     */
    private static void processRecipes(Iterable<Recipe<?>> recipes, RecipeType<Object> jeiType, OperationMode mode) {
        if (recipes == null || !recipes.iterator().hasNext()) {
            return;
        }

        try {
            List<Recipe<?>> recipeList = new ArrayList<>();
            List<Object> objectList = new ArrayList<>();

            for (Recipe<?> recipe : recipes) {
                recipeList.add(recipe);
                objectList.add(recipe);
            }

            switch (mode) {
                case ADD:
                    addRecipesToManagers(recipes);
                    // 继续执行后面的逻辑
                case SHOW:
                    updatePlayerRecipeBook(recipeList, true);
                    //updateJEI(objectList, jeiType, recipeList.get(0).getType(), false);
                    break;

                case HIDE:
                    updatePlayerRecipeBook(recipeList, false);
                    //updateJEI(objectList, jeiType, recipeList.get(0).getType(), true);
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            logError("处理批量配方失败", mode, e);
        }
    }

    /**
     * 添加配方到管理器
     */
    private static boolean addRecipeToManagers(Recipe<?> recipe) {
        try {
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

            return true;
        } catch (Exception e) {
            System.err.println("添加配方到管理器失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 批量添加配方到管理器
     */
    private static void addRecipesToManagers(Iterable<Recipe<?>> recipes) {
        try {
            RecipeManager serverManager = getServerRecipeManager();
            RecipeManager clientManager = getClientRecipeManager();

            for (Recipe<?> recipe : recipes) {
                if (serverManager instanceof IRecipeManagerMixin) {
                    ((IRecipeManagerMixin) serverManager).addRecipe(recipe);
                }

                if (clientManager instanceof IRecipeManagerMixin) {
                    ((IRecipeManagerMixin) clientManager).addRecipe(recipe);
                }
            }
        } catch (Exception e) {
            System.err.println("批量添加配方到管理器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 更新玩家配方书
     */
    private static void updatePlayerRecipeBook(Recipe<?> recipe, boolean add) {
        Minecraft mc = Minecraft.getInstance();
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();

        if (mc.player != null) {
            UUID uuid = mc.player.getUUID();

            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null) {
                    if (add) {
                        player.getRecipeBook().addRecipes(Set.of(recipe), player);
                    } else {
                        player.getRecipeBook().removeRecipes(Set.of(recipe), player);
                    }
                }
            }
        }

        mc.execute(() -> {
            if (mc.player != null && mc.level != null) {
                ClientRecipeBook clientRecipeBook = mc.player.getRecipeBook();

                if (add) {
                    clientRecipeBook.add(recipe);
                } else {
                    clientRecipeBook.remove(recipe);
                }

                rebuildClientRecipeBook(clientRecipeBook, mc);
            }
        });
    }

    /**
     * 批量更新玩家配方书
     */
    private static void updatePlayerRecipeBook(List<Recipe<?>> recipes, boolean add) {
        Minecraft mc = Minecraft.getInstance();
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();

        if (mc.player != null) {
            UUID uuid = mc.player.getUUID();

            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null) {
                    if (add) {
                        player.getRecipeBook().addRecipes(recipes, player);
                    } else {
                        player.getRecipeBook().removeRecipes(recipes, player);
                    }
                }
            }
        }

        mc.execute(() -> {
            if (mc.player != null && mc.level != null) {
                ClientRecipeBook clientRecipeBook = mc.player.getRecipeBook();

                for (Recipe<?> recipe : recipes) {
                    if (add) {
                        clientRecipeBook.add(recipe);
                    } else {
                        clientRecipeBook.remove(recipe);
                    }
                }

                rebuildClientRecipeBook(clientRecipeBook, mc);
            }
        });
    }

    /**
     * 更新JEI显示
     */
    //@SuppressWarnings("all")
    /* static void updateJEI(Recipe<?> recipe, RecipeType<Object> jeiType, boolean hide) {
        IJeiRuntime runtime = getJeiRuntime();
        if (runtime == null) return;

        try {
            RecipeType<Object> type = jeiType;
            if (type == null) {
                ResourceLocation recipeTypeUid = new ResourceLocation(recipe.getType().toString());
                Optional<RecipeType<?>> jeiRecipeTypeOpt = runtime.getRecipeManager().getRecipeType(recipeTypeUid);
                if (jeiRecipeTypeOpt.isPresent()) {
                    type = (RecipeType<Object>) jeiRecipeTypeOpt.get();
                } else {
                    System.err.println("未找到JEI配方类型: " + recipeTypeUid);
                    return;
                }
            }

            if (hide) {
                runtime.getRecipeManager().hideRecipes(type, List.of(recipe));
            } else {
                runtime.getRecipeManager().addRecipes(type, List.of(recipe));
            }
        } catch (Exception e) {
            System.err.println("更新JEI失败: " + e.getMessage());
            e.printStackTrace();
        }
    }*/

    /**
     * 批量更新JEI显示
     */
    @SuppressWarnings("all")
    /*private static void updateJEI(List<Object> recipes, RecipeType<Object> jeiType,
                                  Object recipeType, boolean hide) {
        IJeiRuntime runtime = getJeiRuntime();
        if (runtime == null) return;

        try {
            RecipeType<Object> type = jeiType;
            if (type == null) {
                ResourceLocation recipeTypeUid = new ResourceLocation(recipeType.toString());
                Optional<RecipeType<?>> jeiRecipeTypeOpt = runtime.getRecipeManager().getRecipeType(recipeTypeUid);
                if (jeiRecipeTypeOpt.isPresent()) {
                    type = (RecipeType<Object>) jeiRecipeTypeOpt.get();
                } else {
                    System.err.println("未找到JEI配方类型: " + recipeTypeUid);
                    return;
                }
            }

            if (hide) {
                runtime.getRecipeManager().hideRecipes(type, recipes);
            } else {
                runtime.getRecipeManager().addRecipes(type, recipes);
            }
        } catch (Exception e) {
            System.err.println("批量更新JEI失败: " + e.getMessage());
            e.printStackTrace();
        }
    }*/

    /**
     * 重新构建客户端配方书分类集合
     */
    private static void rebuildClientRecipeBook(ClientRecipeBook clientRecipeBook, Minecraft mc) {
        if (mc.level == null) return;

        Collection<Recipe<?>> allRecipes = mc.level.getRecipeManager().getRecipes();
        clientRecipeBook.setupCollections(allRecipes, mc.level.registryAccess());
    }

    /**
     * 获取JEI运行时
     */
    /*private static IJeiRuntime getJeiRuntime() {
        return NDJeiPlugin.jeiRuntime;
    }*/

    /**
     * 获取服务器RecipeManager
     */
    private static RecipeManager getServerRecipeManager() {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.getRecipeManager();
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
     * 记录错误日志
     */
    private static void logError(String message, OperationMode mode, Exception e) {
        String modeStr = mode != null ? mode.name() : "未知";
        System.err.println(message + " [" + modeStr + "]: " + e.getMessage());
        e.printStackTrace();
    }
}