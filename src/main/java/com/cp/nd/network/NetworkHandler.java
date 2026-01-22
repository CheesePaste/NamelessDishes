// com.cp.nd.network.NetworkHandler.java
package com.cp.nd.network;

import com.cp.nd.NamelessDishes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(NamelessDishes.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        // 注册烹饪提示包
        CHANNEL.registerMessage(packetId++, CookingHintPacket.class,
                CookingHintPacket::encode,
                CookingHintPacket::decode,
                CookingHintPacket::handle);

        NamelessDishes.LOGGER.info("Registered {} network packets", packetId);
    }

    public static void sendToClient(Object packet, net.minecraft.server.level.ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}