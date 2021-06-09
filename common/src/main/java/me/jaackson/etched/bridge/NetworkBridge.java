package me.jaackson.etched.bridge;

import me.jaackson.etched.common.network.EtchedPacket;
import me.shedaniel.architectury.annotations.ExpectPlatform;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Jackson
 */
public final class NetworkBridge {

    @ExpectPlatform
    public static <T extends EtchedPacket> void playToClient(ResourceLocation channel, Class<T> messageType, Function<FriendlyByteBuf, T> read, Consumer<T> handle) {
        Platform.safeAssertionError();
    }

    @ExpectPlatform
    public static <T extends EtchedPacket> void playToServer(ResourceLocation channel, Class<T> messageType, Function<FriendlyByteBuf, T> read, BiConsumer<T, Player> handle) {
        Platform.safeAssertionError();
    }

    @ExpectPlatform
    public static void sendToPlayer(ResourceLocation channel, ServerPlayer player, EtchedPacket packet) {
        Platform.safeAssertionError();
    }

    @ExpectPlatform
    public static void sendToTracking(ResourceLocation channel, Entity tracking, EtchedPacket packet) {
        Platform.safeAssertionError();
    }

    @ExpectPlatform
    public static void sendToNear(ResourceLocation channel, ServerLevel level, double x, double y, double z, double distance, EtchedPacket packet) {
        Platform.safeAssertionError();
    }

    @ExpectPlatform
    public static void sendToServer(ResourceLocation channel, EtchedPacket packet) {
        Platform.safeAssertionError();
    }
}
