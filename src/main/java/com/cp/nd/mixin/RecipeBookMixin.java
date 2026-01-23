package com.cp.nd.mixin;

import com.cp.nd.recipe.RecipeRegisterManager;
import com.cp.nd.recipe.RecipeUnlockManager;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(RecipeBook.class)
public abstract class RecipeBookMixin {


    private static final Logger LOGGER = LogManager.getLogger(RecipeBookMixin.class);
    @Inject(method = "add*", at = @At("HEAD"), cancellable = true)
    private void onAdd(Recipe<?> recipe, CallbackInfo ci) {
        if(RecipeUnlockManager.manager.lockContains(recipe))
        {
            LOGGER.info("试图阻止不合适的解锁"+ recipe);
            ci.cancel();
        }
    }

    /*@Inject(method = "m_12700_", at = @At("HEAD"), cancellable = true)
    private void onAdd(Recipe<?> recipe, CallbackInfo ci) {
        if(RecipeUnlockManager.manager.lockContains(recipe))
        {
            LOGGER.info("试图阻止不合适的解锁"+ recipe);
            ci.cancel();
        }
    }*/
}