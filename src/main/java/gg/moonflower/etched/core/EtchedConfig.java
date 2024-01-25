package gg.moonflower.etched.core;

import net.minecraftforge.common.ForgeConfigSpec;

public class EtchedConfig {

    public static class Client {

        public final ForgeConfigSpec.BooleanValue showNotes;
        public final ForgeConfigSpec.BooleanValue forceStereo;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.push("Game Feel");
            this.showNotes = builder.comment("Displays note particles appear above jukeboxes while a record is playing.").define("Display Note Particles", true);
            this.forceStereo = builder.comment("Always plays tracks in stereo even when in-world").define("Force Stereo", false);
            builder.pop();
        }
    }

    public static class Server {

        public final ForgeConfigSpec.BooleanValue useBoomboxMenu;
        public final ForgeConfigSpec.BooleanValue useAlbumCoverMenu;

        public Server(ForgeConfigSpec.Builder builder) {
            builder.push("Boombox");
            this.useBoomboxMenu = builder.comment("Disables right clicking music discs into boomboxes and allows the menu to be used by shift right-clicking").define("Use boombox menu", false);
            builder.pop();

            builder.push("Album Cover");
            this.useAlbumCoverMenu = builder.comment("Disables right clicking music discs into album covers and allows the menu to be used by shift right-clicking").define("Use album cover menu", false);
            builder.pop();
        }
    }
}
