package gg.moonflower.etched.core.registry;

import dev.architectury.registry.registries.DeferredRegister;
import gg.moonflower.etched.common.menu.*;
import gg.moonflower.etched.core.Etched;
import net.minecraft.core.Registry;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

public class EtchedMenus {

    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Etched.MOD_ID, Registry.MENU_REGISTRY);

    public static final Supplier<MenuType<EtchingMenu>> ETCHING_MENU = REGISTRY.register("etching_table", () -> new MenuType<>(EtchingMenu::new));
    public static final Supplier<MenuType<AlbumJukeboxMenu>> ALBUM_JUKEBOX_MENU = REGISTRY.register("album_jukebox", () -> new MenuType<>(AlbumJukeboxMenu::new));
    public static final Supplier<MenuType<BoomboxMenu>> BOOMBOX_MENU = REGISTRY.register("boombox", () -> new MenuType<>(BoomboxMenu::new));
    public static final Supplier<MenuType<AlbumCoverMenu>> ALBUM_COVER_MENU = REGISTRY.register("album_cover", () -> new MenuType<>(AlbumCoverMenu::new));
    public static final Supplier<MenuType<RadioMenu>> RADIO_MENU = REGISTRY.register("radio", () -> new MenuType<>(RadioMenu::new));

}
