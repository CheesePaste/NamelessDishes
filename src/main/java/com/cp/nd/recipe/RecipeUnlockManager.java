package com.cp.nd.recipe;

import com.cp.nd.util.RecipeUtil;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecipeUnlockManager {
    static final Logger LOGGER = LogManager.getLogger(RecipeUnlockManager.class);
    public static RecipeUnlockManager manager=new RecipeUnlockManager();
    Set<Recipe<?>> allRecipes=new HashSet<>();
    Set<Recipe<?>> lockRecipes=new HashSet<>();

    public void add(Recipe<?> recipe)
    {
        allRecipes.add(recipe);
        lockRecipes.add(recipe);
    }
    public void unlock(Recipe<?> recipe)
    {
        lockRecipes.remove(recipe);
        RecipeUtil.show(recipe);
    }
    public void unlock(Recipe<?> recipe, RecipeType<Object> type)
    {
        lockRecipes.remove(recipe);
        RecipeUtil.show(recipe,type);
    }
    public Set<Recipe<?>> getAllRecipes()
    {
        return allRecipes;
    }
    public Set<Recipe<?>> getLockRecipes()
    {
        return  lockRecipes;
    }
    public boolean isUnlock(Recipe<?> r)
    {
        return allRecipes.contains(r)&&!lockRecipes.contains(r);
    }
    public boolean contains(Recipe<?> r)
    {
        return allRecipes.contains(r);
    }
    public boolean lockContains(Recipe<?> r)
    {
        return lockRecipes.contains(r);
    }
    public void updateLock()
    {
        RecipeUtil.hide(lockRecipes);
    }

    public void HideDefaultRecipe(net.minecraft.world.item.crafting.RecipeType<?> type)
    {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        ClientRecipeBook clientRecipeBook=Minecraft.getInstance().player.getRecipeBook();
        List<RecipeCollection> recipeCollections =clientRecipeBook.getCollections();
        for(RecipeCollection collection : recipeCollections)
        {
            for(Recipe<?> recipe : collection.getRecipes())
            {
                if(recipe.getType().equals(type))
                {
                    add(recipe);
                }
            }
        }
        LOGGER.info("查询配方数:{}", allRecipes.size());
        updateLock();

    }
}
