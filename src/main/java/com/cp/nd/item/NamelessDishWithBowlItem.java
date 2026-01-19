package com.cp.nd.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class NamelessDishWithBowlItem extends AbstractNamelessDishItem {

    public NamelessDishWithBowlItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getContainerItem() {
        // 食用后返回碗
        return new ItemStack(Items.BOWL);
    }
}