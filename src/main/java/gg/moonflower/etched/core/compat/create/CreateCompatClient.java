package gg.moonflower.etched.core.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.api.sound.StopListeningSound;
import gg.moonflower.etched.client.sound.AlbumPlayback;
import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import gg.moonflower.etched.common.blockentity.RadioBlockEntity;
import gg.moonflower.etched.common.network.play.ClientboundContraptionBlockDataPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CreateCompatClient {

    private static final int ALBUM_JUKEBOX_SIZE = 9;

    static void tickRadio(MovementContext context) {
        String url = context.disabled ? null : CreateCompat.getPlayingUrl(context.state, context.blockEntityData);
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();

        PlayingRadio playing = context.temporaryData instanceof PlayingRadio radio ? radio : null;
        if (playing != null ? Objects.equals(playing.url, url) : url == null) {
            if (playing != null) {
                applyRadioSettings(context, playing.sound);
                spawnNotes(context, playing.sound, 0.2);
            }
            return;
        }

        if (playing != null && playing.sound != null) {
            soundManager.stop(playing.sound);
        }

        PlayingRadio next = new PlayingRadio(url);
        context.temporaryData = next;
        if (url == null) {
            return;
        }

        AbstractContraptionEntity entity = context.contraption.entity;
        BlockPos localPos = context.localPos;
        // The sound stops itself once the contraption is gone or the radio was retuned
        next.sound = SoundTracker.getMovingRadio(url, () -> entity.isAlive() && context.temporaryData == next ? entity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0F) : null);
        if (next.sound != null) {
            applyRadioSettings(context, next.sound);
            soundManager.play(next.sound);
        }
    }

    static void tickJukebox(MovementContext context, boolean album) {
        CompoundTag data = context.blockEntityData;
        boolean powered = context.state.hasProperty(BlockStateProperties.POWERED) && context.state.getValue(BlockStateProperties.POWERED);
        Tag discs = context.disabled || powered || data == null ? null : data.get(album ? "Items" : "RecordItem");

        RecordPlayer player = context.temporaryData instanceof RecordPlayer recordPlayer ? recordPlayer : null;
        if (player == null) {
            if (discs == null) {
                return;
            }
            player = new RecordPlayer(context, album);
            context.temporaryData = player;
        }

        if (!Objects.equals(player.discsTag, discs)) {
            player.setDiscs(discs != null ? discs.copy() : null);
        }
        applyJukeboxSettings(context, player.sound, album);
        spawnNotes(context, player.sound, 0.7);
    }

    private static void applyRadioSettings(MovementContext context, @Nullable SoundInstance sound) {
        SoundTracker.setSoundSettings(sound, CreateCompat.getVolume(context.blockEntityData), CreateCompat.getRadius(context.blockEntityData, RadioBlockEntity.DEFAULT_RADIUS), RadioBlockEntity.DEFAULT_RADIUS);
    }

    private static void applyJukeboxSettings(MovementContext context, @Nullable SoundInstance sound, boolean album) {
        // Vanilla jukeboxes have no settings, so they always play like a default album jukebox
        int radius = album ? CreateCompat.getRadius(context.blockEntityData, AlbumJukeboxBlockEntity.DEFAULT_RADIUS) : AlbumJukeboxBlockEntity.DEFAULT_RADIUS;
        SoundTracker.setSoundSettings(sound, album ? CreateCompat.getVolume(context.blockEntityData) : 1.0F, radius, AlbumJukeboxBlockEntity.DEFAULT_RADIUS);
    }

    private static void spawnNotes(MovementContext context, @Nullable SoundInstance sound, double height) {
        if (sound != null && context.position != null && context.world.random.nextInt(10) == 0 && Minecraft.getInstance().getSoundManager().isActive(sound)) {
            context.world.addParticle(ParticleTypes.NOTE, context.position.x, context.position.y + height, context.position.z, context.world.random.nextInt(25) / 24D, 0, 0);
        }
    }

    public static void handleBlockData(ClientboundContraptionBlockDataPacket pkt) {
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getEntity(pkt.entityId()) instanceof AbstractContraptionEntity entity) {
            CreateCompat.applyBlockData(entity.getContraption(), pkt.localPos(), pkt.data());
        }
    }

    /**
     * @return The playback of the album jukebox at the specified position in a contraption or <code>null</code> if it isn't playing
     */
    public static @Nullable AlbumPlayback getAlbumPlayback(int entityId, BlockPos localPos) {
        if (Minecraft.getInstance().level == null || !(Minecraft.getInstance().level.getEntity(entityId) instanceof AbstractContraptionEntity entity) || entity.getContraption() == null) {
            return null;
        }
        MutablePair<StructureBlockInfo, MovementContext> actor = entity.getContraption().getActorAt(localPos);
        return actor != null && actor.right != null && actor.right.temporaryData instanceof RecordPlayer player ? player : null;
    }

    private static class PlayingRadio {

        private final String url;
        private SoundInstance sound;

        private PlayingRadio(@Nullable String url) {
            this.url = url;
        }
    }

    /**
     * Plays the discs of a mounted jukebox track by track, the same way placed jukeboxes do.
     */
    private static class RecordPlayer implements AlbumPlayback {

        private final MovementContext context;
        private final boolean loop;
        private Tag discsTag;
        private List<ItemStack> discs;
        private int index;
        private int track;
        private SoundInstance sound;

        private RecordPlayer(MovementContext context, boolean album) {
            this.context = context;
            this.loop = album;
            this.discs = List.of();
        }

        /**
         * Changes the discs, which keeps the current track playing if its disc wasn't touched.
         */
        private void setDiscs(@Nullable Tag discsTag) {
            HolderLookup.Provider registries = this.context.world.registryAccess();
            List<ItemStack> discs;
            if (discsTag == null) {
                discs = List.of();
            } else if (this.loop) {
                NonNullList<ItemStack> items = NonNullList.withSize(ALBUM_JUKEBOX_SIZE, ItemStack.EMPTY);
                CompoundTag container = new CompoundTag();
                container.put("Items", discsTag);
                ContainerHelper.loadAllItems(container, items, registries);
                discs = items;
            } else {
                discs = discsTag instanceof CompoundTag tag ? List.of(ItemStack.parseOptional(registries, tag)) : List.of();
            }

            boolean keepPlaying = this.sound != null && this.index < discs.size() && ItemStack.matches(this.discs.get(this.index), discs.get(this.index));
            this.discsTag = discsTag;
            this.discs = discs;
            if (!keepPlaying) {
                this.stop();
                this.track = 0;
                this.play();
            }
        }

        private @Nullable Vec3 getPosition() {
            AbstractContraptionEntity entity = this.context.contraption.entity;
            return entity.isAlive() && this.context.temporaryData == this ? entity.toGlobalVector(Vec3.atCenterOf(this.context.localPos), 1.0F) : null;
        }

        private void play() {
            HolderLookup.Provider registries = this.context.world.registryAccess();
            int attempts = this.discs.size();
            for (ItemStack disc : this.discs) {
                attempts += PlayableRecord.getTrackCount(registries, disc);
            }

            // Skip empty slots and tracks that can't be played, giving up once everything was tried
            for (int i = 0; i <= attempts; i++) {
                if (this.index >= this.discs.size()) {
                    if (!this.loop || this.discs.isEmpty()) {
                        break;
                    }
                    this.index = 0;
                }

                ItemStack disc = this.discs.get(this.index);
                if (!PlayableRecord.isPlayableRecord(disc) || this.track >= PlayableRecord.getTrackCount(registries, disc)) {
                    this.index++;
                    this.track = 0;
                    continue;
                }

                Optional<SoundInstance> sound = PlayableRecord.createMovingSound(disc, registries, this::getPosition, this.track, 16);
                if (sound.isPresent()) {
                    this.sound = StopListeningSound.create(sound.get(), () -> Minecraft.getInstance().tell(this::onFinished));
                    applyJukeboxSettings(this.context, this.sound, this.loop);
                    Minecraft.getInstance().getSoundManager().play(this.sound);
                    return;
                }
                this.track++;
            }
            this.sound = null;
        }

        private void onFinished() {
            if (this.getPosition() == null) {
                return;
            }
            this.track++;
            this.play();
        }

        private void stop() {
            if (this.sound != null) {
                if (this.sound instanceof StopListeningSound listening) {
                    listening.stopListening();
                }
                Minecraft.getInstance().getSoundManager().stop(this.sound);
                this.sound = null;
            }
        }

        @Override
        public int getPlayingIndex() {
            return this.sound != null ? this.index : -1;
        }

        @Override
        public int getTrack() {
            return this.track;
        }

        @Override
        public void next() {
            if (this.sound == null) {
                return;
            }
            this.stop();
            this.track++;
            this.play();
        }

        @Override
        public void previous() {
            if (this.sound == null) {
                return;
            }
            this.stop();
            if (this.track > 0) {
                this.track--;
            } else {
                // Go back to the last track of the previous disc
                HolderLookup.Provider registries = this.context.world.registryAccess();
                for (int i = 0; i < this.discs.size(); i++) {
                    this.index = this.index > 0 ? this.index - 1 : this.discs.size() - 1;
                    ItemStack disc = this.discs.get(this.index);
                    if (PlayableRecord.isPlayableRecord(disc)) {
                        this.track = Math.max(0, PlayableRecord.getTrackCount(registries, disc) - 1);
                        break;
                    }
                }
            }
            this.play();
        }
    }
}
