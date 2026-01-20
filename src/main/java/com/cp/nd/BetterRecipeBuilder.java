// BetterRecipeBuilder.java
package com.cp.nd;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.core.NonNullList;

public class BetterRecipeBuilder {



    /**
     * 创建有效的无序配方
     */
    public static ShapelessRecipe createValidShapelessRecipe(ResourceLocation id,
                                                             NonNullList<Ingredient> ingredients,
                                                             ItemStack result) {
        return new ShapelessRecipe(
                id,
                id.toString(),
                CraftingBookCategory.MISC,
                result,
                ingredients
        );
    }


    /**
     * 创建可见的无序配方
     */
    public static ShapelessRecipe createVisibleShapelessRecipe() {
        ResourceLocation id = new ResourceLocation(NamelessDishes.MOD_ID, "visible_cobble_to_stone");
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(Items.COBBLESTONE));

        return new ShapelessRecipe(
                id,
                id.toString(),
                CraftingBookCategory.MISC,
                new ItemStack(Items.STONE),
                ingredients
        );
    }
}