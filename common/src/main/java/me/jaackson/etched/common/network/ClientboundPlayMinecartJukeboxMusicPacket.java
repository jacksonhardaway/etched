package me.jaackson.etched.common.network;

import me.jaackson.etched.Etched;
import me.jaackson.etched.common.entity.MinecartJukebox;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.RecordItem;

/**
 * @author Ocelot
 */
public class ClientboundPlayMinecartJukeboxMusicPacket implements EtchedPacket {

    public static final ResourceLocation CHANNEL = new ResourceLocation(Etched.MOD_ID, "play_minecart_jukebox_music");

    private final Action action;
    private final int recordId;
    private final Component title;
    private final String url;
    private final int entityId;

    public ClientboundPlayMinecartJukeboxMusicPacket(Component title, String url, MinecartJukebox entity, boolean restart) {
        this.action = restart ? Action.RESTART : Action.START;
        this.recordId = 0;
        this.title = title;
        this.url = url;
        this.entityId = entity.getId();
    }

    public ClientboundPlayMinecartJukeboxMusicPacket(RecordItem recordItem, MinecartJukebox entity, boolean restart) {
        this.action = restart ? Action.RESTART : Action.START;
        this.recordId = Registry.ITEM.getId(recordItem);
        this.title = null;
        this.url = null;
        this.entityId = entity.getId();
    }

    public ClientboundPlayMinecartJukeboxMusicPacket(MinecartJukebox entity) {
        this.action = Action.STOP;
        this.recordId = 0;
        this.title = null;
        this.url = null;
        this.entityId = entity.getId();
    }

    public ClientboundPlayMinecartJukeboxMusicPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        this.recordId = this.action == Action.STOP ? 0 : buf.readVarInt();
        this.title = this.action == Action.STOP || this.recordId != 0 ? null : buf.readComponent();
        this.url = this.action == Action.STOP || this.recordId != 0 ? null : buf.readUtf();
        this.entityId = buf.readVarInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
        if (this.action != Action.STOP) {
            buf.writeVarInt(this.recordId);
            if (this.recordId == 0) {
                buf.writeComponent(this.title);
                buf.writeUtf(this.url);
            }
        }
        buf.writeVarInt(this.entityId);
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }

    /**
     * @return The action to be performed on the client
     */
    @Environment(EnvType.CLIENT)
    public Action getAction() {
        return action;
    }

    /**
     * @return The id of the record item or <code>0</code> for no record item
     */
    @Environment(EnvType.CLIENT)
    public int getRecordId() {
        return recordId;
    }

    /**
     * @return The title to show on the HUD as 'now playing'
     */
    @Environment(EnvType.CLIENT)
    public Component getTitle() {
        return title;
    }

    /**
     * @return The URL of the disk to play
     */
    @Environment(EnvType.CLIENT)
    public String getUrl() {
        return url;
    }

    /**
     * @return The id of the minecart entity
     */
    @Environment(EnvType.CLIENT)
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
