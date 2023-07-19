package gg.moonflower.etched.core.registry;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.architectury.registry.registries.RegistrySupplier;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.mixin.StructureTemplatePoolAccessor;
import gg.moonflower.pollen.api.event.entity.v1.ModifyTradesEvents;
import gg.moonflower.pollen.api.registry.wrapper.v1.PollinatedVillagerRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.List;

public class EtchedVillagers {

    public static final PollinatedVillagerRegistry REGISTRY = PollinatedVillagerRegistry.create(Etched.MOD_ID);

    public static final RegistrySupplier<PoiType> BARD_POI = REGISTRY.registerPoiType("bard", () -> new PoiType(ImmutableSet.<BlockState>builder().addAll(Blocks.NOTE_BLOCK.getStateDefinition().getPossibleStates()).build(), 1, 1));
    public static final RegistrySupplier<VillagerProfession> BARD = REGISTRY.register("bard", () -> new VillagerProfession(Etched.MOD_ID + ":bard", poi -> poi.is(BARD_POI.getId()), poi -> poi.is(BARD_POI.getId()), ImmutableSet.of(), ImmutableSet.of(), EtchedSounds.UI_ETCHER_TAKE_RESULT.get()));

    public static void registerVillages() {
        PlainVillagePools.bootstrap();
        DesertVillagePools.bootstrap();
        SavannaVillagePools.bootstrap();
        SnowyVillagePools.bootstrap();
        TaigaVillagePools.bootstrap();

        createVillagePiece("plains", "bard_house", 1, 2, ProcessorLists.MOSSIFY_10_PERCENT, ProcessorLists.ZOMBIE_PLAINS);
        createVillagePiece("desert", "bard_house", 1, 2, ProcessorLists.ZOMBIE_DESERT);
        createVillagePiece("savanna", "bard_house", 1, 4, ProcessorLists.ZOMBIE_SAVANNA);
        createVillagePiece("snowy", "bard_house", 1, 4, ProcessorLists.ZOMBIE_SNOWY);
        createVillagePiece("taiga", "bard_house", 1, 4, ProcessorLists.MOSSIFY_10_PERCENT, ProcessorLists.ZOMBIE_TAIGA);
    }

    public static void registerTrades() {
        ModifyTradesEvents.VILLAGER.register(context -> {
            if (context.getProfession() != EtchedVillagers.BARD.get())
                return;

            ModifyTradesEvents.TradeRegistry tier1 = context.getTrades(1);
            tier1.add(Items.MUSIC_DISC_13, 8, 1, 4, 20, true);
            tier1.add(Items.MUSIC_DISC_11, 8, 1, 4, 20, true);
            tier1.add(Items.MUSIC_DISC_CAT, 8, 1, 4, 20, true);
            tier1.add(Items.MUSIC_DISC_OTHERSIDE, 8, 1, 4, 20, true);
            tier1.add(Items.NOTE_BLOCK, 1, 2, 16, 2, true);
            tier1.add(EtchedItems.MUSIC_LABEL, 4, 2, 16, 1, false);

            ModifyTradesEvents.TradeRegistry tier2 = context.getTrades(2);
            tier2.add(EtchedItems.BLANK_MUSIC_DISC, 28, 2, 12, 15, false);
            tier2.add(EtchedBlocks.ETCHING_TABLE, 32, 1, 8, 15, false);

            ModifyTradesEvents.TradeRegistry tier3 = context.getTrades(3);
            tier3.add(Blocks.CLAY, 6, 1, 16, 2, false);
            tier3.add(Blocks.HAY_BLOCK, 12, 1, 8, 2, false);
            tier3.add(Blocks.WHITE_WOOL, 8, 1, 32, 4, false);
            tier3.add(Blocks.BONE_BLOCK, 24, 1, 8, 4, false);
            tier3.add(Blocks.PACKED_ICE, 36, 1, 4, 8, false);
            tier3.add(Blocks.GOLD_BLOCK, 48, 1, 2, 10, false);

            ModifyTradesEvents.TradeRegistry tier4 = context.getTrades(4);
            tier3.add(Items.JUKEBOX, 26, 1, 4, 30, false);
            tier4.add(EtchedItems.JUKEBOX_MINECART, 28, 1, 4, 30, false);
            tier4.add(EtchedBlocks.ALBUM_JUKEBOX, 30, 1, 4, 30, false);

            ModifyTradesEvents.TradeRegistry tier5 = context.getTrades(5);
            tier5.add(Items.DIAMOND, 8, 1, 8, 40, true);
            tier5.add(Items.AMETHYST_SHARD, 1, 8, 10, 40, true);

            Registry.ITEM.getTag(ItemTags.MUSIC_DISCS).ifPresent(tag -> tag.stream().forEach(item -> tier5.add(item.value(), 10, 1, 4, 40, true)));
        });
    }

    private static void createVillagePiece(String village, String name, int houseId, int weight, Holder<StructureProcessorList> zombieProcessor) {
        createVillagePiece(village, name, houseId, weight, ProcessorLists.EMPTY, zombieProcessor);
    }

    private static void createVillagePiece(String village, String name, int houseId, int weight, Holder<StructureProcessorList> normalProcessor, Holder<StructureProcessorList> zombieProcessor) {
        EtchedVillagers.addToPool(new ResourceLocation("village/" + village + "/houses"), new ResourceLocation(Etched.MOD_ID, "village/" + village + "/houses/" + village + "_" + name + "_" + houseId), normalProcessor, weight);
        EtchedVillagers.addToPool(new ResourceLocation("village/" + village + "/zombie/houses"), new ResourceLocation(Etched.MOD_ID, "village/" + village + "/houses/" + village + "_" + name + "_" + houseId), zombieProcessor, weight);
    }

    private static void addToPool(ResourceLocation poolId, ResourceLocation pieceId, Holder<StructureProcessorList> processorList, int weight) {
        StructureTemplatePool pool = BuiltinRegistries.TEMPLATE_POOL.get(poolId);
        if (pool == null)
            return;

        StructurePoolElement piece = StructurePoolElement.legacy(pieceId.toString(), processorList).apply(StructureTemplatePool.Projection.RIGID);
        List<StructurePoolElement> templates = ((StructureTemplatePoolAccessor) pool).getTemplates();
        List<Pair<StructurePoolElement, Integer>> rawTemplates = ((StructureTemplatePoolAccessor) pool).getRawTemplates();
        if (templates == null || rawTemplates == null)
            return;

        for (int i = 0; i < weight; i++)
            templates.add(piece);
        rawTemplates.add(Pair.of(piece, weight));
    }

}
