package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.common.network.play.handler.EtchedServerPlayPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Jackson
 */
@ApiStatus.Internal
public class ServerboundSetUrlPacket implements EtchedPacket {

    private final String url;

    public ServerboundSetUrlPacket(String url) {
        this.url = url;
    }

    public ServerboundSetUrlPacket(FriendlyByteBuf buf) {
        this.url = buf.readUtf();
    }

    @Override
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeUtf(this.url);
    }

    @Override
    public void processPacket(NetworkEvent.Context ctx) {
        EtchedServerPlayPacketHandler.handleSetUrl(this, ctx);
    }

    /**
     * @return The URL to set in the etching table
     */
    public String getUrl() {
        return this.url;
    }

}
