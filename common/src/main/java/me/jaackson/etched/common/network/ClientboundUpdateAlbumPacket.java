//package me.jaackson.etched.common.network;
//
//import me.jaackson.etched.Etched;
//import net.fabricmc.api.EnvType;
//import net.fabricmc.api.Environment;
//import net.minecraft.core.BlockPos;
//import net.minecraft.network.FriendlyByteBuf;
//import net.minecraft.resources.ResourceLocation;
//
///**
// * @author Ocelot
// */
//public class ClientboundUpdateAlbumPacket implements EtchedPacket {
//
//    public static final ResourceLocation CHANNEL = new ResourceLocation(Etched.MOD_ID, "update_album");
//
//    private final BlockPos pos;
//
//    public ClientboundUpdateAlbumPacket(BlockPos pos) {
//        this.pos = pos;
//    }
//
//    public ClientboundUpdateAlbumPacket(FriendlyByteBuf buf) {
//        this.pos = buf.readBlockPos();
//    }
//
//    @Override
//    public void write(FriendlyByteBuf buf) {
//        buf.writeBlockPos(this.pos);
//    }
//
//    @Override
//    public ResourceLocation getChannel() {
//        return CHANNEL;
//    }
//
//    /**
//     * @return The position the abum jukebox is located at
//     */
//    @Environment(EnvType.CLIENT)
//    public BlockPos getPos() {
//        return pos;
//    }
//}
