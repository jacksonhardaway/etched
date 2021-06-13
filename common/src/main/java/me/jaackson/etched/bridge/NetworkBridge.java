package me.jaackson.etched.bridge;

import me.jaackson.etched.common.entity.MinecartJukebox;
import me.jaackson.etched.common.network.EtchedPacket;
import me.shedaniel.architectury.annotations.ExpectPlatform;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Jackson
 */
public final class NetworkBridge {

    @ExpectPlatform
    public static <T extends EtchedPacket> void registerPlayToClient(ResourceLocation channel, Class<T> messageType, Function<FriendlyByteBuf, T> read, Supplier<Consumer<T>> handle) {
        Platform.safeAssertionError();
    }

    @ExpectPlatform
    public static <T extends EtchedPacket> void registerPlayToServer(ResourceLocation channel, Class<T> messageType, Function<FriendlyByteBuf, T> read, BiConsumer<T, Player> handle) {
        Platform.safeAssertionError();
    }

    @ExpectPlatform
    public static void sendToPlayer(ServerPlayer player, EtchedPacket packet) {
        Platform.safeAssertionError();
    }

    @ExpectPlatform
    public static void sendToTracking(Entity tracking, EtchedPacket packet) {
        Platform.safeAssertionError();
    }

    @ExpectPlatform
    public static void sendToNear(ServerLevel level, double x, double y, double z, double distance, EtchedPacket packet) {
        Platform.safeAssertionError();
    }

    @ExpectPlatform
    public static void sendToServer(EtchedPacket packet) {
        Platform.safeAssertionError();
    }

    @ExpectPlatform
    public static Packet<?> toVanillaPacket(EtchedPacket packet, boolean clientbound) {
        throw new AssertionError();
    }
}
