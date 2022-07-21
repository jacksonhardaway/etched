package gg.moonflower.etched.api.record;

import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.ClientboundPlayEntityMusicPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.net.Proxy;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Denotes an item as having the capability of being played as a record item.
 *
 * @author Ocelot
 * @since 2.0.0
 */
public interface PlayableRecord {
    
    /**
     * Checks to see if the specified stack can be played.
     *
     * @param stack The stack to check
     * @return Whether that stack can play
     */
    static boolean isPlayableRecord(ItemStack stack) {
        return stack.getItem() instanceof PlayableRecord && ((PlayableRecord) stack.getItem()).canPlay(stack);
    }

    /**
     * Checks to see if the local player is close enough to receive the record text.
     *
     * @param x The x position of the entity
     * @param y The y position of the entity
     * @param z The z position of the entity
     * @return Whether the player is within distance
     */
    static boolean canShowMessage(double x, double y, double z) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null || player.distanceToSqr(x, y, z) <= 4096.0;
    }

    /**
     * Displays the 'now playing' text on the screen.
     *
     * @param text The text to display as the record name
     */
    static void showMessage(Component text) {
        Minecraft.getInstance().gui.setNowPlaying(text);
    }

    /**
     * Sends a packet to the client notifying them to begin playing an entity record.
     *
     * @param entity  The entity playing the record
     * @param record  The record to play
     * @param restart Whether to restart the track from the beginning or start a new playback
     */
    static void playEntityRecord(Entity entity, ItemStack record, boolean restart) {
        EtchedMessages.PLAY.sendToTracking(entity, new ClientboundPlayEntityMusicPacket(record, entity, restart));
    }

    /**
     * Sends a packet to the client notifying them to stop playing an entity record.
     *
     * @param entity The entity to stop playing records
     */
    static void stopEntityRecord(Entity entity) {
        EtchedMessages.PLAY.sendToTracking(entity, new ClientboundPlayEntityMusicPacket(entity));
    }

    /**
     * Retrieves the music for the specified stack.
     *
     * @param stack The stack to check
     * @return The tracks on that record
     */
    static Optional<TrackData[]> getStackMusic(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PlayableRecord))
            return Optional.empty();
        return ((PlayableRecord) stack.getItem()).getMusic(stack);
    }

    /**
     * Retrieves the album music for the specified stack.
     *
     * @param stack The stack to check
     * @return The album track on that record
     */
    static Optional<TrackData> getStackAlbum(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PlayableRecord))
            return Optional.empty();
        return ((PlayableRecord) stack.getItem()).getAlbum(stack);
    }

    /**
     * Retrieves the number of tracks on the specified stack.
     *
     * @param stack The stack to check
     * @return The number of tracks on the record
     */
    static int getStackTrackCount(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PlayableRecord))
            return 0;
        return ((PlayableRecord) stack.getItem()).getTrackCount(stack);
    }

    /**
     * Checks to see if this item can be played.
     *
     * @param stack The stack to check
     * @return Whether it can play
     */
    default boolean canPlay(ItemStack stack) {
        return this.getMusic(stack).isPresent();
    }

    /**
     * Creates the sound for an entity.
     *
     * @param stack  The stack to play
     * @param entity The entity to play the sound for
     * @param track  The track to play on the disc
     * @return The sound to play or nothing to error
     */
    @Environment(EnvType.CLIENT)
    default Optional<? extends SoundInstance> createEntitySound(ItemStack stack, Entity entity, int track) {
        return track < 0 ? Optional.empty() : this.getMusic(stack).filter(tracks -> track < tracks.length).map(tracks -> SoundTracker.getEtchedRecord(tracks[track].getUrl(), tracks[track].getDisplayName(), entity, false));
    }

    /**
     * Retrieves the album cover for this item.
     *
     * @param stack The stack to get art for
     * @return A future for a potential cover
     */
    @Environment(EnvType.CLIENT)
    CompletableFuture<AlbumCover> getAlbumCover(ItemStack stack, Proxy proxy, ResourceManager resourceManager);

    /**
     * Retrieves the music URL from the specified stack.
     *
     * @param stack The stack to get NBT from
     * @return The optional URL for that item
     */
    Optional<TrackData[]> getMusic(ItemStack stack);

    /**
     * Retrieves the album data from the specified stack.
     *
     * @param stack The stack to get the album for
     * @return The album data or the first track if not an album
     */
    Optional<TrackData> getAlbum(ItemStack stack);

    /**
     * Retrieves the number of tracks in the specified stack.
     *
     * @param stack The stack to get tracks for
     * @return The number of tracks
     */
    int getTrackCount(ItemStack stack);
}
