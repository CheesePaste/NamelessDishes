package com.cp.nd.item;

import net.minecraft.world.item.ItemStack;

//这个类完全由AI生成，没有人为检验过代码是否符合预期
public class NamelessDishItem extends AbstractNamelessDishItem {

    public NamelessDishItem(Properties properties) {
        super(properties);
    }


    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return ItemStack.EMPTY;
    }
}