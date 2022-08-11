package gg.moonflower.etched.core.registry;

import gg.moonflower.etched.core.Etched;
import gg.moonflower.pollen.api.registry.resource.TagRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.block.Block;

public class EtchedTags {

    public static final Tag<Block> AUDIO_PROVIDER = TagRegistry.bindBlock(new ResourceLocation(Etched.MOD_ID, "audio_providers"));
}
