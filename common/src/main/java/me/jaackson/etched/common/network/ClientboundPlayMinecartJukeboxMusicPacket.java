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

    private final boolean stop;
    private final int recordId;
    private final Component title;
    private final String url;
    private final int entityId;

    public ClientboundPlayMinecartJukeboxMusicPacket(Component title, String url, MinecartJukebox entity) {
        this.stop = false;
        this.recordId = 0;
        this.title = title;
        this.url = url;
        this.entityId = entity.getId();
    }

    public ClientboundPlayMinecartJukeboxMusicPacket(RecordItem recordItem, MinecartJukebox entity) {
        this.stop = false;
        this.recordId = Registry.ITEM.getId(recordItem);
        this.title = null;
        this.url = null;
        this.entityId = entity.getId();
    }

    public ClientboundPlayMinecartJukeboxMusicPacket(MinecartJukebox entity) {
        this.stop = true;
        this.recordId = 0;
        this.title = null;
        this.url = null;
        this.entityId = entity.getId();
    }

    public ClientboundPlayMinecartJukeboxMusicPacket(FriendlyByteBuf buf) {
        this.stop = buf.readBoolean();
        this.recordId = this.stop ? 0 : buf.readVarInt();
        this.title = this.stop || this.recordId != 0 ? null : buf.readComponent();
        this.url = this.stop || this.recordId != 0 ? null : buf.readUtf();
        this.entityId = buf.readVarInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.stop);
        if (!this.stop) {
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
     * @return Whether or not to stop playing music
     */
    @Environment(EnvType.CLIENT)
    public boolean isStop() {
        return stop;
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
}
