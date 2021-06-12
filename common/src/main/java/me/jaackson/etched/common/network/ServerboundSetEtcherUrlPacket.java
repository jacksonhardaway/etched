package me.jaackson.etched.common.network;

import me.jaackson.etched.Etched;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Jackson
 */
public class ServerboundSetEtcherUrlPacket implements EtchedPacket {

    public static final ResourceLocation CHANNEL = new ResourceLocation(Etched.MOD_ID, "set_url");

    private final String url;

    public ServerboundSetEtcherUrlPacket(String url) {
        this.url = url;
    }

    public ServerboundSetEtcherUrlPacket(FriendlyByteBuf buf) {
        this.url = buf.readUtf(32767);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.url);
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }

    /**
     * @return The URL to set in the etcher
     */
    public String getUrl() {
        return url;
    }

}
