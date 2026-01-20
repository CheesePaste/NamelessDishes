package com.cp.nd.mixin.fd;

import com.cp.nd.NamelessDishes;
import com.cp.nd.compatibility.fd.FarmersDelightHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//注意，mixin一定要加入remap=false
@Mixin(CookingPotBlockEntity.class)
public abstract class CookingPotBlockEntityMixin {
    @Unique
    private final static int namelessDishes$cooktime =100;

    @Inject(
            method = "cookingTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lvectorwing/farmersdelight/common/block/entity/CookingPotBlockEntity;)V",
            at = @At("HEAD"),
            cancellable = true,remap = false
    )
    private static void onCookingTick(Level level, BlockPos pos, BlockState state,
                                      CookingPotBlockEntity cookingPot, CallbackInfo ci) {
        if (level.isClientSide()) {
            return;
        }

        // 通过访问器调用私有方法
        CookingPotBlockEntityAccessor accessor = (CookingPotBlockEntityAccessor) cookingPot;

        // 检查是否被加热且有输入
        if (!cookingPot.isHeated(level, pos) || !accessor.farmersdelight$hasInput()) {
            return;
        }

        // 先检查是否有原版配方
        Optional<CookingPotRecipe> recipe = accessor.farmersdelight$getMatchingRecipe(
                new net.minecraftforge.items.wrapper.RecipeWrapper(accessor.farmersdelight$getInventory())
        );

        if (recipe.isPresent()) {
            return; // 有原版配方，让原版逻辑处理
        }

        // 获取农夫乐事处理器
        FarmersDelightHandler handler = new FarmersDelightHandler();

        // 获取当前输入物品
        List<ItemStack> inputs = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ItemStack stack = accessor.farmersdelight$getInventory().getStackInSlot(i);
            if (!stack.isEmpty()) {
                inputs.add(stack.copy());
            }
        }

        // 检查是否允许创建无名料理
        if (!handler.allowNamelessCrafting(level, cookingPot, inputs)) {
            return;
        }
        ci.cancel();

        accessor.farmersdelight$setCookTimeTotal(namelessDishes$cooktime *inputs.size());
        accessor.farmersdelight$setCookTime(accessor.farmersdelight$getCookTime()+1);
        boolean didInventoryChange=false;
        if(accessor.farmersdelight$getCookTime()> accessor.farmersdelight$getCookTimeTotal())
        {
            // 创建无名料理
            ItemStack namelessResult = handler.createNamelessResult(level, cookingPot, inputs);
            if (namelessResult.isEmpty()) {
                //这个if里面基本不可能触发，不用考虑是这里卡住
                return;
            }
            //executeCooking负责在展示槽生成无名料理，展示槽是“需要碗”提示的那个槽
            boolean success = handler.executeCooking(level, cookingPot, inputs, namelessResult);
            if(success) {
                accessor.farmersdelight$setCookTime(0);
                accessor.farmersdelight$setCookTimeTotal(0);
                NamelessDishes.LOGGER.debug("Created nameless dish in FarmersDelight cooking pot at {}", pos);
            }
            didInventoryChange=true;
        }

        //这里是将展示槽的物品挪入输出槽，在展示槽创建无名料理使用handler的execute'Cooking实现
        ItemStack mealStack = cookingPot.getMeal();
        if (!mealStack.isEmpty()) {
            if (!accessor.farmersdelight$doesMealHaveContainer(mealStack)) {
                accessor.farmersdelight$moveMealToOutput();
                didInventoryChange = true;
            } else if (!accessor.farmersdelight$getInventory().getStackInSlot(7).isEmpty()) {
                accessor.farmersdelight$useStoredContainerOnMeal();
                didInventoryChange = true;
            }
        }

        //照搬农夫乐事原版的背包刷新，避免吞产物
        if (didInventoryChange) {
            cookingPot.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }

    }
}