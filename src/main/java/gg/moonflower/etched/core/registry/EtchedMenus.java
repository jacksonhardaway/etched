package gg.moonflower.etched.core.registry;

import gg.moonflower.etched.common.menu.*;
import gg.moonflower.etched.core.Etched;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class EtchedMenus {

    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.MENU, Etched.MOD_ID);

    public static final Supplier<MenuType<EtchingMenu>> ETCHING_MENU = REGISTRY.register("etching_table", () -> new MenuType<>(EtchingMenu::new, FeatureFlags.VANILLA_SET));
    public static final Supplier<MenuType<AlbumJukeboxMenu>> ALBUM_JUKEBOX_MENU = REGISTRY.register("album_jukebox", () -> new MenuType<>((IContainerFactory<AlbumJukeboxMenu>) (windowId, inv, data) -> new AlbumJukeboxMenu(windowId, inv, data.readBlockPos(), data.readFloat(), data.readVarInt(), data.readVarInt()), FeatureFlags.VANILLA_SET));
    public static final Supplier<MenuType<BoomboxMenu>> BOOMBOX_MENU = REGISTRY.register("boombox", () -> new MenuType<>(BoomboxMenu::new, FeatureFlags.VANILLA_SET));
    public static final Supplier<MenuType<AlbumCoverMenu>> ALBUM_COVER_MENU = REGISTRY.register("album_cover", () -> new MenuType<>(AlbumCoverMenu::new, FeatureFlags.VANILLA_SET));
    public static final Supplier<MenuType<RadioMenu>> RADIO_MENU = REGISTRY.register("radio", () -> new MenuType<>((IContainerFactory<RadioMenu>) (windowId, inv, data) -> new RadioMenu(windowId, data.readUtf(), data.readFloat(), data.readVarInt()), FeatureFlags.VANILLA_SET));

}
