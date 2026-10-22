package gg.moonflower.etched.core;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import gg.moonflower.etched.core.registry.EtchedComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddAttributeTooltipsEvent;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;

import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT, modid = Etched.MOD_ID)
public class EtchedClientEvents {

    private static final Component HOLD_SHIFT = Component.translatable("tooltip." + Etched.MOD_ID + ".supported_sites.hold_shift", Component.literal("Shift").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY);
    private static final Component SUPPORTED_SITES = Component.translatable("tooltip." + Etched.MOD_ID + ".supported_sites").withStyle(ChatFormatting.GRAY);
    private static final List<Component> SITES = List.of(
            Component.literal("SoundCloud, Bandcamp, Audius"),
            Component.translatable("tooltip." + Etched.MOD_ID + ".supported_sites.yandex_music"),
            Component.literal("Internet Archive"),
            Component.literal("Google Drive, Dropbox"),
            Component.translatable("tooltip." + Etched.MOD_ID + ".supported_sites.direct"));
    private static final Component RADIO_STREAMS = Component.translatable("tooltip." + Etched.MOD_ID + ".supported_sites.radio_streams");

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onItemTooltip(AddAttributeTooltipsEvent event) {
        AttributeTooltipContext context = event.getContext();
        ItemStack stack = event.getStack();

        stack.addToTooltip(EtchedComponents.ALBUM_COVER, context, event::addTooltipLines, context.flag());
        stack.addToTooltip(EtchedComponents.MUSIC_LABEL, context, event::addTooltipLines, context.flag());
        stack.addToTooltip(EtchedComponents.PLAYING_RECORD, context, event::addTooltipLines, context.flag());
        PlayableRecord.addToTooltip(stack, context, event::addTooltipLines);
        stack.addToTooltip(EtchedComponents.PAUSED, context, event::addTooltipLines, context.flag());

        boolean radio = stack.is(EtchedBlocks.RADIO.asItem()) || stack.is(EtchedBlocks.PORTAL_RADIO_ITEM.get());
        if (radio || stack.is(EtchedBlocks.ETCHING_TABLE.asItem())) {
            if (Screen.hasShiftDown()) {
                event.addTooltipLines(SUPPORTED_SITES);
                if (radio) { // Only radios can play live streams
                    event.addTooltipLines(Component.literal(" • ").append(RADIO_STREAMS).withStyle(ChatFormatting.DARK_GRAY));
                }
                SITES.forEach(site -> event.addTooltipLines(Component.literal(" • ").append(site).withStyle(ChatFormatting.DARK_GRAY)));
            } else {
                event.addTooltipLines(HOLD_SHIFT);
            }
        }
    }
}
