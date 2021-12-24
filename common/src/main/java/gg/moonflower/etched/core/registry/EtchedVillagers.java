package gg.moonflower.etched.core.registry;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.mixin.StructureTemplatePoolAccessor;
import gg.moonflower.pollen.api.registry.PollinatedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.DesertVillagePools;
import net.minecraft.data.worldgen.PlainVillagePools;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.data.worldgen.SavannaVillagePools;
import net.minecraft.data.worldgen.SnowyVillagePools;
import net.minecraft.data.worldgen.TaigaVillagePools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.structures.StructurePoolElement;
import net.minecraft.world.level.levelgen.feature.structures.StructureTemplatePool;

import java.util.List;
import java.util.function.Supplier;

public class EtchedVillagers {

    public static final PollinatedRegistry<VillagerProfession> PROFESSIONS = PollinatedRegistry.create(Registry.VILLAGER_PROFESSION, Etched.MOD_ID);
    public static final PollinatedRegistry<PoiType> POI_TYPES = PollinatedRegistry.create(Registry.POINT_OF_INTEREST_TYPE, Etched.MOD_ID);

    public static final Supplier<PoiType> ETCHING_TABLE_POI = POI_TYPES.register("etching_table", () -> PoiType.registerBlockStates(new PoiType("etched:etching_table", ImmutableSet.<BlockState>builder().addAll(EtchedBlocks.ETCHING_TABLE.get().getStateDefinition().getPossibleStates()).build(), 1, 1)));
    public static final Supplier<VillagerProfession> BARD = PROFESSIONS.register("bard", () -> new VillagerProfession("etched:bard", ETCHING_TABLE_POI.get(), ImmutableSet.of(), ImmutableSet.of(), EtchedSounds.UI_ETCHER_TAKE_RESULT.get()));

    public static void registerVillages() {
        PlainVillagePools.bootstrap();
        DesertVillagePools.bootstrap();
        SavannaVillagePools.bootstrap();
        SnowyVillagePools.bootstrap();
        TaigaVillagePools.bootstrap();

        createVillagePiece("plains", "bard_house", 1, 3);
        createVillagePiece("desert", "bard_house", 1, 5);
        createVillagePiece("savanna", "bard_house", 1, 7);
        createVillagePiece("snowy", "bard_house", 1, 10);
        createVillagePiece("taiga", "bard_house", 1, 8);
    }

    private static void createVillagePiece(String village, String name, int houseId, int weight) {
        ResourceLocation patternId = new ResourceLocation("village/" + village + "/houses");
        StructureTemplatePool pattern = BuiltinRegistries.TEMPLATE_POOL.get(patternId);
        if (pattern == null)
            return;

        StructurePoolElement piece = StructurePoolElement.legacy(Etched.MOD_ID + ":village/" + village + "/houses/" + village + "_" + name + "_" + houseId, ProcessorLists.MOSSIFY_10_PERCENT).apply(StructureTemplatePool.Projection.RIGID);
        List<StructurePoolElement> templates = ((StructureTemplatePoolAccessor) pattern).getTemplates();
        List<Pair<StructurePoolElement, Integer>> rawTemplates = ((StructureTemplatePoolAccessor) pattern).getRawTemplates();
        if (templates == null || rawTemplates == null)
            return;

        for (int i = 0; i < weight; i++)
            templates.add(piece);
        rawTemplates.add(Pair.of(piece, weight));
    }

}
