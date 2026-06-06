package gg.moonflower.etched.core.compat;

import gg.moonflower.etched.common.component.MusicTrackComponent;
import gg.moonflower.etched.common.network.play.ClientboundPlayBlockMusicPacket;
import gg.moonflower.etched.common.network.play.ClientboundPlayEntityMusicPacket;
import gg.moonflower.etched.core.registry.EtchedComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.api.IDiscHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.ServerStorageSoundHandler;
import org.jetbrains.annotations.NotNull;
import java.util.Optional;
import java.util.UUID;

public class EtchedDiscHandler implements IDiscHandler<MusicTrackComponent> {

    @Override
    public @NotNull Optional<MusicTrackComponent> getSongInfo(@NotNull ItemStack stack, @NotNull Level level) {
        return Optional.ofNullable(stack.get(EtchedComponents.MUSIC));
    }

    @Override
    public void playDisc(@NotNull ServerLevel serverLevel, @NotNull BlockPos pos, @NotNull UUID storageUuid, @NotNull ItemStack discItemStack, @NotNull Runnable onFinished) {
        PacketDistributor.sendToPlayersNear(serverLevel, null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 64, new ClientboundPlayBlockMusicPacket(discItemStack.copy(), pos, storageUuid));
        ServerStorageSoundHandler.putSoundInfo(serverLevel, storageUuid, onFinished, Vec3.atCenterOf(pos), Long.MAX_VALUE);
    }

    @Override
    public void playDisc(@NotNull ServerLevel serverLevel, @NotNull Vec3 pos, @NotNull UUID storageUuid, @NotNull ItemStack discItemStack, int entityId, @NotNull Runnable onFinished) {
        Entity entity = serverLevel.getEntity(entityId);
        if (entity != null) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new ClientboundPlayEntityMusicPacket(discItemStack.copy(), entity, false, storageUuid));
            ServerStorageSoundHandler.putSoundInfo(serverLevel, storageUuid, onFinished, pos, Long.MAX_VALUE);
        }
    }

    @Override
    public @NotNull Optional<Integer> getMusicLengthInTicks(@NotNull ItemStack stack, @NotNull Level level) {
        return Optional.empty(); // Not possible to know the length
    }

    @Override
    public boolean supports(@NotNull ItemStack stack) {
        return stack.has(EtchedComponents.MUSIC);
    }

    @Override
    public @NotNull Optional<ItemStack> getRandomDisc(@NotNull RandomSource randomSource) {
        return Optional.empty();
    }

    @Override
    public int getMusicDiscSize() {
        return 0;
    }
}
