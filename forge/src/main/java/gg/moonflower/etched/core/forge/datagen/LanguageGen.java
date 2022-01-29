package gg.moonflower.etched.core.forge.datagen;

import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import gg.moonflower.etched.core.registry.EtchedItems;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;

public class LanguageGen extends LanguageProvider {

    public LanguageGen(DataGenerator gen) {
        super(gen, Etched.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        this.addBlock(EtchedBlocks.ETCHING_TABLE, "Etching Table");
        this.addBlock(EtchedBlocks.ALBUM_JUKEBOX, "Album Jukebox");
        this.addItem(EtchedItems.BLANK_MUSIC_DISC, "Blank Music Disc");
        this.addItem(EtchedItems.ETCHED_MUSIC_DISC, "Etched Music Disc");
        this.addItem(EtchedItems.BOOMBOX, "Boombox");
        this.addItem(EtchedItems.MUSIC_LABEL, "Music Label");
        this.addItem(EtchedItems.JUKEBOX_MINECART, "Minecart with Jukebox");
        this.add("item." + Etched.MOD_ID + ".etched_music_disc.sound_cloud", "Provided by SoundCloud");
        this.add("item." + Etched.MOD_ID + ".boombox.paused", "Paused");
        this.add("container." + Etched.MOD_ID + ".etching_table", "Etching Table");
        this.add("container." + Etched.MOD_ID + ".album_jukebox", "Album Jukebox");
        this.add("record." + Etched.MOD_ID + ".downloadProgress", "Downloading (%s MB / %s MB): %s");
        this.add("record." + Etched.MOD_ID + ".loading", "Loading %s");
        this.add("record." + Etched.MOD_ID + ".downloadFail", "Failed to download %s");
        this.add("subtitles." + Etched.MOD_ID + ".ui.etching_table.take_result", "Etching Table used");
        this.add("sound_cloud.requesting", "Requesting from SoundCloud...");
        this.add("entity.minecraft.villager." + Etched.MOD_ID + ".bard", "Bard");
        this.add("entity.minecraft.villager.bard", "Bard");
        this.add("screen." + Etched.MOD_ID + ".etching_table.error.missing_label", "Music label is required to etch");
        this.add("screen." + Etched.MOD_ID + ".etching_table.error.missing_disc", "Blank disc is required to etch");
        this.add("screen." + Etched.MOD_ID + ".etching_table.error.invalid_url", "Cannot etch invalid url");
    }
}
