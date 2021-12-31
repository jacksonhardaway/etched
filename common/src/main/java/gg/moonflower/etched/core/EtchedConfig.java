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
}
