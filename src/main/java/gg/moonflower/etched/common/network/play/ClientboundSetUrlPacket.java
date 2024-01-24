package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public class ClientboundSetUrlPacket implements EtchedPacket {

    private final String url;

    public ClientboundSetUrlPacket(@Nullable String url) {
        this.url = url != null ? url : "";
    }

    public ClientboundSetUrlPacket(FriendlyByteBuf buf) {
        this.url = buf.readUtf(32767);
    }

    @Override
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeUtf(this.url);
    }

    @Override
    public void processPacket(NetworkEvent.Context ctx) {
        EtchedClientPlayPacketHandler.handleSetUrl(this, ctx);
    }

    /**
     * @return The URL to set in the etching table
     */
    public String getUrl() {
        return this.url;
    }
}
