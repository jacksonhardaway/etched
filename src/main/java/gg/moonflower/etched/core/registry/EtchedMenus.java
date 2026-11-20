package gg.moonflower.etched.core.registry;

import gg.moonflower.etched.common.blockentity.RadioBlockEntity;
import gg.moonflower.etched.common.menu.*;
import gg.moonflower.etched.common.radio.RadioStation;
import gg.moonflower.etched.common.radio.RadioStationSerialization;
import gg.moonflower.etched.core.Etched;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.function.Supplier;

public class EtchedMenus {

    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.MENU, Etched.MOD_ID);

    public static final Supplier<MenuType<EtchingMenu>> ETCHING_MENU = REGISTRY.register("etching_table", () -> new MenuType<>(EtchingMenu::new, FeatureFlags.VANILLA_SET));
    public static final Supplier<MenuType<AlbumJukeboxMenu>> ALBUM_JUKEBOX_MENU = REGISTRY.register("album_jukebox", () -> new MenuType<>((IContainerFactory<AlbumJukeboxMenu>) (windowId, inv, data) -> new AlbumJukeboxMenu(windowId, inv, data.readBlockPos()), FeatureFlags.VANILLA_SET));
    public static final Supplier<MenuType<BoomboxMenu>> BOOMBOX_MENU = REGISTRY.register("boombox", () -> new MenuType<>(BoomboxMenu::new, FeatureFlags.VANILLA_SET));
    public static final Supplier<MenuType<AlbumCoverMenu>> ALBUM_COVER_MENU = REGISTRY.register("album_cover", () -> new MenuType<>(AlbumCoverMenu::new, FeatureFlags.VANILLA_SET));
    public static final Supplier<MenuType<RadioMenu>> RADIO_MENU = REGISTRY.register("radio", () -> new MenuType<>((IContainerFactory<RadioMenu>) (windowId, inv, data) -> {
        String url = data.readUtf();
        int presetCount = Math.min(data.readVarInt(), RadioBlockEntity.PRESET_COUNT);
        ArrayList<RadioStation> presets = new ArrayList<>(RadioBlockEntity.PRESET_COUNT);
        for (int i = 0; i < presetCount; i++) {
            presets.add(RadioStationSerialization.STREAM_CODEC.decode(data));
        }
        while (presets.size() < RadioBlockEntity.PRESET_COUNT) {
            presets.add(RadioStation.EMPTY);
        }
        return new RadioMenu(windowId, url, presets);
    }, FeatureFlags.VANILLA_SET));

}
