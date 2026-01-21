package com.cp.nd.item;

import com.cp.nd.NamelessDishes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

//这个类完全由AI生成，没有人为检验过代码是否符合预期
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, NamelessDishes.MOD_ID);

    // 带碗的无名料理
    public static final RegistryObject<Item> NAMELESS_DISH_WITH_BOWL = ITEMS.register(
            "nameless_dish_with_bowl",
            () -> new NamelessDishWithBowlItem(
                    new Item.Properties()
                            .stacksTo(64)
                            .food(new FoodProperties.Builder()
                                    .nutrition(0) // 动态设置
                                    .saturationMod(0)
                                    .build())
            )
    );

    // 不带碗的无名料理
    public static final RegistryObject<Item> NAMELESS_DISH = ITEMS.register(
            "nameless_dish",
            () -> new NamelessDishItem(
                    new Item.Properties()
                            .stacksTo(64)
                            .food(new FoodProperties.Builder()
                                    .nutrition(0) // 动态设置
                                    .saturationMod(0)
                                    .build())
            )
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}