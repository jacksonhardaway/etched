package me.jaackson.etched.common.network;

import me.jaackson.etched.Etched;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Ocelot
 */
public class ClientboundPlayMusicPacket implements EtchedPacket {

    public static final ResourceLocation CHANNEL = new ResourceLocation(Etched.MOD_ID, "play_music");

    private final Component title;
    private final String url;
    private final BlockPos pos;

    public ClientboundPlayMusicPacket(Component title, String url, BlockPos pos) {
        this.title = title;
        this.url = url;
        this.pos = pos;
    }

    public ClientboundPlayMusicPacket(FriendlyByteBuf buf) {
        this.title = buf.readComponent();
        this.url = buf.readUtf();
        this.pos = buf.readBlockPos();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeComponent(this.title);
        buf.writeUtf(this.url);
        buf.writeBlockPos(this.pos);
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
     * @return The position the music disk is playing at
     */
    @Environment(EnvType.CLIENT)
    public BlockPos getPos() {
        return pos;
    }
}
