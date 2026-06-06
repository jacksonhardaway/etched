package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.core.Etched;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public record ClientboundPlayEntityMusicPacket(Action action, ItemStack record, int entityId, @Nullable UUID storageId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientboundPlayEntityMusicPacket> TYPE = new CustomPacketPayload.Type<>(Etched.etchedPath("play_entity_music"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundPlayEntityMusicPacket> CODEC = StreamCodec.of((buf, packet) -> {
        buf.writeEnum(packet.action);
        if (packet.action != Action.STOP) {
            ItemStack.STREAM_CODEC.encode(buf, packet.record);
        }
        buf.writeVarInt(packet.entityId);
        if (packet.action != Action.STOP && Etched.SOPHSTICATED_CORE_LOADED) {
            buf.writeBoolean(packet.storageId != null);
            if (packet.storageId != null) {
                buf.writeUUID(packet.storageId);
            }
        }
    }, buf -> {
        Action action = buf.readEnum(Action.class);
        ItemStack record = action == Action.STOP ? ItemStack.EMPTY : ItemStack.STREAM_CODEC.decode(buf);
        int entityId = buf.readVarInt();
        UUID storageId = action != Action.STOP && buf.readBoolean() && Etched.SOPHSTICATED_CORE_LOADED ? buf.readUUID() : null;
        return new ClientboundPlayEntityMusicPacket(action, record, entityId, storageId);
    });

    public ClientboundPlayEntityMusicPacket(ItemStack record, Entity entity, boolean restart, @Nullable UUID storageId) {
        this(restart ? Action.RESTART : Action.START, record, entity.getId(), storageId);
    }

    public ClientboundPlayEntityMusicPacket(Entity entity) {
        this(Action.STOP, ItemStack.EMPTY, entity.getId(), null);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * @author Ocelot
     */
    public enum Action {
        START, STOP, RESTART
    }
}
