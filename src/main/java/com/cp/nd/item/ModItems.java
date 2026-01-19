package com.cp.nd.item;

import com.cp.nd.NamelessDishes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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
                                    .alwaysEat()
                                    .build())
            )
    );

    // 不带碗的无名料理
    public static final RegistryObject<Item> NAMELESS_DISH = ITEMS.register(
            "nameless_dish",
            () -> new NamelessDishItem(
                    new Item.Properties()
                            .stacksTo(64) // 不带碗的可以堆叠
                            .food(new FoodProperties.Builder()
                                    .nutrition(0) // 动态设置
                                    .saturationMod(0)
                                    .alwaysEat()
                                    .build())
            )
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}