package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.common.network.play.handler.EtchedServerPlayPacketHandler;
import gg.moonflower.pollen.api.network.packet.PollinatedPacket;
import gg.moonflower.pollen.api.network.packet.PollinatedPacketContext;
import net.minecraft.network.FriendlyByteBuf;

public class ServerboundEditMusicLabelPacket implements PollinatedPacket<EtchedServerPlayPacketHandler> {

    private final int slot;
    private final String author;
    private final String title;

    public ServerboundEditMusicLabelPacket(int slot, String author, String title) {
        this.slot = slot;
        this.author = author;
        this.title = title;
    }

    public ServerboundEditMusicLabelPacket(FriendlyByteBuf buf) {
        this.slot = buf.readVarInt();
        this.author = buf.readUtf(128);
        this.title = buf.readUtf(128);
    }

    @Override
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeVarInt(this.slot);
        buf.writeUtf(this.author, 128);
        buf.writeUtf(this.title, 128);
    }

    @Override
    public void processPacket(EtchedServerPlayPacketHandler handler, PollinatedPacketContext ctx) {
        handler.handleEditMusicLabel(this, ctx);
    }

    public int getSlot() {
        return slot;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }
}
