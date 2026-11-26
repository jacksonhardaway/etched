package gg.moonflower.etched.core.registry;

import gg.moonflower.etched.core.Etched;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EtchedCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Etched.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ETCHED_TAB = TABS.register("etched", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("creative_tab.etched_main"))
                    .icon(() -> EtchedItems.ETCHED_MUSIC_DISC.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(EtchedItems.BLANK_MUSIC_DISC);
                        output.accept(EtchedItems.MUSIC_LABEL);
                        output.accept(EtchedItems.ALBUM_COVER);

                        output.accept(EtchedItems.BOOMBOX);
                        output.accept(EtchedItems.JUKEBOX_MINECART);
                        output.accept(EtchedBlocks.PORTAL_RADIO_ITEM);

                        output.accept(EtchedBlocks.ETCHING_TABLE);
                        output.accept(EtchedBlocks.ALBUM_JUKEBOX);
                        output.accept(EtchedBlocks.RADIO);
                    })
                    .build());

    private EtchedCreativeTab() {
    }
}