package gg.moonflower.etched.core.registry;

import gg.moonflower.etched.common.menu.*;
import gg.moonflower.etched.core.Etched;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class EtchedMenus {

    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Etched.MOD_ID);

    public static final Supplier<MenuType<EtchingMenu>> ETCHING_MENU = REGISTRY.register("etching_table", () -> new MenuType<>(EtchingMenu::new, FeatureFlags.VANILLA_SET));
    public static final Supplier<MenuType<AlbumJukeboxMenu>> ALBUM_JUKEBOX_MENU = REGISTRY.register("album_jukebox",
            () -> IForgeMenuType.create((IContainerFactory<AlbumJukeboxMenu>) (windowId, inv, buf) -> {
                BlockPos pos = buf.readBlockPos();
                return new AlbumJukeboxMenu(windowId, inv, new SimpleContainer(9), pos);
            })
    );
    public static final Supplier<MenuType<BoomboxMenu>> BOOMBOX_MENU = REGISTRY.register("boombox", () -> new MenuType<>(BoomboxMenu::new, FeatureFlags.VANILLA_SET));
    public static final Supplier<MenuType<AlbumCoverMenu>> ALBUM_COVER_MENU = REGISTRY.register("album_cover", () -> new MenuType<>(AlbumCoverMenu::new, FeatureFlags.VANILLA_SET));
    public static final Supplier<MenuType<RadioMenu>> RADIO_MENU = REGISTRY.register("radio", () -> new MenuType<>(RadioMenu::new, FeatureFlags.VANILLA_SET));

}
