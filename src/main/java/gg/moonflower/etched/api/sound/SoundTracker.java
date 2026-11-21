package gg.moonflower.etched.api.sound;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.common.block.AlbumJukeboxBlock;
import gg.moonflower.etched.common.block.RadioBlock;
import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import gg.moonflower.etched.common.blockentity.RadioBlockEntity;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.mixin.client.gui.GuiAccessor;
import gg.moonflower.etched.core.mixin.client.render.LevelRendererAccessor;
import gg.moonflower.etched.core.registry.EtchedTags;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.CommonLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.StorageSoundHandler;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleSupplier;

/**
 * Tracks entity sounds and all etched playing sounds for the client side.
 *
 * @author Ocelot
 * @since 2.0.0
 */
public class SoundTracker {

    private static final Int2ObjectArrayMap<SoundInstance> ENTITY_PLAYING_SOUNDS = new Int2ObjectArrayMap<>();
    private static final Set<String> FAILED_URLS = ConcurrentHashMap.newKeySet();
    private static final Map<RadioStreamKey, Long> RADIO_RETRY_AFTER = new ConcurrentHashMap<>();
    private static final Component RADIO = Component.translatable("sound_source." + Etched.MOD_ID + ".radio");
    private static final long RADIO_URL_CONFIRMATION_TIMEOUT_MS = 30_000L;
    private static final long RADIO_RETRY_DELAY_MS = 5_000L;
    private static @Nullable String pendingRadioUrl;
    private static boolean pendingRadioUrlChanged;
    private static long pendingRadioUrlDeadline;
    private static int radioCleanupTicks;

    private static void runOnClientThread(Runnable action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isSameThread()) {
            action.run();
        } else {
            minecraft.execute(action);
        }
    }

    static {
        NeoForge.EVENT_BUS.<ClientPlayerNetworkEvent.LoggingOut>addListener(event -> {
            FAILED_URLS.clear();
            RADIO_RETRY_AFTER.clear();
            clearPendingRadioUrlConfirmation(null);
            retireAllRadios();
        });
        NeoForge.EVENT_BUS.addListener(SoundTracker::onClientTickPost);
    }

    private static synchronized void setRecordPlayingNearby(CommonLevelAccessor level, BlockPos pos, boolean playing) {
        if (!playing || level.getBlockState(pos).is(EtchedTags.RECORD_PLAYERS)) {
            for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(3.0D))) {
                livingEntity.setRecordPlayingNearby(pos, playing);
            }
        }
    }

    private static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || ++radioCleanupTicks < 20) {
            return;
        }

        radioCleanupTicks = 0;
        long now = System.currentTimeMillis();
        RADIO_RETRY_AFTER.entrySet().removeIf(entry -> entry.getValue() <= now);
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) minecraft.levelRenderer).getPlayingJukeboxSongs();
        Iterator<Map.Entry<BlockPos, SoundInstance>> iterator = playingRecords.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, SoundInstance> entry = iterator.next();
            if (!(entry.getValue() instanceof OnlineRecordSoundInstance onlineRecord) || !onlineRecord.isRadio()) {
                continue;
            }

            BlockEntity blockEntity = minecraft.level.getBlockEntity(entry.getKey());
            if (blockEntity instanceof RadioBlockEntity radio && Objects.equals(onlineRecord.getUrl(), radio.getUrl())) {
                continue;
            }

            iterator.remove();
            onlineRecord.retireRadio();
            setRecordPlayingNearby(minecraft.level, entry.getKey(), false);
        }
    }

    private static void retireAllRadios() {
        Minecraft minecraft = Minecraft.getInstance();
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) minecraft.levelRenderer).getPlayingJukeboxSongs();
        Iterator<Map.Entry<BlockPos, SoundInstance>> iterator = playingRecords.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, SoundInstance> entry = iterator.next();
            if (entry.getValue() instanceof OnlineRecordSoundInstance onlineRecord && onlineRecord.isRadio()) {
                iterator.remove();
                onlineRecord.retireRadio();
            }
        }
    }

    private static void retireRadio(CommonLevelAccessor level, BlockPos pos, Map<BlockPos, SoundInstance> playingRecords) {
        SoundInstance soundInstance = playingRecords.remove(pos);
        if (soundInstance == null) {
            return;
        }

        if (soundInstance instanceof StopListeningSound stopListeningSound) {
            stopListeningSound.stopListening();
        }
        if (soundInstance instanceof OnlineRecordSoundInstance onlineRecord && onlineRecord.isRadio()) {
            onlineRecord.retireRadio();
        } else {
            Minecraft.getInstance().getSoundManager().stop(soundInstance);
        }
        setRecordPlayingNearby(level, pos, false);
    }

    /**
     * Retires the radio stream tracked at the specified position.
     *
     * <p>The sound is muted immediately while its network buffer is allowed to finish
     * asynchronously, preserving the freeze-free shutdown behaviour.</p>
     *
     * @return whether a live radio stream was retired
     */
    public static boolean retireRadioStream(CommonLevelAccessor level, BlockPos pos) {
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingJukeboxSongs();
        SoundInstance sound = playingRecords.get(pos);
        if (!(sound instanceof OnlineRecordSoundInstance onlineRecord) || !onlineRecord.isRadio()) {
            return false;
        }
        retireRadio(level, pos, playingRecords);
        return true;
    }

    /**
     * Retrieves the sound instance for the specified entity id.
     *
     * @param entity The id of the entity to get a sound for
     * @return The sound for that entity
     */
    @Nullable
    public static SoundInstance getEntitySound(int entity) {
        return ENTITY_PLAYING_SOUNDS.get(entity);
    }

    /**
     * Sets the playing sound for the specified entity.
     *
     * @param entity   The id of the entity to play a sound for
     * @param instance The new sound to play or <code>null</code> to stop
     */
    public static void setEntitySound(int entity, @Nullable SoundInstance instance) {
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        if (instance == null) {
            SoundInstance old = ENTITY_PLAYING_SOUNDS.remove(entity);
            if (old != null) {
                if (old instanceof StopListeningSound) {
                    ((StopListeningSound) old).stopListening();
                }
                soundManager.stop(old);
            }
        } else {
            ENTITY_PLAYING_SOUNDS.put(entity, instance);
            soundManager.play(instance);
        }
    }

    /**
     * Creates an online sound for the specified entity.
     *
     * @param url                 The url to play
     * @param title               The title of the record
     * @param entity              The entity to play for
     * @param attenuationDistance The attenuation distance of the sound
     * @param stream              Whether to play a stream or regular file
     * @return A new sound instance
     */
    public static @Nullable AbstractOnlineSoundInstance getEtchedRecord(String url, Component title, Entity entity, int attenuationDistance, boolean stream) {
        if (!TrackData.isValidURL(url) || FAILED_URLS.contains(url)) {
            return null;
        }

        BlockState aboveState = entity.level().getBlockState(entity.blockPosition().above());
        boolean muffled = aboveState.is(BlockTags.WOOL);
        boolean hidden = !aboveState.isAir();

        return new OnlineRecordSoundInstance(url, entity, muffled ? 2.0F : 4.0F, muffled ? attenuationDistance / 2 : attenuationDistance,
                new MusicDownloadListener(title, entity::getX, entity::getY, entity::getZ) {
                    @Override
                    public void onSuccess() {
                        if (!entity.isAlive() || !ENTITY_PLAYING_SOUNDS.containsKey(entity.getId())) {
                            this.clearComponent();
                        } else {
                            if (!hidden && PlayableRecord.canShowMessage(entity.getX(), entity.getY(), entity.getZ())) {
                                Minecraft.getInstance().gui.setNowPlaying(title);
                            }
                        }
                    }

                    @Override
                    public void onFail() {
                        Minecraft.getInstance().gui.setOverlayMessage(Component.translatable("record." + Etched.MOD_ID + ".downloadFail", title), true);
                        FAILED_URLS.add(url);
                    }
                }, stream ? AudioSource.AudioFileType.STREAM : AudioSource.AudioFileType.FILE);
    }

    /**
     * Creates an online sound for the specified entity with the default attenuation distance.
     *
     * @param url    The url to play
     * @param title  The title of the record
     * @param entity The entity to play for
     * @param stream Whether to play a stream or regular file
     * @return A new sound instance
     */
    public static @Nullable AbstractOnlineSoundInstance getEtchedRecord(String url, Component title, Entity entity, boolean stream) {
        return getEtchedRecord(url, title, entity, 16, stream);
    }

    /**
     * Creates an online sound for the specified position.
     *
     * @param url                 The url to play
     * @param title               The title of the record
     * @param level               The level to play the record in
     * @param pos                 The position of the record
     * @param attenuationDistance The attenuation distance of the sound
     * @param type                The type of audio to accept
     * @return A new sound instance
     */
    public static @Nullable AbstractOnlineSoundInstance getEtchedRecord(String url, Component title, CommonLevelAccessor level, BlockPos pos, int attenuationDistance, AudioSource.AudioFileType type) {
        return getEtchedRecord(url, title, level, pos, 4.0F, 2.0F, attenuationDistance, 2, type);
    }

    private static @Nullable AbstractOnlineSoundInstance getEtchedRecord(String url, Component title, CommonLevelAccessor level, BlockPos pos,
                                                                         float volume, float muffledVolume, int attenuationDistance,
                                                                         int muffledRangeDivisor, AudioSource.AudioFileType type) {
        if (!TrackData.isValidURL(url) || FAILED_URLS.contains(url)) {
            return null;
        }

        BlockState aboveState = level.getBlockState(pos.above());
        boolean muffled = aboveState.is(BlockTags.WOOL);
        boolean hidden = !aboveState.isAir();

        int effectiveDistance = muffled ? Math.max(1, attenuationDistance / muffledRangeDivisor) : attenuationDistance;
        return new OnlineRecordSoundInstance(url, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F,
                muffled ? muffledVolume : volume, effectiveDistance,
                new MusicDownloadListener(title, () -> pos.getX() + 0.5, () -> pos.getY() + 0.5, () -> pos.getZ() + 0.5) {
                    @Override
                    public void onSuccess() {
                        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingJukeboxSongs();
                        if (!playingRecords.containsKey(pos)) {
                            this.clearComponent();
                        } else {
                            if (!hidden && PlayableRecord.canShowMessage(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                                Minecraft.getInstance().gui.setNowPlaying(title);
                            }
                            setRecordPlayingNearby(level, pos, true);
                        }
                    }

                    @Override
                    public void onFail() {
                        this.clearComponent();
                        Minecraft.getInstance().gui.setOverlayMessage(Component.translatable("record." + Etched.MOD_ID + ".downloadFail", title), true);
                        FAILED_URLS.add(url);
                    }
                }, type);
    }

    private static @Nullable AbstractOnlineSoundInstance getRadioRecord(String url, Component title, CommonLevelAccessor level,
                                                                        BlockPos pos, int volumePercent, int range,
                                                                        AudioSource.AudioFileType type) {
        float volume = Math.max(0, Math.min(100, volumePercent)) / 100.0F;
        int clampedRange = Math.max(RadioBlockEntity.MIN_RANGE, Math.min(RadioBlockEntity.MAX_RANGE, range));
        boolean muffled = level.getBlockState(pos.above()).is(BlockTags.WOOL);
        float effectiveVolume = muffled ? volume * 0.5F : volume;
        int effectiveRange = muffled ? Math.max(1, clampedRange / 4) : clampedRange;
        RadioDownloadListener listener = new RadioDownloadListener(url, title, level, pos);
        OnlineRecordSoundInstance record = new OnlineRecordSoundInstance(url,
                pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F,
                effectiveVolume, RadioBlockEntity.MAX_RANGE, listener, type);
        listener.attach(record);
        record.setRadioSettings(effectiveVolume, effectiveRange);
        return record;
    }

    public static void updateRadioSettings(CommonLevelAccessor level, BlockPos pos, int volumePercent, int range) {
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingJukeboxSongs();
        SoundInstance sound = playingRecords.get(pos);
        if (sound instanceof OnlineRecordSoundInstance onlineRecord) {
            float volume = Math.max(0, Math.min(100, volumePercent)) / 100.0F;
            int clampedRange = Math.max(RadioBlockEntity.MIN_RANGE, Math.min(RadioBlockEntity.MAX_RANGE, range));
            boolean muffled = level.getBlockState(pos.above()).is(BlockTags.WOOL);
            onlineRecord.setRadioSettings(muffled ? volume * 0.5F : volume,
                    muffled ? Math.max(1, clampedRange / 4) : clampedRange);
        }
    }

    /**
     * Mutes or unmutes an existing radio without destroying its live network stream.
     *
     * @return whether a live radio instance existed at this position
     */
    public static boolean setRadioMuted(CommonLevelAccessor level, BlockPos pos, boolean muted) {
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingJukeboxSongs();
        SoundInstance sound = playingRecords.get(pos);
        if (sound instanceof OnlineRecordSoundInstance onlineRecord) {
            onlineRecord.setRadioMuted(muted);
            if (muted) {
                setRecordPlayingNearby(level, pos, false);
            }
            return true;
        }
        return false;
    }

    /**
     * Checks whether this client still owns a usable stream for the radio at {@code pos}.
     *
     * <p>The block entity is persistent server state, while the sound instance only lives for the
     * current client connection. Keeping the two separate is important: reconnecting to a server
     * must create a fresh stream even when the radio's URL and powered state never changed.</p>
     */
    public static boolean hasRadioStream(BlockPos pos, @Nullable String url) {
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingJukeboxSongs();
        SoundInstance sound = playingRecords.get(pos);
        return sound instanceof OnlineRecordSoundInstance onlineRecord
                && onlineRecord.isRadio()
                && onlineRecord.isUsableStream()
                && (onlineRecord.isStreamConnecting()
                || onlineRecord.isWithinActivationGracePeriod()
                || Minecraft.getInstance().getSoundManager().isActive(sound))
                && Objects.equals(onlineRecord.getUrl(), url);
    }

    /**
     * Returns the squared distance between the client listener and a radio block center.
     */
    public static double getRadioListenerDistanceSquared(BlockPos pos) {
        if (Minecraft.getInstance().player == null) {
            return Double.POSITIVE_INFINITY;
        }
        return Minecraft.getInstance().player.distanceToSqr(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        );
    }

    /**
     * Requests a one-shot confirmation for the player currently editing a radio URL.
     * Automatic stream starts never call this method and therefore remain silent.
     */
    public static synchronized void requestRadioUrlConfirmation(@Nullable String previousUrl, @Nullable String newUrl) {
        if (newUrl == null || newUrl.isEmpty() || Objects.equals(previousUrl, newUrl)) {
            clearPendingRadioUrlConfirmation(null);
            return;
        }

        pendingRadioUrl = newUrl;
        pendingRadioUrlChanged = previousUrl != null && !previousUrl.isEmpty();
        pendingRadioUrlDeadline = System.currentTimeMillis() + RADIO_URL_CONFIRMATION_TIMEOUT_MS;
    }

    private static void confirmPendingRadioUrl(String url) {
        Component confirmation;
        synchronized (SoundTracker.class) {
            if (!Objects.equals(pendingRadioUrl, url) || System.currentTimeMillis() > pendingRadioUrlDeadline) {
                return;
            }

            confirmation = Component.translatable(pendingRadioUrlChanged
                    ? "message.etched.radio.url_changed"
                    : "message.etched.radio.url_saved");
            pendingRadioUrl = null;
            pendingRadioUrlDeadline = 0L;
        }

        Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.setOverlayMessage(confirmation, false));
    }

    private static synchronized void clearPendingRadioUrlConfirmation(@Nullable String url) {
        if (url != null && !Objects.equals(pendingRadioUrl, url)) {
            return;
        }
        pendingRadioUrl = null;
        pendingRadioUrlDeadline = 0L;
    }

    private static synchronized boolean consumePendingRadioUrlFailure(String url) {
        if (!Objects.equals(pendingRadioUrl, url) || System.currentTimeMillis() > pendingRadioUrlDeadline) {
            return false;
        }
        pendingRadioUrl = null;
        pendingRadioUrlDeadline = 0L;
        return true;
    }

    /**
     * Creates an online sound for the specified position with the default attenuation distance.
     *
     * @param url   The url to play
     * @param title The title of the record
     * @param level The level to play the record in
     * @param pos   The position of the record
     * @param type  The type of audio to accept
     * @return A new sound instance
     */
    public static @Nullable AbstractOnlineSoundInstance getEtchedRecord(String url, Component title, CommonLevelAccessor level, BlockPos pos, AudioSource.AudioFileType type) {
        return getEtchedRecord(url, title, level, pos, 16, type);
    }

    private static void playRecord(BlockPos pos, SoundInstance sound) {
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingJukeboxSongs();
        playingRecords.put(pos, sound);
        soundManager.play(sound);
    }

    private static void playNextRecord(CommonLevelAccessor level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AlbumJukeboxBlockEntity jukebox)) {
            return;
        }

        jukebox.next();
        playAlbum(jukebox, blockEntity.getBlockState(), level, pos, true);
    }

    public static void playBlockRecord(BlockPos pos, TrackData[] tracks, int track) {
        playBlockRecord(pos, tracks, track, null);
    }

    public static void playBlockRecord(BlockPos pos, TrackData[] tracks, int track, @Nullable UUID storageId) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        if (track >= tracks.length) {
            setRecordPlayingNearby(level, pos, false);
            return;
        }

        TrackData trackData = tracks[track];
        String url = trackData.url();
        if (!TrackData.isValidURL(url) || FAILED_URLS.contains(url)) {
            playBlockRecord(pos, tracks, track + 1, storageId);
            return;
        }

        StopListeningSound sound = StopListeningSound.create(getEtchedRecord(url, trackData.getDisplayName(), level, pos, AudioSource.AudioFileType.FILE), () -> Minecraft.getInstance().tell(() -> {
            if (!((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingJukeboxSongs().containsKey(pos)) {
                return;
            }
            playBlockRecord(pos, tracks, track + 1, storageId);
        }));
        if (Etched.SOPHSTICATED_CORE_LOADED && storageId != null) {
            StorageSoundHandler.playStorageSound(storageId, sound);
        } else {
            playRecord(pos, sound);
        }
    }

    /**
     * Plays a record stack for an entity.
     *
     * @param record              The record to play
     * @param entityId            The id of the entity to play the record at
     * @param track               The track to play
     * @param attenuationDistance The attenuation distance of the sound
     * @param loop                Whether to loop
     */
    public static void playEntityRecord(ItemStack record, int entityId, int track, int attenuationDistance, boolean loop) {
        playEntityRecord(record, entityId, track, attenuationDistance, loop, null);
    }

    /**
     * Plays a record stack for an entity.
     *
     * @param record              The record to play
     * @param entityId            The id of the entity to play the record at
     * @param track               The track to play
     * @param attenuationDistance The attenuation distance of the sound
     * @param loop                Whether to loop
     */
    public static void playEntityRecord(ItemStack record, int entityId, int track, int attenuationDistance, boolean loop, @Nullable UUID storageId) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        Entity entity = level.getEntity(entityId);
        if (entity == null) {
            return;
        }

        Optional<? extends SoundInstance> sound = PlayableRecord.createEntitySound(record, entity, track, attenuationDistance);
        if (sound.isEmpty()) {
            if (loop && track != 0) {
                playEntityRecord(record, entityId, 0, attenuationDistance, true, storageId);
            }
            return;
        }

        SoundInstance entitySound = ENTITY_PLAYING_SOUNDS.remove(entity.getId());
        if (entitySound != null) {
            if (entitySound instanceof StopListeningSound) {
                ((StopListeningSound) entitySound).stopListening();
            }
            Minecraft.getInstance().getSoundManager().stop(entitySound);
        }

        entitySound = StopListeningSound.create(sound.get(), () -> Minecraft.getInstance().tell(() -> {
            ENTITY_PLAYING_SOUNDS.remove(entityId);
            playEntityRecord(record, entityId, track + 1, attenuationDistance, loop, storageId);
        }));

        if (Etched.SOPHSTICATED_CORE_LOADED && storageId != null) {
            StorageSoundHandler.playStorageSound(storageId, entitySound);
        } else {
            ENTITY_PLAYING_SOUNDS.put(entityId, entitySound);
            Minecraft.getInstance().getSoundManager().play(entitySound);
        }
    }

    public static void playEntityRecord(ItemStack record, int entityId, int track, boolean loop) {
        SoundTracker.playEntityRecord(record, entityId, track, 16, loop);
    }

    /**
     * Plays a record stack for an entity with a boombox.
     *
     * @param entityId The id of the entity to play the record at
     * @param record   The record to play
     */
    public static void playBoombox(int entityId, ItemStack record) {
        setEntitySound(entityId, null);
        if (!record.isEmpty()) {
            playEntityRecord(record, entityId, 0, 8, true);
        }
    }

    /**
     * Plays the records on an album jukebox in order.
     *
     * @param url   The URL of the stream
     * @param state The block state of the radio
     * @param level The level to play records in
     * @param pos   The position of the jukebox
     */
    public static void playRadio(@Nullable String url, BlockState state, CommonLevelAccessor level, BlockPos pos) {
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingJukeboxSongs();
        retireRadio(level, pos, playingRecords);

        if (url == null) {
            return;
        }
        RadioStreamKey streamKey = new RadioStreamKey(pos, url);
        Long retryAfter = RADIO_RETRY_AFTER.get(streamKey);
        if (retryAfter != null && retryAfter > System.currentTimeMillis()) {
            return;
        }
        RADIO_RETRY_AFTER.remove(streamKey);
        if (!state.hasProperty(RadioBlock.POWERED) || state.getValue(RadioBlock.POWERED)) { // Something must already be playing since it would otherwise be -1 and a change would occur
            return;
        }

        int volumePercent = RadioBlockEntity.DEFAULT_VOLUME_PERCENT;
        int range = RadioBlockEntity.DEFAULT_RANGE;
        if (level.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
            volumePercent = radio.getVolumePercent();
            range = radio.getRange();
        }

        if (volumePercent > 0 && TrackData.isValidURL(url)) {
            AbstractOnlineSoundInstance record = getRadioRecord(url, RADIO, level, pos, volumePercent, range, AudioSource.AudioFileType.BOTH);
            if (record == null) {
                return;
            }
            // A live radio stream is already endless. Marking it as a looping file makes
            // Minecraft wrap the network stream in LoopingAudioStream and can limit playback
            // to the initially buffered audio.
            record.setLoop(false);
            playRecord(pos, record); // Get the new block state
        }
    }

    private record RadioStreamKey(BlockPos pos, String url) {
    }

    /**
     * Plays the records on an album jukebox in order.
     *
     * @param jukebox The jukebox to play records
     * @param level   The level to play records in
     * @param pos     The position of the jukebox
     * @param force   Whether to force the jukebox to play
     */
    public static void playAlbum(AlbumJukeboxBlockEntity jukebox, BlockState state, CommonLevelAccessor level, BlockPos pos, boolean force) {
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingJukeboxSongs();

        if (!state.hasProperty(AlbumJukeboxBlock.POWERED) || !state.getValue(AlbumJukeboxBlock.POWERED) && !force &&
                !jukebox.recalculatePlayingIndex(false)) {// Something must already be playing since it would otherwise be -1 and a change would occur
            return;
        }

        SoundInstance soundInstance = playingRecords.get(pos);
        if (soundInstance != null) {
            if (soundInstance instanceof StopListeningSound) {
                ((StopListeningSound) soundInstance).stopListening();
            }
            soundManager.stop(soundInstance);
            playingRecords.remove(pos);
            setRecordPlayingNearby(level, pos, false);
        }

        if (state.getValue(AlbumJukeboxBlock.POWERED)) {
            jukebox.stopPlaying();
        }

        if (jukebox.getPlayingIndex() < 0) {// Nothing can be played inside the jukebox
            return;
        }

        ItemStack disc = jukebox.getItem(jukebox.getPlayingIndex());
        int trackCount = PlayableRecord.getTrackCount(level.registryAccess(), disc);
        int track = jukebox.getTrack() < 0 || jukebox.getTrack() >= trackCount ? 0 : jukebox.getTrack();

        PlayableRecord.createBlockSound(disc, level, pos, track).ifPresent(sound -> {
            playRecord(pos, StopListeningSound.create(sound, () -> Minecraft.getInstance().tell(() -> playNextRecord(level, pos))));
            setRecordPlayingNearby(level, pos, true);
        });
    }

    private static class DownloadTextComponent implements Component {

        private ComponentContents contents;
        private FormattedCharSequence visualOrderText;
        private Language decomposedWith;

        public DownloadTextComponent() {
            this.contents = PlainTextContents.EMPTY;
            this.visualOrderText = FormattedCharSequence.EMPTY;
            this.decomposedWith = null;
        }

        @Override
        public ComponentContents getContents() {
            return this.contents;
        }

        @Override
        public List<Component> getSiblings() {
            return Collections.emptyList();
        }

        @Override
        public Style getStyle() {
            return Style.EMPTY;
        }

        @Override
        public FormattedCharSequence getVisualOrderText() {
            Language language = Language.getInstance();
            if (this.decomposedWith != language) {
                this.visualOrderText = language.getVisualOrder(this);
                this.decomposedWith = language;
            }

            return this.visualOrderText;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }

            DownloadTextComponent that = (DownloadTextComponent) o;
            return this.contents.equals(that.contents);
        }

        @Override
        public int hashCode() {
            return this.contents.hashCode();
        }

        @Override
        public String toString() {
            return this.contents.toString();
        }

        public void setText(String text) {
            this.contents = new PlainTextContents.LiteralContents(text);
            this.decomposedWith = null;
        }
    }

    /**
     * Radio streams load silently in the background. The listener also owns the
     * short retry cooldown, so one broken request cannot poison the URL for the
     * entire client session.
     */
    private static final class RadioDownloadListener extends MusicDownloadListener {

        private final String url;
        private final Component title;
        private final CommonLevelAccessor level;
        private final BlockPos pos;
        private final RadioStreamKey streamKey;
        private @Nullable OnlineRecordSoundInstance record;

        private RadioDownloadListener(String url, Component title, CommonLevelAccessor level, BlockPos pos) {
            super(title, () -> pos.getX() + 0.5, () -> pos.getY() + 0.5, () -> pos.getZ() + 0.5);
            this.url = url;
            this.title = title;
            this.level = level;
            this.pos = pos;
            this.streamKey = new RadioStreamKey(pos, url);
        }

        private void attach(OnlineRecordSoundInstance record) {
            this.record = record;
        }

        @Override
        public void progressStartRequest(Component component) {
        }

        @Override
        public void progressStartDownload(float size) {
        }

        @Override
        public void progressStagePercentage(int percentage) {
        }

        @Override
        public void progressStartLoading() {
        }

        @Override
        public void onSuccess() {
            OnlineRecordSoundInstance currentRecord = this.record;
            if (currentRecord == null) {
                return;
            }

            Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingJukeboxSongs();
            if (playingRecords.get(this.pos) != currentRecord) {
                return;
            }

            RADIO_RETRY_AFTER.remove(this.streamKey);
            confirmPendingRadioUrl(this.url);
            setRecordPlayingNearby(this.level, this.pos, true);
        }

        @Override
        public void onFail() {
            OnlineRecordSoundInstance currentRecord = this.record;
            Map<BlockPos, SoundInstance> playingRecords = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getPlayingJukeboxSongs();
            if (currentRecord != null && playingRecords.get(this.pos) == currentRecord) {
                playingRecords.remove(this.pos);
                currentRecord.retireRadio();
                setRecordPlayingNearby(this.level, this.pos, false);
            }

            RADIO_RETRY_AFTER.put(this.streamKey, System.currentTimeMillis() + RADIO_RETRY_DELAY_MS);
            if (consumePendingRadioUrlFailure(this.url)) {
                Minecraft.getInstance().gui.setOverlayMessage(Component.translatable("record." + Etched.MOD_ID + ".downloadFail", this.title), true);
            }
        }
    }

    private static abstract class MusicDownloadListener implements DownloadProgressListener {

        private final Component title;
        private final DoubleSupplier x;
        private final DoubleSupplier y;
        private final DoubleSupplier z;
        private final BlockPos.MutableBlockPos pos;
        private float size;
        private Component requesting;
        private DownloadTextComponent component;

        protected MusicDownloadListener(Component title, DoubleSupplier x, DoubleSupplier y, DoubleSupplier z) {
            this.title = title;
            this.x = x;
            this.y = y;
            this.z = z;
            this.pos = new BlockPos.MutableBlockPos();
        }

        private BlockPos.MutableBlockPos getPos() {
            return this.pos.set(this.x.getAsDouble(), this.y.getAsDouble(), this.z.getAsDouble());
        }

        private void setComponent(Component text) {
            if (this.component == null && (Minecraft.getInstance().level == null || !Minecraft.getInstance().level.getBlockState(this.getPos().move(Direction.UP)).isAir() ||
                    !PlayableRecord.canShowMessage(this.x.getAsDouble(), this.y.getAsDouble(), this.z.getAsDouble()))) {
                return;
            }

            if (this.component == null) {
                this.component = new DownloadTextComponent();
                Minecraft.getInstance().gui.setOverlayMessage(this.component, true);
                ((GuiAccessor) Minecraft.getInstance().gui).setOverlayMessageTime(Short.MAX_VALUE);
            }
            this.component.setText(text.getString());
        }

        protected void clearComponent() {
            runOnClientThread(() -> {
                if (((GuiAccessor) Minecraft.getInstance().gui).getOverlayMessageString() == this.component) {
                    ((GuiAccessor) Minecraft.getInstance().gui).setOverlayMessageTime(60);
                    this.component = null;
                }
            });
        }

        @Override
        public void progressStartRequest(Component component) {
            runOnClientThread(() -> {
                this.requesting = component;
                this.setComponent(component);
            });
        }

        @Override
        public void progressStartDownload(float size) {
            runOnClientThread(() -> {
                this.size = size;
                this.requesting = null;
                this.updateProgressComponent(0);
            });
        }

        @Override
        public void progressStagePercentage(int percentage) {
            runOnClientThread(() -> this.updateProgressComponent(percentage));
        }

        private void updateProgressComponent(int percentage) {
            if (this.requesting != null) {
                this.setComponent(this.requesting.copy().append(" " + percentage + "%"));
            } else if (this.size != 0) {
                this.setComponent(Component.translatable("record." + Etched.MOD_ID + ".downloadProgress", String.format(Locale.ROOT, "%.2f", percentage / 100.0F * this.size),
                        String.format(Locale.ROOT, "%.2f", this.size), this.title));
            }
        }

        @Override
        public void progressStartLoading() {
            runOnClientThread(() -> {
                this.requesting = null;
                this.setComponent(Component.translatable("record." + Etched.MOD_ID + ".loading", this.title));
            });
        }

        @Override
        public void onFail() {
            runOnClientThread(() -> Minecraft.getInstance().gui.setOverlayMessage(Component.translatable("record." + Etched.MOD_ID + ".downloadFail", this.title), true));
        }
    }
}
