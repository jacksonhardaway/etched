package gg.moonflower.etched.core;

import gg.moonflower.pollen.api.config.PollinatedConfigBuilder;

public class EtchedConfig {

    public static class Client {

        public final PollinatedConfigBuilder.ConfigValue<Boolean> showNotes;

        public Client(PollinatedConfigBuilder builder) {
            builder.push("Game Feel");
            this.showNotes = builder.comment("Displays note particles appear above jukeboxes while a record is playing.").define("Display Note Particles", true);
            builder.pop();
        }
    }

    public static class Server {

        public final PollinatedConfigBuilder.ConfigValue<Boolean> useBoomboxMenu;
        public final PollinatedConfigBuilder.ConfigValue<Boolean> useAlbumCoverMenu;

        public Server(PollinatedConfigBuilder builder) {
            builder.push("Boombox");
            this.useBoomboxMenu = builder.comment("Disables right clicking music discs into boomboxes and allows the menu to be used by shift right-clicking").define("Use boombox menu", false);
            builder.pop();

            builder.push("Album Cover");
            this.useAlbumCoverMenu = builder.comment("Disables right clicking music discs into album covers and allows the menu to be used by shift right-clicking").define("Use album cover menu", false);
            builder.pop();
        }
    }
}
