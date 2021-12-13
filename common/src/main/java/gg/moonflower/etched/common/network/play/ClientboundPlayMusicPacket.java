package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandler;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.pollen.api.network.packet.PollinatedPacket;
import gg.moonflower.pollen.api.network.packet.PollinatedPacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Ocelot
 */
public class ClientboundPlayMusicPacket implements PollinatedPacket<EtchedClientPlayPacketHandler> {

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
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeComponent(this.title);
        buf.writeUtf(this.url);
        buf.writeBlockPos(this.pos);
    }

    @Override
    public void processPacket(EtchedClientPlayPacketHandler handler, PollinatedPacketContext ctx) {
        handler.handlePlayMusicPacket(this, ctx);
    }

    /**
     * @return The title to show on the HUD as 'now playing'
     */
    public Component getTitle() {
        return title;
    }

    /**
     * @return The URL of the disk to play
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return The position the music disk is playing at
     */
    public BlockPos getPos() {
        return pos;
    }
}
