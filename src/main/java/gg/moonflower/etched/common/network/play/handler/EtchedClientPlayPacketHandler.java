package gg.moonflower.etched.common.network.play.handler;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.api.sound.StopListeningSound;
import gg.moonflower.etched.client.screen.AlbumJukeboxScreen;
import gg.moonflower.etched.client.screen.EtchingScreen;
import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import gg.moonflower.etched.common.menu.UrlMenu;
import gg.moonflower.etched.common.network.play.*;
import gg.moonflower.etched.core.mixin.client.render.LevelRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

@ApiStatus.Internal
public class EtchedClientPlayPacketHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void handlePlayBlockMusicPacket(ClientboundPlayBlockMusicPacket pkt, IPayloadContext ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }

        BlockPos pos = pkt.pos();
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) client.levelRenderer).getPlayingJukeboxSongs();
        SoundInstance soundInstance = playingRecords.get(pos);

        if (soundInstance != null) {
            client.getSoundManager().stop(soundInstance);
            playingRecords.remove(pos);
        }

        List<TrackData> tracks = pkt.tracks(client.getConnection().registryAccess());
        if (tracks.isEmpty()) {
            return;
        }

        SoundTracker.playBlockRecord(pos, tracks.toArray(TrackData[]::new), 0);
    }

    public static void handlePlayEntityMusicPacket(ClientboundPlayEntityMusicPacket pkt, IPayloadContext ctx) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null) {
            return;
        }

        int entityId = pkt.entityId();
        SoundInstance soundInstance = SoundTracker.getEntitySound(entityId);
        if (soundInstance != null) {
            if (pkt.action() == ClientboundPlayEntityMusicPacket.Action.RESTART && client.getSoundManager().isActive(soundInstance)) {
                return;
            }
            if (soundInstance instanceof StopListeningSound) {
                ((StopListeningSound) soundInstance).stopListening();
            }
            SoundTracker.setEntitySound(entityId, null);
        }

        if (pkt.action() == ClientboundPlayEntityMusicPacket.Action.STOP) {
            return;
        }

        Entity entity = level.getEntity(entityId);
        if (entity == null) {
            LOGGER.error("Server sent sound for nonexistent entity: {}", entityId);
            return;
        }

        ItemStack record = pkt.record();
        if (!PlayableRecord.isPlayableRecord(record)) {
            LOGGER.error("Server sent invalid music disc: {}", record);
            return;
        }

        SoundTracker.playEntityRecord(record, entityId, 0, 16, false, pkt.storageId());
    }

    public static void handleSetInvalidEtch(ClientboundInvalidEtchUrlPacket pkt, IPayloadContext ctx) {
        if (Minecraft.getInstance().screen instanceof EtchingScreen screen) {
            screen.setReason(pkt.exception());
        }
    }

    public static void handleSetUrl(SetUrlPacket pkt, IPayloadContext ctx) {
        if (Minecraft.getInstance().screen instanceof UrlMenu screen) {
            screen.setUrl(pkt.url());
        }
    }

    public static void handleSetAlbumJukeboxTrack(SetAlbumJukeboxTrackPacket pkt, IPayloadContext ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.screen instanceof AlbumJukeboxScreen screen) {
            BlockPos pos = screen.getMenu().getPos();
            if (screen.getMenu().setPlayingTrack(client.level, pkt)) {
                AlbumJukeboxBlockEntity entity = (AlbumJukeboxBlockEntity) Objects.requireNonNull(client.level.getBlockEntity(pos));
                SoundTracker.playAlbum(entity, entity.getBlockState(), client.level, pos, true);
            }
        }
    }
}
