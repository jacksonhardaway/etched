package gg.moonflower.etched.core.compat.create;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import gg.moonflower.etched.common.menu.AlbumJukeboxMenu;
import gg.moonflower.etched.core.Etched;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;

/**
 * Opens the album jukebox menu for a jukebox on a moving contraption so its discs and settings can be changed without stopping.
 */
public class AlbumJukeboxMovingInteraction extends MovingInteractionBehaviour {

    private static final Component CONTAINER_TITLE = Component.translatable("container." + Etched.MOD_ID + ".album_jukebox");

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        if (player.level().isClientSide()) {
            return true;
        }

        CompoundTag data = CreateCompat.getBlockData(contraptionEntity, localPos);
        float volume = CreateCompat.getVolume(data);
        int radius = CreateCompat.getRadius(data, AlbumJukeboxBlockEntity.DEFAULT_RADIUS);
        player.openMenu(new SimpleMenuProvider((menuId, playerInventory, menuPlayer) ->
                new AlbumJukeboxMenu(menuId, playerInventory, new ContraptionAlbumJukeboxContainer(contraptionEntity, localPos), localPos), CONTAINER_TITLE), buf -> {
            buf.writeBlockPos(localPos);
            buf.writeFloat(volume);
            buf.writeVarInt(radius);
            buf.writeVarInt(contraptionEntity.getId());
        });
        return true;
    }
}
