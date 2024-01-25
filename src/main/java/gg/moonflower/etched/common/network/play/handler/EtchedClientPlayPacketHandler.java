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
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null) {
            return;
        }

        ctx.enqueueWork(() -> {
            BlockPos pos = pkt.pos();
            Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) client.levelRenderer).getPlayingRecords();
            SoundInstance soundInstance = playingRecords.get(pos);

            if (soundInstance != null) {
                client.getSoundManager().stop(soundInstance);
                playingRecords.remove(pos);
            }

            TrackData[] tracks = pkt.tracks();
            if (tracks.length == 0) {
                return;
            }

            SoundTracker.playBlockRecord(pos, tracks, 0);
        });
    }

    public static void handlePlayEntityMusicPacket(ClientboundPlayEntityMusicPacket pkt, NetworkEvent.Context ctx) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null) {
            return;
        }

        ctx.enqueueWork(() -> {
            int entityId = pkt.getEntityId();
            SoundInstance soundInstance = SoundTracker.getEntitySound(entityId);
            if (soundInstance != null) {
                if (soundInstance instanceof StopListeningSound) {
                    ((StopListeningSound) soundInstance).stopListening();
                }
                if (pkt.getAction() == ClientboundPlayEntityMusicPacket.Action.RESTART && client.getSoundManager().isActive(soundInstance)) {
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

            SoundInstance entitySound = StopListeningSound.create(sound.get(), () -> client.tell(() -> {
                SoundTracker.setEntitySound(entityId, null);
                SoundTracker.playEntityRecord(record, entityId, 1, false);
            }));

            SoundTracker.setEntitySound(entityId, entitySound);
        });
    }

    public static void handleSetInvalidEtch(ClientboundInvalidEtchUrlPacket pkt, NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof EtchingScreen screen) {
                screen.setReason(pkt.exception());
            }
        });
    }

    public static void handleSetUrl(ClientboundSetUrlPacket pkt, NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof RadioScreen screen) {
                screen.receiveUrl(pkt.url());
            }
        });
    }

    public static void handleSetAlbumJukeboxTrack(SetAlbumJukeboxTrackPacket pkt, NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null && client.screen instanceof AlbumJukeboxScreen screen) {
                BlockPos pos = screen.getMenu().getPos();
                if (screen.getMenu().setPlayingTrack(client.level, pkt)) {
                    AlbumJukeboxBlockEntity entity = (AlbumJukeboxBlockEntity) Objects.requireNonNull(client.level.getBlockEntity(pos));
                    SoundTracker.playAlbum(entity, entity.getBlockState(), client.level, pos, true);
                }
            }
        });
    }
}
