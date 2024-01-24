package gg.moonflower.etched.common.network.play.handler;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.api.sound.StopListeningSound;
import gg.moonflower.etched.client.screen.AlbumJukeboxScreen;
import gg.moonflower.etched.client.screen.EtchingScreen;
import gg.moonflower.etched.client.screen.RadioScreen;
import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import gg.moonflower.etched.common.network.play.*;
import gg.moonflower.etched.core.mixin.client.LevelRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@ApiStatus.Internal
public class EtchedClientPlayPacketHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void handlePlayMusicPacket(ClientboundPlayMusicPacket pkt, NetworkEvent.Context ctx) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingRecords();

        BlockPos pos = pkt.getPos();
        SoundInstance soundInstance = playingRecords.get(pos);
        ctx.enqueueWork(() -> {
            if (soundInstance != null) {
                soundManager.stop(soundInstance);
                playingRecords.remove(pos);
            }

            TrackData[] tracks = pkt.getTracks();
            if (tracks.length == 0) {
                return;
            }

            SoundTracker.playBlockRecord(pos, tracks, 0);
        });
    }

    public static void handlePlayEntityMusicPacket(ClientboundPlayEntityMusicPacket pkt, NetworkEvent.Context ctx) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        int entityId = pkt.getEntityId();
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        SoundInstance soundInstance = SoundTracker.getEntitySound(entityId);
        ctx.enqueueWork(() -> {
            if (soundInstance != null) {
                if (soundInstance instanceof StopListeningSound) {
                    ((StopListeningSound) soundInstance).stopListening();
                }
                if (pkt.getAction() == ClientboundPlayEntityMusicPacket.Action.RESTART && soundManager.isActive(soundInstance)) {
                    return;
                }
                SoundTracker.setEntitySound(entityId, null);
            }

            if (pkt.getAction() == ClientboundPlayEntityMusicPacket.Action.STOP) {
                return;
            }

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
            if (sound.isEmpty()) {
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

    public static void handleSetInvalidEtch(ClientboundInvalidEtchUrlPacket pkt, NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof EtchingScreen screen) {
                screen.setReason(pkt.getException());
            }
        });
    }

    public static void handleSetUrl(ClientboundSetUrlPacket pkt, NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof RadioScreen screen) {
                screen.receiveUrl(pkt.getUrl());
            }
        });
    }

    public static void handleSetAlbumJukeboxTrack(SetAlbumJukeboxTrackPacket pkt, NetworkEvent.Context ctx) {
        Minecraft minecraft = Minecraft.getInstance();
        ctx.enqueueWork(() -> {
            if (minecraft.level != null && minecraft.screen instanceof AlbumJukeboxScreen screen) {
                BlockPos pos = screen.getMenu().getPos();
                if (screen.getMenu().setPlayingTrack(minecraft.level, pkt)) {
                    AlbumJukeboxBlockEntity entity = (AlbumJukeboxBlockEntity) Objects.requireNonNull(minecraft.level.getBlockEntity(pos));
                    SoundTracker.playAlbum(entity, entity.getBlockState(), minecraft.level, pos, true);
                }
            }
        });
    }
}
