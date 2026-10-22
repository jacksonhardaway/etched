package gg.moonflower.etched.core.compat.create;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import dev.ryanhcode.sable.companion.SableCompanion;
import gg.moonflower.etched.common.block.RadioBlock;
import gg.moonflower.etched.common.menu.SoundSettingsMenu;
import gg.moonflower.etched.common.network.play.ClientboundContraptionBlockDataPacket;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Lets radios and jukeboxes keep playing, and be changed, while mounted on Create contraptions such as trains.
 */
public class CreateCompat {

    private static final double MAX_USE_DISTANCE_SQ = 8.0 * 8.0;
    private static final Field UPDATE_TAGS = findUpdateTags();

    private static @Nullable Field findUpdateTags() {
        try {
            Field field = Contraption.class.getDeclaredField("updateTags");
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            Etched.LOGGER.warn("Failed to access Create contraption update tags, players loading a contraption may hear its previous music", e);
            return null;
        }
    }

    public static void load() {
        MovementBehaviour.REGISTRY.register(EtchedBlocks.RADIO.get(), new RadioMovementBehaviour());
        MovingInteractionBehaviour.REGISTRY.register(EtchedBlocks.RADIO.get(), new RadioMovingInteraction());
        MovementBehaviour.REGISTRY.register(EtchedBlocks.ALBUM_JUKEBOX.get(), new JukeboxMovementBehaviour(true));
        MovingInteractionBehaviour.REGISTRY.register(EtchedBlocks.ALBUM_JUKEBOX.get(), new AlbumJukeboxMovingInteraction());
        // Create Connected adds its own behaviour for vanilla jukeboxes and only one can be registered
        if (!ModList.get().isLoaded("create_connected") && MovementBehaviour.REGISTRY.get(Blocks.JUKEBOX) == null) {
            MovementBehaviour.REGISTRY.register(Blocks.JUKEBOX, new JukeboxMovementBehaviour(false));
        }
    }

    /**
     * @return The URL the radio should currently play or <code>null</code> if it is silent
     */
    public static @Nullable String getPlayingUrl(BlockState state, @Nullable CompoundTag blockEntityData) {
        if (state.hasProperty(RadioBlock.POWERED) && state.getValue(RadioBlock.POWERED)) {
            return null;
        }
        if (blockEntityData == null || !blockEntityData.contains("Url", Tag.TAG_STRING)) {
            return null;
        }
        String url = blockEntityData.getString("Url");
        return StringUtil.isNullOrEmpty(url) ? null : url;
    }

    /**
     * @return Whether the player can still use the menu of the block at the specified position in the contraption
     */
    public static boolean canUse(AbstractContraptionEntity entity, BlockPos localPos, Block block, Player player) {
        if (!entity.isAlive() || entity.getContraption() == null) {
            return false;
        }
        StructureBlockInfo info = entity.getContraption().getBlocks().get(localPos);
        return info != null && info.state().is(block) && SableCompanion.INSTANCE.distanceSquaredWithSubLevels(player.level(), player.position(), entity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0F)) <= MAX_USE_DISTANCE_SQ;
    }

    /**
     * @return The volume of a mounted radio or jukebox from 0 to 1
     */
    public static float getVolume(@Nullable CompoundTag blockEntityData) {
        return blockEntityData != null && blockEntityData.contains("Volume", Tag.TAG_FLOAT) ? Mth.clamp(blockEntityData.getFloat("Volume"), 0.0F, 1.0F) : 1.0F;
    }

    /**
     * @return How far away a mounted radio or jukebox can be heard in blocks
     */
    public static int getRadius(@Nullable CompoundTag blockEntityData, int defaultRadius) {
        return blockEntityData != null && blockEntityData.contains("Radius", Tag.TAG_INT) ? Mth.clamp(blockEntityData.getInt("Radius"), SoundSettingsMenu.MIN_RADIUS, SoundSettingsMenu.MAX_RADIUS) : defaultRadius;
    }

    /**
     * @return The stored block entity data of the block at the specified position in the contraption
     */
    public static @Nullable CompoundTag getBlockData(AbstractContraptionEntity entity, BlockPos localPos) {
        StructureBlockInfo info = entity.getContraption() != null ? entity.getContraption().getBlocks().get(localPos) : null;
        return info != null ? info.nbt() : null;
    }

    public static void setVolume(AbstractContraptionEntity entity, BlockPos localPos, float volume) {
        CompoundTag data = new CompoundTag();
        data.putFloat("Volume", Mth.clamp(volume, 0.0F, 1.0F));
        updateBlockData(entity, localPos, data);
    }

    public static void setRadius(AbstractContraptionEntity entity, BlockPos localPos, int radius) {
        CompoundTag data = new CompoundTag();
        data.putInt("Radius", Mth.clamp(radius, SoundSettingsMenu.MIN_RADIUS, SoundSettingsMenu.MAX_RADIUS));
        updateBlockData(entity, localPos, data);
    }

    /**
     * Changes the stored block entity data of a mounted block on the server and tells every client tracking the contraption.
     *
     * @param data The data that changed, merged into the stored data
     */
    public static void updateBlockData(AbstractContraptionEntity entity, BlockPos localPos, CompoundTag data) {
        if (entity.isAlive() && applyBlockData(entity.getContraption(), localPos, data)) {
            PacketDistributor.sendToPlayersTrackingEntity(entity, new ClientboundContraptionBlockDataPacket(entity.getId(), localPos, data));
        }
    }

    /**
     * Merges data into every copy of a mounted block's data so it is kept when the contraption is saved, sent to new players or disassembled.
     */
    static boolean applyBlockData(@Nullable Contraption contraption, BlockPos localPos, CompoundTag data) {
        if (contraption == null) {
            return false;
        }

        StructureBlockInfo info = contraption.getBlocks().get(localPos);
        if (info == null) {
            return false;
        }

        if (info.nbt() == null) {
            info = new StructureBlockInfo(info.pos(), info.state(), new CompoundTag());
            contraption.getBlocks().put(localPos, info);
        }
        info.nbt().merge(data);

        // Clients that start tracking the contraption later are sent the update tags instead of the block data
        CompoundTag updateTag = getUpdateTag(contraption, localPos);
        if (updateTag != null) {
            updateTag.merge(data);
        }

        // Actors may hold their own copy of the data
        MutablePair<StructureBlockInfo, MovementContext> actor = contraption.getActorAt(localPos);
        if (actor != null) {
            if (actor.left.nbt() != null) {
                actor.left.nbt().merge(data);
            }
            MovementContext context = actor.right;
            if (context != null) {
                if (context.blockEntityData == null) {
                    context.blockEntityData = info.nbt();
                }
                context.blockEntityData.merge(data);
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    static @Nullable CompoundTag getUpdateTag(Contraption contraption, BlockPos localPos) {
        if (UPDATE_TAGS == null) {
            return null;
        }
        try {
            return ((Map<BlockPos, CompoundTag>) UPDATE_TAGS.get(contraption)).get(localPos);
        } catch (Exception e) {
            return null;
        }
    }
}
