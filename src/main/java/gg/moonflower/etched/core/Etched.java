package gg.moonflower.etched.core;

import com.mojang.logging.LogUtils;
import gg.moonflower.etched.api.sound.download.SoundSourceManager;
import gg.moonflower.etched.common.component.MusicLabelComponent;
import gg.moonflower.etched.common.sound.download.BandcampSource;
import gg.moonflower.etched.common.sound.download.SoundCloudSource;
import gg.moonflower.etched.core.data.*;
import gg.moonflower.etched.core.data.loot.EtchedBlockLootProvider;
import gg.moonflower.etched.core.data.tags.EtchedBlockTagsProvider;
import gg.moonflower.etched.core.data.tags.EtchedEntityTypeTagsProvider;
import gg.moonflower.etched.core.data.tags.EtchedItemTagsProvider;
import gg.moonflower.etched.core.data.tags.EtchedPointOfInterestTagsProvider;
import gg.moonflower.etched.core.registry.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mod(Etched.MOD_ID)
public class Etched {

    public static final String MOD_ID = "etched";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final EtchedConfig.Client CLIENT_CONFIG;
    public static final EtchedConfig.Server SERVER_CONFIG;
    private static final ModConfigSpec clientSpec;
    private static final ModConfigSpec serverSpec;

    static {
        Pair<EtchedConfig.Client, ModConfigSpec> clientConfig = new ModConfigSpec.Builder().configure(EtchedConfig.Client::new);
        clientSpec = clientConfig.getRight();
        CLIENT_CONFIG = clientConfig.getLeft();

        Pair<EtchedConfig.Server, ModConfigSpec> serverConfig = new ModConfigSpec.Builder().configure(EtchedConfig.Server::new);
        serverSpec = serverConfig.getRight();
        SERVER_CONFIG = serverConfig.getLeft();
    }

    public Etched(IEventBus bus, ModContainer container) {
        bus.addListener(this::init);
        bus.addListener(this::dataInit);

        EtchedBlocks.BLOCKS.register(bus);
        EtchedBlocks.BLOCK_ENTITIES.register(bus);
        EtchedComponents.REGISTRY.register(bus);
        EtchedEntities.REGISTRY.register(bus);
        EtchedItems.REGISTRY.register(bus);
        EtchedMenus.REGISTRY.register(bus);
        EtchedRecipes.REGISTRY.register(bus);
        EtchedSounds.REGISTRY.register(bus);
        EtchedVillagers.POI_REGISTRY.register(bus);
        EtchedVillagers.PROFESSION_REGISTRY.register(bus);

        container.registerConfig(ModConfig.Type.CLIENT, clientSpec);
        container.registerConfig(ModConfig.Type.SERVER, serverSpec);
    }

    public static ResourceLocation etchedPath(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private void init(FMLCommonSetupEvent event) {
        SoundSourceManager.registerSource(new SoundCloudSource());
        SoundSourceManager.registerSource(new BandcampSource());

        event.enqueueWork(() -> {
            Map<Item, CauldronInteraction> map = CauldronInteraction.WATER.map();
            map.put(EtchedItems.BLANK_MUSIC_DISC.get(), CauldronInteraction.DYED_ITEM);
            map.put(EtchedItems.MUSIC_LABEL.get(), CauldronInteraction.DYED_ITEM);
            map.put(EtchedItems.MUSIC_LABEL.get(), (state, level, pos, player, hand, stack) -> {
                MusicLabelComponent label = stack.get(EtchedComponents.MUSIC_LABEL);
                if (label == null || !label.isColored()) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }

                if (!level.isClientSide()) {
                    ItemStack newStack = stack.copy();
                    newStack.set(EtchedComponents.MUSIC_LABEL, label.withColor(-1, -1));
                    player.setItemInHand(hand, newStack);
                    player.awardStat(Stats.CLEAN_ARMOR);
                    LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                }

                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            });
        });
    }

    private void dataInit(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput packOutput = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        EtchedBlockTagsProvider blockTags = new EtchedBlockTagsProvider(packOutput, lookupProvider, existingFileHelper);
        gen.addProvider(event.includeServer(), blockTags);
        gen.addProvider(event.includeServer(), new EtchedItemTagsProvider(packOutput, lookupProvider, blockTags.contentsGetter(), existingFileHelper));
        gen.addProvider(event.includeServer(), new EtchedEntityTypeTagsProvider(packOutput, lookupProvider, existingFileHelper));
        gen.addProvider(event.includeServer(), new EtchedPointOfInterestTagsProvider(packOutput, lookupProvider, existingFileHelper));
        gen.addProvider(event.includeServer(), new EtchedRecipeProvider(packOutput, lookupProvider));
        gen.addProvider(event.includeServer(), new LootTableProvider(packOutput,
                Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(
                        EtchedBlockLootProvider::new,
                        LootContextParamSets.BLOCK
                )),
                lookupProvider
        ));

        gen.addProvider(event.includeClient(), new EtchedItemModelProvider(packOutput, existingFileHelper));
    }
}
