package gg.moonflower.etched.core.data.tags;

import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import gg.moonflower.etched.core.registry.EtchedTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class EtchedBlockTagsProvider extends BlockTagsProvider {

    public EtchedBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Etched.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(EtchedTags.RECORD_PLAYERS)
                .add(EtchedBlocks.ALBUM_JUKEBOX.get(), EtchedBlocks.RADIO.get(), Blocks.JUKEBOX);
        this.tag(BlockTags.MINEABLE_WITH_AXE)
                .add(EtchedBlocks.ETCHING_TABLE.get(), EtchedBlocks.ALBUM_JUKEBOX.get(), EtchedBlocks.RADIO.get());
        this.tag(Tags.Blocks.VILLAGER_JOB_SITES)
                .add(Blocks.NOTE_BLOCK);
    }
}
