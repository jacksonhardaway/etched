package gg.moonflower.etched.core.registry;

import gg.moonflower.etched.common.menu.*;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.pollen.api.registry.PollinatedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

public class EtchedMenus {

    public static final PollinatedRegistry<MenuType<?>> MENUS = PollinatedRegistry.create(Registry.MENU, Etched.MOD_ID);

    public static final Supplier<MenuType<EtchingMenu>> ETCHING_MENU = MENUS.register("etching_table", () -> new MenuType<>(EtchingMenu::new));
    public static final Supplier<MenuType<AlbumJukeboxMenu>> ALBUM_JUKEBOX_MENU = MENUS.register("album_jukebox", () -> new MenuType<>(AlbumJukeboxMenu::new));
    public static final Supplier<MenuType<BoomboxMenu>> BOOMBOX_MENU = MENUS.register("boombox", () -> new MenuType<>(BoomboxMenu::new));
    public static final Supplier<MenuType<AlbumCoverMenu>> ALBUM_COVER_MENU = MENUS.register("album_cover", () -> new MenuType<>(AlbumCoverMenu::new));
    public static final Supplier<MenuType<RadioMenu>> RADIO_MENU = MENUS.register("radio", () -> new MenuType<>(RadioMenu::new));

}
