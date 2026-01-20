// RecipeManagerMixin.java
package com.cp.nd.mixin;

import com.cp.nd.item.IRecipeManagerMixin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin implements IRecipeManagerMixin {

    @Shadow
    private Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes;

    @Shadow
    private Map<ResourceLocation, Recipe<?>> byName;

    @Unique
    @Override
    public void addRecipe(Recipe<?> recipe) {
        ResourceLocation recipeId = recipe.getId();
        RecipeType<?> recipeType = recipe.getType();

        // 确保Map是可变的
        ensureMutable();

        // 添加到type-specific map
        Map<ResourceLocation, Recipe<?>> typeMap = this.recipes.get(recipeType);
        if (typeMap == null) {
            typeMap = new HashMap<>();
            this.recipes.put(recipeType, typeMap);
        }

        // 添加新配方
        typeMap.put(recipeId, recipe);

        // 添加到byName map
        this.byName.put(recipeId, recipe);
    }

    @Unique
    @Override
    public boolean removeRecipe(ResourceLocation recipeId) {
        ensureMutable();

        // 从byName中移除
        Recipe<?> removed = this.byName.remove(recipeId);
        if (removed == null) {
            return false;
        }

        // 从type-specific map中移除
        RecipeType<?> recipeType = removed.getType();
        Map<ResourceLocation, Recipe<?>> typeMap = this.recipes.get(recipeType);
        if (typeMap != null) {
            typeMap.remove(recipeId);

            // 如果这个type的map为空，移除整个entry
            if (typeMap.isEmpty()) {
                this.recipes.remove(recipeType);
            }
        }

        return true;
    }

    @Unique
    @SuppressWarnings("unchecked")
    private void ensureMutable() {
        // 将ImmutableMap转换为可变Map
        if (this.recipes != null && !(this.recipes instanceof HashMap)) {
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> mutableRecipes = new HashMap<>();

            for (Map.Entry<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> entry : this.recipes.entrySet()) {
                Map<ResourceLocation, Recipe<?>> mutableInnerMap = new HashMap<>(entry.getValue());
                mutableRecipes.put(entry.getKey(), mutableInnerMap);
            }

            this.recipes = mutableRecipes;
        }

        if (this.byName != null && !(this.byName instanceof HashMap)) {
            this.byName = new HashMap<>(this.byName);
        }
    }

    // 为了兼容reload操作，我们需要在reload时重新确保可变性
    @Inject(method = "apply", at = @At("HEAD"))
    private void onApplyHead(Map<ResourceLocation, com.google.gson.JsonElement> p_44037_,
                             net.minecraft.server.packs.resources.ResourceManager p_44038_,
                             net.minecraft.util.profiling.ProfilerFiller p_44039_,
                             CallbackInfo ci) {
        // 确保reload前maps是可变的（如果需要）
        ensureMutable();
    }
}