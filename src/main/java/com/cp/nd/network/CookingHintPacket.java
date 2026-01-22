// com.cp.nd.network.CookingHintPacket.java
package com.cp.nd.network;

import com.cp.nd.gui.toast.RecipeToastManager;
import com.cp.nd.util.RecipeMatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CookingHintPacket {
    private final ResourceLocation recipeId;
    private final String hint;
    private final float similarity;
    private final List<ItemStack> missingItems;
    private final List<ItemStack> extraItems;
    private final String hintType;

    public CookingHintPacket(ResourceLocation recipeId, String hint, float similarity,
                             List<ItemStack> missingItems, List<ItemStack> extraItems, String hintType) {
        this.recipeId = recipeId;
        this.hint = hint;
        this.similarity = similarity;
        this.missingItems = missingItems;
        this.extraItems = extraItems;
        this.hintType = hintType;
    }

    public static void encode(CookingHintPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.recipeId);
        buffer.writeUtf(packet.hint);
        buffer.writeFloat(packet.similarity);

        // 写入缺失物品
        buffer.writeInt(packet.missingItems.size());
        for (ItemStack stack : packet.missingItems) {
            buffer.writeItem(stack);
        }

        // 写入多余物品
        buffer.writeInt(packet.extraItems.size());
        for (ItemStack stack : packet.extraItems) {
            buffer.writeItem(stack);
        }

        buffer.writeUtf(packet.hintType);
    }

    public static CookingHintPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation recipeId = buffer.readResourceLocation();
        String hint = buffer.readUtf();
        float similarity = buffer.readFloat();

        // 读取缺失物品
        int missingCount = buffer.readInt();
        List<ItemStack> missingItems = new ArrayList<>();
        for (int i = 0; i < missingCount; i++) {
            missingItems.add(buffer.readItem());
        }

        // 读取多余物品
        int extraCount = buffer.readInt();
        List<ItemStack> extraItems = new ArrayList<>();
        for (int i = 0; i < extraCount; i++) {
            extraItems.add(buffer.readItem());
        }

        String hintType = buffer.readUtf();

        return new CookingHintPacket(recipeId, hint, similarity, missingItems, extraItems, hintType);
    }

    public static void handle(CookingHintPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            // 在客户端处理
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            // 获取配方
            RecipeManager recipeManager = mc.level.getRecipeManager();
            Recipe<?> recipe = recipeManager.byKey(packet.recipeId).orElse(null);

            if (recipe != null) {
                // 创建匹配结果
                RecipeMatcher.MatchResult match = new RecipeMatcher.MatchResult(
                        recipe, packet.similarity, packet.hint,
                        packet.missingItems, packet.extraItems,
                        packet.missingItems.size() + packet.extraItems.size(),
                        recipe.getIngredients().size()
                );

                // 显示Toast
                RecipeToastManager.showCookingHint(match);
            } else {
                // 如果找不到配方，只显示提示
                RecipeMatcher.HintType type;
                try {
                    type = RecipeMatcher.HintType.valueOf(packet.hintType);
                } catch (IllegalArgumentException e) {
                    type = RecipeMatcher.HintType.GENERAL;
                }
                RecipeToastManager.showCookingHint(packet.hint, type);
            }
        });
        ctx.setPacketHandled(true);
    }
}