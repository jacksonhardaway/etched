package gg.moonflower.etched.core.compat.create;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import gg.moonflower.etched.common.blockentity.RadioBlockEntity;
import gg.moonflower.etched.common.menu.RadioMenu;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/**
 * Opens the radio menu for a radio on a moving contraption so the station can be changed without stopping.
 */
public class RadioMovingInteraction extends MovingInteractionBehaviour {

    private static final Component CONTAINER_TITLE = Component.translatable("container." + Etched.MOD_ID + ".radio");

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        if (player.level().isClientSide()) {
            return true;
        }

        StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return false;
        }

        String url = info.nbt() != null ? info.nbt().getString("Url") : "";
        float volume = CreateCompat.getVolume(info.nbt());
        int radius = CreateCompat.getRadius(info.nbt(), RadioBlockEntity.DEFAULT_RADIUS);
        player.openMenu(new SimpleMenuProvider((menuId, playerInventory, menuPlayer) -> new RadioMenu(menuId,
                user -> CreateCompat.canUse(contraptionEntity, localPos, EtchedBlocks.RADIO.get(), user),
                newUrl -> {
                    CompoundTag data = new CompoundTag();
                    data.putString("Url", newUrl);
                    CreateCompat.updateBlockData(contraptionEntity, localPos, data);
                },
                newVolume -> CreateCompat.setVolume(contraptionEntity, localPos, newVolume),
                newRadius -> CreateCompat.setRadius(contraptionEntity, localPos, newRadius)), CONTAINER_TITLE), buf -> {
            buf.writeUtf(url);
            buf.writeFloat(volume);
            buf.writeVarInt(radius);
        });
        return true;
    }
}
