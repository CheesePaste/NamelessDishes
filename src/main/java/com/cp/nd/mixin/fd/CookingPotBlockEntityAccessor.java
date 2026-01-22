package com.cp.nd.mixin.fd;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

import java.util.Optional;

//注意，mixin一定要加入remap=false
@Mixin(value = CookingPotBlockEntity.class,remap = false)
public interface CookingPotBlockEntityAccessor {
    @Accessor(value = "inventory",remap = false)
    ItemStackHandler farmersdelight$getInventory();

    @Invoker(value = "hasInput",remap = false)
    boolean farmersdelight$hasInput();


    @Invoker(value = "getMatchingRecipe",remap = false)
    Optional<CookingPotRecipe> farmersdelight$getMatchingRecipe(RecipeWrapper recipeWrapper);

    @Accessor(value = "cookTime",remap = false)
    void farmersdelight$setCookTime(int cookTime);

    @Accessor(value = "cookTime",remap = false)
    int farmersdelight$getCookTime();

    @Accessor(value = "cookTimeTotal",remap = false)
    int farmersdelight$getCookTimeTotal();

    @Accessor(value = "cookTimeTotal",remap = false)
    void farmersdelight$setCookTimeTotal(int cookTimeTotal);

    @Invoker(value = "useStoredContainersOnMeal",remap = false)
    void farmersdelight$useStoredContainerOnMeal();

}