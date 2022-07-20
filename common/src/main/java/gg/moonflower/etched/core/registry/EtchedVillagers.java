package gg.moonflower.etched.core.registry;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.mixin.StructureTemplatePoolAccessor;
import gg.moonflower.pollen.api.registry.PollinatedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.structures.StructurePoolElement;
import net.minecraft.world.level.levelgen.feature.structures.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.List;
import java.util.function.Supplier;

public class EtchedVillagers {

    public static final PollinatedRegistry<VillagerProfession> PROFESSIONS = PollinatedRegistry.create(Registry.VILLAGER_PROFESSION, Etched.MOD_ID);
    public static final PollinatedRegistry<PoiType> POI_TYPES = PollinatedRegistry.create(Registry.POINT_OF_INTEREST_TYPE, Etched.MOD_ID);

    public static final Supplier<PoiType> BARD_POI = POI_TYPES.register("bard", () -> PoiType.registerBlockStates(new PoiType("etched:bard", ImmutableSet.<BlockState>builder().addAll(Blocks.NOTE_BLOCK.getStateDefinition().getPossibleStates()).build(), 1, 1)));
    public static final Supplier<VillagerProfession> BARD = PROFESSIONS.register("bard", () -> new VillagerProfession("etched:bard", BARD_POI.get(), ImmutableSet.of(), ImmutableSet.of(), null));

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

    private static void createVillagePiece(String village, String name, int houseId, int weight, StructureProcessorList zombieProcessor) {
        createVillagePiece(village, name, houseId, weight, ProcessorLists.EMPTY, zombieProcessor);
    }

    private static void createVillagePiece(String village, String name, int houseId, int weight, StructureProcessorList normalProcessor, StructureProcessorList zombieProcessor) {
        EtchedVillagers.addToPool(new ResourceLocation("village/" + village + "/houses"), new ResourceLocation(Etched.MOD_ID, "village/" + village + "/houses/" + village + "_" + name + "_" + houseId), normalProcessor, weight);
        EtchedVillagers.addToPool(new ResourceLocation("village/" + village + "/zombie/houses"), new ResourceLocation(Etched.MOD_ID, "village/" + village + "/houses/" + village + "_" + name + "_" + houseId), zombieProcessor, weight);
    }

    private static void addToPool(ResourceLocation poolId, ResourceLocation pieceId, StructureProcessorList processorList, int weight) {
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
