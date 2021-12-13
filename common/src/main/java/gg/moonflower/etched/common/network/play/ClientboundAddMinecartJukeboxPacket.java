package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.common.entity.MinecartJukebox;
import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandler;
import gg.moonflower.pollen.api.network.packet.PollinatedPacket;
import gg.moonflower.pollen.api.network.packet.PollinatedPacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

import java.util.UUID;

/**
 * @author Ocelot
 */
public class ClientboundAddMinecartJukeboxPacket implements PollinatedPacket<EtchedClientPlayPacketHandler> {

    private final int id;
    private final UUID uuid;
    private final double x;
    private final double y;
    private final double z;
    private final int xa;
    private final int ya;
    private final int za;
    private final int xRot;
    private final int yRot;

    public ClientboundAddMinecartJukeboxPacket(MinecartJukebox entity) {
        this.id = entity.getId();
        this.uuid = entity.getUUID();
        this.x = entity.getX();
        this.y = entity.getY();
        this.z = entity.getZ();
        this.xRot = Mth.floor(entity.getXRot() * 256.0F / 360.0F);
        this.yRot = Mth.floor(entity.getYRot() * 256.0F / 360.0F);
        this.xa = (int) (Mth.clamp(entity.getDeltaMovement().x, -3.9D, 3.9D) * 8000.0D);
        this.ya = (int) (Mth.clamp(entity.getDeltaMovement().y, -3.9D, 3.9D) * 8000.0D);
        this.za = (int) (Mth.clamp(entity.getDeltaMovement().z, -3.9D, 3.9D) * 8000.0D);
    }

    public ClientboundAddMinecartJukeboxPacket(FriendlyByteBuf friendlyByteBuf) {
        this.id = friendlyByteBuf.readVarInt();
        this.uuid = friendlyByteBuf.readUUID();
        this.x = friendlyByteBuf.readDouble();
        this.y = friendlyByteBuf.readDouble();
        this.z = friendlyByteBuf.readDouble();
        this.xRot = friendlyByteBuf.readByte();
        this.yRot = friendlyByteBuf.readByte();
        this.xa = friendlyByteBuf.readShort();
        this.ya = friendlyByteBuf.readShort();
        this.za = friendlyByteBuf.readShort();
    }


    @Override
    public void writePacketData(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeVarInt(this.id);
        friendlyByteBuf.writeUUID(this.uuid);
        friendlyByteBuf.writeDouble(this.x);
        friendlyByteBuf.writeDouble(this.y);
        friendlyByteBuf.writeDouble(this.z);
        friendlyByteBuf.writeByte(this.xRot);
        friendlyByteBuf.writeByte(this.yRot);
        friendlyByteBuf.writeShort(this.xa);
        friendlyByteBuf.writeShort(this.ya);
        friendlyByteBuf.writeShort(this.za);
    }

    @Override
    public void processPacket(EtchedClientPlayPacketHandler handler, PollinatedPacketContext ctx) {
        handler.handleAddMinecartJukeboxPacket(this, ctx);
    }

    public int getId() {
        return this.id;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public double getXa() {
        return (double) this.xa / 8000.0D;
    }

    public double getYa() {
        return (double) this.ya / 8000.0D;
    }

    public double getZa() {
        return (double) this.za / 8000.0D;
    }

    public int getxRot() {
        return this.xRot;
    }

    public int getyRot() {
        return this.yRot;
    }
}
