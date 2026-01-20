// IRecipeManagerMixin.java
package com.cp.nd.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

public interface IRecipeManagerMixin {
    void addRecipe(Recipe<?> recipe);
    boolean removeRecipe(ResourceLocation recipeId);
}