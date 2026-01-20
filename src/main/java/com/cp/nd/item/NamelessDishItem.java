package com.cp.nd.item;

import net.minecraft.world.item.ItemStack;

/**
 * 请使用FoodUtil相关方法生成无名料理
 */
public class NamelessDishItem extends AbstractNamelessDishItem {

    public NamelessDishItem(Properties properties) {
        super(properties);
    }


    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return ItemStack.EMPTY;
    }
}