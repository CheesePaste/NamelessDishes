// com.cp.nd.gui.toast.RecipeToastManager.java 修改版
package com.cp.nd.gui.toast;

import com.cp.nd.util.RecipeMatcher;
import com.cp.nd.util.RecipeMatcher.HintType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeToastManager {

    public static void showRecipeUnlockToast(Recipe<?> recipe) {
        if (recipe != null && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().getToasts().addToast(
                        new RecipeUnlockToast(recipe)
                );
            });
        }
    }

    // 显示烹饪提示
    public static void showCookingHint(String hintText, HintType type) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().getToasts().addToast(
                        new CookingHintToast(hintText, type)
                );
            });
        }
    }

    // 显示带目标物品的烹饪提示
    public static void showCookingHintWithItem(String hintText, HintType type, ItemStack targetItem) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().getToasts().addToast(
                        new CookingHintToast(hintText, type, targetItem)
                );
            });
        }
    }

    // 根据匹配结果显示提示
    public static void showCookingHint(RecipeMatcher.MatchResult match) {
        if (match == null || Minecraft.getInstance().player == null) {
            return;
        }

        HintType type = RecipeMatcher.determineHintType(match);
        ItemStack targetItem = match.recipe.getResultItem(Minecraft.getInstance().level.registryAccess()).copy();

        Minecraft.getInstance().execute(() -> {
            // 显示主要提示
            Minecraft.getInstance().getToasts().addToast(
                    new CookingHintToast(match.hint, type, targetItem)
            );

        });
    }

    // 批量解锁配方的方法
    public static void showBulkRecipeUnlockToast(Iterable<Recipe<?>> recipes) {
        Minecraft.getInstance().execute(() -> {
            for (Recipe<?> recipe : recipes) {
                if (recipe != null) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                        Minecraft.getInstance().getToasts().addToast(
                                new RecipeUnlockToast(recipe)
                        );
                }
            }
        });
    }
}