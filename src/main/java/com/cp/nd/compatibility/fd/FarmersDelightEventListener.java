/*package com.cp.nd.compatibility.fd;

import com.cp.nd.NamelessDishes;
import com.cp.nd.api.ICookingRecipeHandler;
import com.cp.nd.compatibility.ModCompatibilityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vectorwing.farmersdelight.common.block.CookingPotBlock;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = NamelessDishes.MODID)
public class FarmersDelightEventListener {

    @SubscribeEvent
    public static void onCookingPotCook(BlockEvent.NeighborNotifyEvent event) {
        // 检查是否为烹饪锅方块
        if (!(event.getState().getBlock() instanceof CookingPotBlock)) {
            return;
        }

        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof CookingPotBlockEntity cookingPot)) {
            return;
        }

        // 获取农夫乐事处理器
        ICookingRecipeHandler handler = ModCompatibilityManager.getInstance().getHandlerForMod("farmersdelight");
        if (!(handler instanceof FarmersDelightHandler farmersDelightHandler)) {
            return;
        }

        // 检查是否允许创建无名料理
        List<ItemStack> inputs = getCookingPotInputs(cookingPot);
        if (!handler.allowNamelessCrafting(level, cookingPot, inputs)) {
            return;
        }

        // 创建无名料理
        ItemStack namelessResult = handler.createNamelessResult(level, cookingPot, inputs);
        if (namelessResult.isEmpty()) {
            return;
        }

        // 执行烹饪
        boolean success = handler.executeCooking(level, cookingPot, inputs, namelessResult);
        if (success) {
            NamelessDishes.LOGGER.debug("Created nameless dish in FarmersDelight cooking pot at {}", pos);
        }
    }

    private static List<ItemStack> getCookingPotInputs(CookingPotBlockEntity cookingPot) {
        List<ItemStack> inputs = new ArrayList<>();

        // 获取输入槽位物品（农夫乐事烹饪锅的0-5槽位是输入）
        for (int i = 0; i < 6; i++) {
            ItemStack stack = cookingPot.getInventory().getStackInSlot(i);
            if (!stack.isEmpty()) {
                inputs.add(stack.copy());
            }
        }

        return inputs;
    }
}
 */