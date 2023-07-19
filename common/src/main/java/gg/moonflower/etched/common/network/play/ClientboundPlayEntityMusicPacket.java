package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandler;
import gg.moonflower.pollen.api.network.v1.packet.PollinatedPacket;
import gg.moonflower.pollen.api.network.v1.packet.PollinatedPacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public class ClientboundPlayEntityMusicPacket implements PollinatedPacket<EtchedClientPlayPacketHandler> {

    private final Action action;
    private final ItemStack record;
    private final int entityId;

    public ClientboundPlayEntityMusicPacket(ItemStack record, Entity entity, boolean restart) {
        this.action = restart ? Action.RESTART : Action.START;
        this.record = record;
        this.entityId = entity.getId();
    }

    public ClientboundPlayEntityMusicPacket(Entity entity) {
        this.action = Action.STOP;
        this.record = ItemStack.EMPTY;
        this.entityId = entity.getId();
    }

    public ClientboundPlayEntityMusicPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        this.record = this.action == Action.STOP ? ItemStack.EMPTY : buf.readItem();
        this.entityId = buf.readVarInt();
    }

    @Override
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
        if (this.action != Action.STOP)
            buf.writeItem(this.record);
        buf.writeVarInt(this.entityId);
    }

    @Override
    public void processPacket(EtchedClientPlayPacketHandler handler, PollinatedPacketContext ctx) {
        handler.handlePlayEntityMusicPacket(this, ctx);
    }

    /**
     * @return The action to be performed on the client
     */
    public Action getAction() {
        return action;
    }

    /**
     * @return The id of the record item
     */
    public ItemStack getRecord() {
        return record;
    }

    /**
     * @return The id of the minecart entity
     */
    public int getEntityId() {
        return entityId;
    }

    /**
     * @author Ocelot
     */
    public enum Action {
        START, STOP, RESTART
    }
}
