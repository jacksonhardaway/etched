package me.jaackson.etched.datagen;

import me.jaackson.etched.Etched;
import me.jaackson.etched.EtchedRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;

public class LanguageGen extends LanguageProvider {

    public LanguageGen(DataGenerator gen) {
        super(gen, Etched.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        this.addBlock(EtchedRegistry.ETCHING_TABLE, "Etching Table");
        this.addBlock(EtchedRegistry.ALBUM_JUKEBOX, "Album Jukebox");
        this.addItem(EtchedRegistry.BLANK_MUSIC_DISC, "Blank Music Disc");
        this.addItem(EtchedRegistry.ETCHED_MUSIC_DISC, "Etched Music Disc");
        this.addItem(EtchedRegistry.MUSIC_LABEL, "Music Label");
        this.addItem(EtchedRegistry.JUKEBOX_MINECART, "Minecart with Jukebox");
        this.add("item." + Etched.MOD_ID + ".etched_music_disc.sound_cloud", "Provided by SoundCloud");
        this.add("container." + Etched.MOD_ID + ".etching_table", "Etching Table");
        this.add("container." + Etched.MOD_ID + ".album_jukebox", "Album Jukebox");
        this.add("record." + Etched.MOD_ID + ".downloadProgress", "Downloading (%s MB / %s MB): %s");
        this.add("record." + Etched.MOD_ID + ".downloadFail", "Failed to download %s");
        this.add("subtitles." + Etched.MOD_ID + ".ui.etching_table.take_result", "Etching Table used");
        this.add("sound_cloud.requesting", "Requesting from SoundCloud...");
    }
}
