package gg.moonflower.etched.core.registry;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.mixin.StructureTemplatePoolAccessor;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Etched.MOD_ID)
public class EtchedVillagers {

    public static final DeferredRegister<PoiType> POI_REGISTRY = DeferredRegister.create(ForgeRegistries.POI_TYPES, Etched.MOD_ID);
    public static final DeferredRegister<VillagerProfession> PROFESSION_REGISTRY = DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, Etched.MOD_ID);

    public static final RegistryObject<PoiType> BARD_POI = POI_REGISTRY.register("bard", () -> new PoiType(ImmutableSet.<BlockState>builder().addAll(Blocks.NOTE_BLOCK.getStateDefinition().getPossibleStates()).build(), 1, 1));
    public static final RegistryObject<VillagerProfession> BARD = PROFESSION_REGISTRY.register("bard", () -> new VillagerProfession(Etched.MOD_ID + ":bard", poi -> poi.is(BARD_POI.getId()), poi -> poi.is(BARD_POI.getId()), ImmutableSet.of(), ImmutableSet.of(), null));

    @SubscribeEvent
    public static void onEvent(net.minecraftforge.event.village.VillagerTradesEvent event) {
        if (event.getType() != EtchedVillagers.BARD.get()) {
            return;
        }

        Int2ObjectMap<TradeRegistry> newTrades = new Int2ObjectOpenHashMap<>();
        int minTier = event.getTrades().keySet().intStream().min().orElse(1);
        int maxTier = event.getTrades().keySet().intStream().max().orElse(5);
        registerTrades(tier -> {
            Validate.inclusiveBetween(minTier, maxTier, tier, "Tier must be between " + minTier + " and " + maxTier);
            return newTrades.computeIfAbsent(tier, key -> new TradeRegistry());
        });

        newTrades.forEach((tier, registry) -> event.getTrades().get(tier.intValue()).addAll(registry));
    }

    private static void registerTrades(Function<Integer, TradeRegistry> context) {
        TradeRegistry tier1 = context.apply(1);
        tier1.add(Items.MUSIC_DISC_13, 8, 1, 4, 20, true);
        tier1.add(Items.MUSIC_DISC_11, 8, 1, 4, 20, true);
        tier1.add(Items.MUSIC_DISC_CAT, 8, 1, 4, 20, true);
        tier1.add(Items.MUSIC_DISC_OTHERSIDE, 8, 1, 4, 20, true);
        tier1.add(Items.NOTE_BLOCK, 1, 2, 16, 2, true);
        tier1.add(EtchedItems.MUSIC_LABEL, 4, 2, 16, 1, false);

        TradeRegistry tier2 = context.apply(2);
        tier2.add(EtchedItems.BLANK_MUSIC_DISC, 28, 2, 12, 15, false);
        tier2.add(EtchedBlocks.ETCHING_TABLE, 32, 1, 8, 15, false);

        TradeRegistry tier3 = context.apply(3);
        tier3.add(Blocks.CLAY, 6, 1, 16, 2, false);
        tier3.add(Blocks.HAY_BLOCK, 12, 1, 8, 2, false);
        tier3.add(Blocks.WHITE_WOOL, 8, 1, 32, 4, false);
        tier3.add(Blocks.BONE_BLOCK, 24, 1, 8, 4, false);
        tier3.add(Blocks.PACKED_ICE, 36, 1, 4, 8, false);
        tier3.add(Blocks.GOLD_BLOCK, 48, 1, 2, 10, false);

        TradeRegistry tier4 = context.apply(4);
        tier3.add(Items.JUKEBOX, 26, 1, 4, 30, false);
        tier4.add(EtchedItems.JUKEBOX_MINECART, 28, 1, 4, 30, false);
        tier4.add(EtchedBlocks.ALBUM_JUKEBOX, 30, 1, 4, 30, false);

        TradeRegistry tier5 = context.apply(5);
        tier5.add(Items.DIAMOND, 8, 1, 8, 40, true);
        tier5.add(Items.AMETHYST_SHARD, 1, 8, 10, 40, true);

        // sucks to suck forge
        BuiltInRegistries.ITEM.getTag(ItemTags.MUSIC_DISCS).ifPresent(tag -> tag.stream().forEach(item -> tier5.add(item.value(), 10, 1, 4, 40, true)));
    }

    @SubscribeEvent
    public static void onEvent(ServerAboutToStartEvent event) {
        RegistryAccess.Frozen access = event.getServer().registryAccess();
        Optional<Registry<StructureTemplatePool>> templateRegistryOptional = access.registry(Registries.TEMPLATE_POOL);
        Optional<Registry<StructureProcessorList>> processorListRegistyOptional = access.registry(Registries.PROCESSOR_LIST);

        if (templateRegistryOptional.isEmpty() || processorListRegistyOptional.isEmpty()) {
            return;
        }

        Registry<StructureTemplatePool> templatePools = templateRegistryOptional.get();
        Registry<StructureProcessorList> processorLists = processorListRegistyOptional.get();
        createVillagePiece(templatePools, processorLists, "plains", "bard_house", 1, 2, ProcessorLists.MOSSIFY_10_PERCENT, ProcessorLists.ZOMBIE_PLAINS);
        createVillagePiece(templatePools, processorLists, "desert", "bard_house", 1, 2, ProcessorLists.ZOMBIE_DESERT);
        createVillagePiece(templatePools, processorLists, "savanna", "bard_house", 1, 4, ProcessorLists.ZOMBIE_SAVANNA);
        createVillagePiece(templatePools, processorLists, "snowy", "bard_house", 1, 4, ProcessorLists.ZOMBIE_SNOWY);
        createVillagePiece(templatePools, processorLists, "taiga", "bard_house", 1, 4, ProcessorLists.MOSSIFY_10_PERCENT, ProcessorLists.ZOMBIE_TAIGA);
    }

    private static void createVillagePiece(Registry<StructureTemplatePool> templatePools, Registry<StructureProcessorList> processorLists, String village, String name, int houseId, int weight, ResourceKey<StructureProcessorList> zombieProcessor) {
        createVillagePiece(templatePools, processorLists, village, name, houseId, weight, ProcessorLists.EMPTY, zombieProcessor);
    }

    private static void createVillagePiece(Registry<StructureTemplatePool> templatePools, Registry<StructureProcessorList> processorLists, String village, String name, int houseId, int weight, ResourceKey<StructureProcessorList> normalProcessor, ResourceKey<StructureProcessorList> zombieProcessor) {
        EtchedVillagers.addToPool(templatePools.get(new ResourceLocation("village/" + village + "/houses")), new ResourceLocation(Etched.MOD_ID, "village/" + village + "/houses/" + village + "_" + name + "_" + houseId), processorLists.getHolder(normalProcessor).orElse(null), weight);
        EtchedVillagers.addToPool(templatePools.get(new ResourceLocation("village/" + village + "/zombie/houses")), new ResourceLocation(Etched.MOD_ID, "village/" + village + "/houses/" + village + "_" + name + "_" + houseId), processorLists.getHolder(zombieProcessor).orElse(null), weight);
    }

    private static void addToPool(@Nullable StructureTemplatePool pool, ResourceLocation pieceId, @Nullable Holder<StructureProcessorList> processorList, int weight) {
        if (pool == null || processorList == null) {
            return;
        }

        StructurePoolElement piece = StructurePoolElement.legacy(pieceId.toString(), processorList).apply(StructureTemplatePool.Projection.RIGID);
        List<StructurePoolElement> templates = ((StructureTemplatePoolAccessor) pool).getTemplates();
        if (templates == null) {
            return;
        }

        for (int i = 0; i < weight; i++) {
            templates.add(piece);
        }
    }

    private static class TradeRegistry implements List<VillagerTrades.ItemListing> {

        private final List<VillagerTrades.ItemListing> trades;

        public TradeRegistry() {
            this.trades = NonNullList.create();
        }

        @Override
        public int size() {
            return this.trades.size();
        }

        @Override
        public boolean isEmpty() {
            return this.trades.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return this.trades.contains(o);
        }

        @NotNull
        @Override
        public Iterator<VillagerTrades.ItemListing> iterator() {
            return this.trades.iterator();
        }

        @NotNull
        @Override
        public Object[] toArray() {
            return this.trades.toArray();
        }

        @NotNull
        @Override
        public <T> T[] toArray(@NotNull T[] a) {
            return this.trades.toArray(a);
        }

        @Override
        public boolean add(VillagerTrades.ItemListing listing) {
            return this.trades.add(listing);
        }

        @Override
        public boolean remove(Object o) {
            return this.trades.remove(o);
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            return this.trades.containsAll(c);
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends VillagerTrades.ItemListing> c) {
            return this.trades.addAll(c);
        }

        @Override
        public boolean addAll(int index, @NotNull Collection<? extends VillagerTrades.ItemListing> c) {
            return this.trades.addAll(index, c);
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            return this.trades.removeAll(c);
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            return this.trades.retainAll(c);
        }

        @Override
        public void clear() {
            this.trades.clear();
        }

        @Override
        public VillagerTrades.ItemListing get(int index) {
            return this.trades.get(index);
        }

        @Override
        public VillagerTrades.ItemListing set(int index, VillagerTrades.ItemListing element) {
            return this.trades.set(index, element);
        }

        @Override
        public void add(int index, VillagerTrades.ItemListing element) {
            this.trades.add(index, element);
        }

        @Override
        public VillagerTrades.ItemListing remove(int index) {
            return this.trades.remove(index);
        }

        @Override
        public int indexOf(Object o) {
            return this.trades.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            return this.trades.lastIndexOf(o);
        }

        @NotNull
        @Override
        public ListIterator<VillagerTrades.ItemListing> listIterator() {
            return this.trades.listIterator();
        }

        @NotNull
        @Override
        public ListIterator<VillagerTrades.ItemListing> listIterator(int index) {
            return this.trades.listIterator(index);
        }

        @NotNull
        @Override
        public List<VillagerTrades.ItemListing> subList(int fromIndex, int toIndex) {
            return this.trades.subList(fromIndex, toIndex);
        }

        /**
         * Adds a simple trade for items or emeralds.
         *
         * @param item           The item to trade for
         * @param emeralds       The amount of emeralds to trade
         * @param itemCount      The amount of the item to trade
         * @param maxUses        The maximum amount of times this trade can be used before needing to reset
         * @param xpGain         The amount of experience gained by this exchange
         * @param sellToVillager Whether the villager is buying or selling the item for emeralds
         */
        public void add(ItemLike item, int emeralds, int itemCount, int maxUses, int xpGain, boolean sellToVillager) {
            this.add(new ItemTrade(() -> item, emeralds, itemCount, maxUses, xpGain, 0.05F, sellToVillager));
        }

        /**
         * Adds a simple trade for items or emeralds.
         *
         * @param item            The item to trade for
         * @param emeralds        The amount of emeralds to trade
         * @param itemCount       The amount of the item to trade
         * @param maxUses         The maximum amount of times this trade can be used before needing to reset
         * @param xpGain          The amount of experience gained by this exchange
         * @param priceMultiplier The multiplier for how much the price deviates
         * @param sellToVillager  Whether the villager is buying or selling the item for emeralds
         */
        public void add(ItemLike item, int emeralds, int itemCount, int maxUses, int xpGain, float priceMultiplier, boolean sellToVillager) {
            this.add(new ItemTrade(() -> item, emeralds, itemCount, maxUses, xpGain, priceMultiplier, sellToVillager));
        }

        /**
         * Adds a simple trade for items or emeralds.
         *
         * @param item           The item to trade for as a supplier
         * @param emeralds       The amount of emeralds to trade
         * @param itemCount      The amount of the item to trade
         * @param maxUses        The maximum amount of times this trade can be used before needing to reset
         * @param xpGain         The amount of experience gained by this exchange
         * @param sellToVillager Whether the villager is buying or selling the item for emeralds
         */
        public void add(Supplier<? extends ItemLike> item, int emeralds, int itemCount, int maxUses, int xpGain, boolean sellToVillager) {
            this.add(new ItemTrade(item, emeralds, itemCount, maxUses, xpGain, 0.05F, sellToVillager));
        }

        /**
         * Adds a simple trade for items or emeralds.
         *
         * @param item            The item to trade for as a supplier
         * @param emeralds        The amount of emeralds to trade
         * @param itemCount       The amount of the item to trade
         * @param maxUses         The maximum amount of times this trade can be used before needing to reset
         * @param xpGain          The amount of experience gained by this exchange
         * @param priceMultiplier The multiplier for how much the price deviates
         * @param sellToVillager  Whether the villager is buying or selling the item for emeralds
         */
        public void add(Supplier<? extends ItemLike> item, int emeralds, int itemCount, int maxUses, int xpGain, float priceMultiplier, boolean sellToVillager) {
            this.add(new ItemTrade(item, emeralds, itemCount, maxUses, xpGain, priceMultiplier, sellToVillager));
        }
    }

    private static class ItemTrade implements VillagerTrades.ItemListing {

        private final Supplier<? extends ItemLike> item;
        private final int emeralds;
        private final int itemCount;
        private final int maxUses;
        private final int xpGain;
        private final float priceMultiplier;
        private final boolean sellToVillager;

        private ItemTrade(Supplier<? extends ItemLike> Item, int emeralds, int itemCount, int maxUses, int xpGain, float priceMultiplier, boolean sellToVillager) {
            this.item = Item;
            this.emeralds = emeralds;
            this.itemCount = itemCount;
            this.maxUses = maxUses;
            this.xpGain = xpGain;
            this.priceMultiplier = priceMultiplier;
            this.sellToVillager = sellToVillager;
        }

        @Override
        public MerchantOffer getOffer(Entity entity, RandomSource random) {
            ItemStack emeralds = new ItemStack(Items.EMERALD, this.emeralds);
            ItemStack item = new ItemStack(this.item.get(), this.itemCount);

            return new MerchantOffer(this.sellToVillager ? item : emeralds, this.sellToVillager ? emeralds : item, this.maxUses, this.xpGain, this.priceMultiplier);
        }
    }
}
