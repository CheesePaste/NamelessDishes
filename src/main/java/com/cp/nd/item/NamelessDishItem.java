package com.cp.nd.item;

import net.minecraft.world.item.ItemStack;

public class NamelessDishItem extends AbstractNamelessDishItem {

    public NamelessDishItem(Properties properties) {
        super(properties);
    }


    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return ItemStack.EMPTY;
    }
}