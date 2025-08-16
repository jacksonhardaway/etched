package gg.moonflower.etched.core.data.loot;

import gg.moonflower.etched.common.block.RadioBlock;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Set;

public class EtchedBlockLootProvider extends BlockLootSubProvider {
    public EtchedBlockLootProvider(HolderLookup.Provider lookupProvider) {
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, lookupProvider);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return EtchedBlocks.BLOCKS.getEntries()
                .stream()
                .map(e -> (Block) e.value())
                .toList();
    }

    @Override
    protected void generate() {
        this.dropSelf(EtchedBlocks.ALBUM_JUKEBOX.get());
        this.dropSelf(EtchedBlocks.ETCHING_TABLE.get());

        this.add(EtchedBlocks.RADIO.get(), block -> LootTable.lootTable()
                .withPool(
                        LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0F))
                                .add(
                                        this.applyExplosionCondition(
                                                block,
                                                AlternativesEntry.alternatives(
                                                        LootItem.lootTableItem(EtchedBlocks.PORTAL_RADIO_ITEM)
                                                                .when(
                                                                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                                                                .setProperties(
                                                                                        StatePropertiesPredicate.Builder.properties()
                                                                                                .hasProperty(RadioBlock.PORTAL, true)
                                                                                )
                                                                ),
                                                        LootItem.lootTableItem(EtchedBlocks.RADIO)
                                                )

                                        )
                                )
                ));
    }
}
