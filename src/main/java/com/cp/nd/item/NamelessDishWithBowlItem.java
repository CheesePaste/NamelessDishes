package com.cp.nd.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

//这个类完全由AI生成，没有人为检验过代码是否符合预期
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