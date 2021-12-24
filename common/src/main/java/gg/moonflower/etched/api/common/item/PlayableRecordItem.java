package gg.moonflower.etched.api.common.item;

import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.ClientboundPlayMinecartJukeboxMusicPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Denotes an item as having the capability of being played as a record item.
 *
 * @author Ocelot
 * @since 2.0.0
 */
public interface PlayableRecordItem {

    /**
     * Checks to see if this item can be played.
     *
     * @param stack The stack to check
     * @return Whether it can play
     */
    boolean canPlay(ItemStack stack);

    /**
     * Creates the sound for an entity.
     *
     * @param stack  The stack to play
     * @param entity The entity to play the sound for
     * @return The sound to play or nothing to error
     */
    Optional<SoundInstance> createEntitySound(ItemStack stack, Entity entity);

    /**
     * Checks to see if the specified stack can be played in a jukebox.
     *
     * @param stack The stack to check
     * @return Whether that stack can play
     */
    static boolean isPlayableRecord(ItemStack stack) {
        return stack.getItem() instanceof PlayableRecordItem && ((PlayableRecordItem) stack.getItem()).canPlay(stack);
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
     * Sends a packet to the client notifying them to begin playing an entity record.
     *
     * @param entity  The entity playing the record
     * @param record  The record to play
     * @param restart Whether to restart the track from the beginning or start a new playback
     */
    static void playEntityRecord(Entity entity, ItemStack record, boolean restart) {
        EtchedMessages.PLAY.sendToTracking(entity, new ClientboundPlayMinecartJukeboxMusicPacket(record, entity, restart));
    }

    /**
     * Sends a packet to the client notifying them to stop playing an entity record.
     *
     * @param entity The entity to stop playing records
     */
    static void stopEntityRecord(Entity entity) {
        EtchedMessages.PLAY.sendToTracking(entity, new ClientboundPlayMinecartJukeboxMusicPacket(entity));
    }
}
