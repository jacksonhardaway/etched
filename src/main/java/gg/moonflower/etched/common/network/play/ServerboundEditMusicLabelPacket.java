package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.common.network.play.handler.EtchedServerPlayPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ServerboundEditMusicLabelPacket implements EtchedPacket {

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
    public void processPacket(NetworkEvent.Context ctx) {
        EtchedServerPlayPacketHandler.handleEditMusicLabel(this, ctx);
    }

    /**
     * @return The slot the music label is in
     */
    public int getSlot() {
        return this.slot;
    }

    /**
     * @return The new author
     */
    public String getAuthor() {
        return this.author;
    }

    public String getTitle() {
        return this.title;
    }
}
