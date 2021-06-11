package me.jaackson.etched.bridge.fabric;

import me.jaackson.etched.common.network.EtchedPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Jackson
 */
public class NetworkBridgeImpl {

    public static <T extends EtchedPacket> void registerPlayToClient(ResourceLocation channel, Class<T> messageType, Function<FriendlyByteBuf, T> read, Supplier<Consumer<T>> handle) {
        ClientPlayNetworking.registerGlobalReceiver(channel, (client, handler, buf, responseSender) -> handle.get().accept(read.apply(buf)));
    }

    public static <T extends EtchedPacket> void registerPlayToServer(ResourceLocation channel, Class<T> messageType, Function<FriendlyByteBuf, T> read, BiConsumer<T, Player> handle) {
        ServerPlayNetworking.registerGlobalReceiver(channel, (server, player, handler, buf, responseSender) -> handle.accept(read.apply(buf), player));
    }

    public static void sendToPlayer(ServerPlayer player, EtchedPacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.write(buf);
        ServerPlayNetworking.send(player, packet.getChannel(), buf);
    }

    public static void sendToTracking(Entity tracking, EtchedPacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.write(buf);
        for (ServerPlayer player : PlayerLookup.tracking(tracking))
            ServerPlayNetworking.send(player, packet.getChannel(), buf);
    }

    public static void sendToNear(ServerLevel level, double x, double y, double z, double distance, EtchedPacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.write(buf);
        for (ServerPlayer player : PlayerLookup.around(level, new Vec3(x, y, z), distance))
            ServerPlayNetworking.send(player, packet.getChannel(), buf);
    }

    public static void sendToServer(EtchedPacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.write(buf);
        ClientPlayNetworking.send(packet.getChannel(), buf);
    }
}
