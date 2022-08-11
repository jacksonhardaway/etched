package gg.moonflower.etched.common.network.play.handler;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.api.sound.StopListeningSound;
import gg.moonflower.etched.client.screen.AlbumJukeboxScreen;
import gg.moonflower.etched.client.screen.EtchingScreen;
import gg.moonflower.etched.client.screen.RadioScreen;
import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import gg.moonflower.etched.common.entity.MinecartJukebox;
import gg.moonflower.etched.common.network.play.*;
import gg.moonflower.etched.core.mixin.client.LevelRendererAccessor;
import gg.moonflower.pollen.api.network.packet.PollinatedPacketContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.MinecartSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@ApiStatus.Internal
public class EtchedClientPlayPacketHandlerImpl implements EtchedClientPlayPacketHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handlePlayMusicPacket(ClientboundPlayMusicPacket pkt, PollinatedPacketContext ctx) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingRecords();

        BlockPos pos = pkt.getPos();
        SoundInstance soundInstance = playingRecords.get(pos);
        ctx.enqueueWork(() -> {
            if (soundInstance != null) {
                soundManager.stop(soundInstance);
                playingRecords.remove(pos);
            }

            if (pkt.getTracks().length == 0)
                return;

            SoundTracker.playBlockRecord(pos, pkt.getTracks(), 0);
        });
    }

    @Override
    public void handleAddMinecartJukeboxPacket(ClientboundAddMinecartJukeboxPacket pkt, PollinatedPacketContext ctx) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        ctx.enqueueWork(() -> {
            MinecartJukebox entity = new MinecartJukebox(level, pkt.getX(), pkt.getY(), pkt.getZ());
            int i = pkt.getId();
            entity.setPacketCoordinates(pkt.getX(), pkt.getY(), pkt.getZ());
            entity.moveTo(pkt.getX(), pkt.getY(), pkt.getZ());
            entity.xRot = (float) (pkt.getxRot() * 360) / 256.0F;
            entity.yRot = (float) (pkt.getyRot() * 360) / 256.0F;
            entity.setId(i);
            entity.setUUID(pkt.getUUID());
            level.putNonPlayerEntity(i, entity);
            Minecraft.getInstance().getSoundManager().play(new MinecartSoundInstance(entity));
        });
    }

    @Override
    public void handlePlayEntityMusicPacket(ClientboundPlayEntityMusicPacket pkt, PollinatedPacketContext ctx) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        int entityId = pkt.getEntityId();
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        SoundInstance soundInstance = SoundTracker.getEntitySound(entityId);
        ctx.enqueueWork(() -> {
            if (soundInstance != null) {
                if (soundInstance instanceof StopListeningSound)
                    ((StopListeningSound) soundInstance).stopListening();
                if (pkt.getAction() == ClientboundPlayEntityMusicPacket.Action.RESTART && soundManager.isActive(soundInstance))
                    return;
                SoundTracker.setEntitySound(entityId, null);
            }

            if (pkt.getAction() == ClientboundPlayEntityMusicPacket.Action.STOP)
                return;

            Entity entity = level.getEntity(entityId);
            if (entity == null) {
                LOGGER.error("Server sent sound for nonexistent entity: " + entityId);
                return;
            }

            ItemStack record = pkt.getRecord();
            if (!PlayableRecord.isPlayableRecord(record)) {
                LOGGER.error("Server sent invalid music disc: " + record);
                return;
            }

            Optional<? extends SoundInstance> sound = ((PlayableRecord) record.getItem()).createEntitySound(record, entity, 0);
            if (!sound.isPresent()) {
                LOGGER.error("Server sent invalid music disc: " + record);
                return;
            }

            SoundInstance entitySound = StopListeningSound.create(sound.get(), () -> Minecraft.getInstance().tell(() -> {
                SoundTracker.setEntitySound(entityId, null);
                SoundTracker.playEntityRecord(record, entityId, 1, false);
            }));

            SoundTracker.setEntitySound(entityId, entitySound);
        });
    }

    @Override
    public void handleSetInvalidEtch(ClientboundInvalidEtchUrlPacket pkt, PollinatedPacketContext ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof EtchingScreen) {
                EtchingScreen screen = (EtchingScreen) Minecraft.getInstance().screen;
                screen.setReason(pkt.getException());
            }
        });
    }

    @Override
    public void handleSetUrl(ClientboundSetUrlPacket pkt, PollinatedPacketContext ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof RadioScreen) {
                RadioScreen screen = (RadioScreen) Minecraft.getInstance().screen;
                screen.receiveUrl(pkt.getUrl());
            }
        });
    }

    @Override
    public void handleSetAlbumJukeboxTrack(SetAlbumJukeboxTrackPacket pkt, PollinatedPacketContext ctx) {
        Minecraft minecraft = Minecraft.getInstance();
        ctx.enqueueWork(() -> {
            if (minecraft.level != null && minecraft.screen instanceof AlbumJukeboxScreen) {
                AlbumJukeboxScreen screen = (AlbumJukeboxScreen) minecraft.screen;
                BlockPos pos = screen.getMenu().getPos();
                if (screen.getMenu().setPlayingTrack(minecraft.level, pkt)) {
                    AlbumJukeboxBlockEntity entity = (AlbumJukeboxBlockEntity) Objects.requireNonNull(minecraft.level.getBlockEntity(pos));
                    SoundTracker.playAlbum(entity, entity.getBlockState(), minecraft.level, pos, true);
                }
            }
        });
    }
}
