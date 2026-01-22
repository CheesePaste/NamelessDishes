package com.cp.nd.mixin.fd;

import com.cp.nd.NamelessDishes;
import com.cp.nd.compatibility.fd.FarmersDelightHandler;
import com.cp.nd.gui.toast.RecipeToastManager;
import com.cp.nd.network.CookingHintPacket;
import com.cp.nd.network.NetworkHandler;
import com.cp.nd.recipe.RecipeUnlockManager;
import com.cp.nd.util.RecipeMatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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

import static com.cp.nd.util.FoodUtil.allowNamelessCrafting;
import static com.cp.nd.util.FoodUtil.createNamelessResult;


@Mixin(value = CookingPotBlockEntity.class)
public abstract class CookingPotBlockEntityMixin {
    @Unique
    private final static String namelessDishes$modName ="farmersdelight";
    @Unique
    private final static String namelessDishes$cookingBlockName ="farmersdelight:cooking_pot";
    @Unique
    private final static int namelessDishes$cooktime =100;

    @Inject(
            method = "cookingTick",
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
        if (!allowNamelessCrafting(level, cookingPot, inputs, namelessDishes$modName, namelessDishes$cookingBlockName)) {
            return;
        }
        ci.cancel();

        accessor.farmersdelight$setCookTimeTotal(namelessDishes$cooktime *inputs.size());
        accessor.farmersdelight$setCookTime(accessor.farmersdelight$getCookTime()+1);
        boolean didInventoryChange=false;
        if(accessor.farmersdelight$getCookTime()> accessor.farmersdelight$getCookTimeTotal())
        {
            // 创建无名料理
            ItemStack namelessResult = createNamelessResult(cookingPot, inputs,Items.BOWL.toString());
            if (namelessResult.isEmpty()) {
                //这个if里面基本不可能触发，不用考虑是这里卡住
                return;
            }
            //executeCooking负责在展示槽生成无名料理，展示槽是“需要碗”提示的那个槽
            boolean success = handler.executeCooking(level, cookingPot,namelessResult);
            // 寻找最接近的配方
            RecipeMatcher.MatchResult match = RecipeMatcher.findBestMatch(
                    inputs,
                    RecipeUnlockManager.manager.getLockRecipes(),
                    handler.getStandardRecipe().getType()
            );

            // 如果有匹配的配方，显示Toast提示
            if (match != null && match.recipe != null) {
                // 向附近的玩家发送提示数据包
                List<ServerPlayer> nearbyPlayers = level.getEntitiesOfClass(
                        ServerPlayer.class,
                        new AABB(pos).inflate(8.0) // 8格范围内的玩家
                );

                for (ServerPlayer player : nearbyPlayers) {
                    // 创建数据包
                    CookingHintPacket packet = new CookingHintPacket(
                            match.recipe.getId(),
                            match.hint,
                            match.similarity,
                            match.missingItems,
                            match.extraItems,
                            RecipeMatcher.determineHintType(match).name()
                    );

                    // 发送给客户端
                    NetworkHandler.sendToClient(packet, player);
                }

                NamelessDishes.LOGGER.debug("Found close recipe match for failed cooking: {} (similarity: {})",
                        match.recipe.getId(), match.similarity);
            }
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
            if (!accessor.farmersdelight$getInventory().getStackInSlot(7).isEmpty()) {
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

    /**
     * 注入到 setRecipeUsed 方法中
     * 这个方法在每次配方成功使用时被调用
     */
    @Inject(
            method = "m_6029_",
            at = @At("HEAD"),
            remap = true
    )
    public void onSetRecipeUsed(Recipe<?> recipe, CallbackInfo ci) {
        if(RecipeUnlockManager.manager.lockContains(recipe)) {
            RecipeUnlockManager.manager.unlock(recipe);
            RecipeToastManager.showRecipeUnlockToast(recipe);
        }
    }
}