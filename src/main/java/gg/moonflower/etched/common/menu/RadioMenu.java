package gg.moonflower.etched.common.menu;

import gg.moonflower.etched.common.blockentity.RadioBlockEntity;
import gg.moonflower.etched.common.radio.RadioStation;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import gg.moonflower.etched.core.registry.EtchedMenus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

/**
 * @author Ocelot
 */
public class RadioMenu extends AbstractContainerMenu implements UrlMenu {

    private final ContainerLevelAccess access;
    private String initialUrl;
    private List<RadioStation> initialPresets = Collections.nCopies(
            RadioBlockEntity.PRESET_COUNT, RadioStation.EMPTY);

    public RadioMenu(int id, String url, List<RadioStation> presets) {
        this(id, ContainerLevelAccess.NULL);
        this.initialUrl = url;
        if (presets.size() == RadioBlockEntity.PRESET_COUNT) {
            this.initialPresets = List.copyOf(presets);
        }
    }

    public RadioMenu(int id, ContainerLevelAccess access) {
        super(EtchedMenus.RADIO_MENU.get(), id);
        this.access = access;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, EtchedBlocks.RADIO.get());
    }

    @Override
    public void setUrl(String url) {
        this.access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof RadioBlockEntity be) {
                be.setUrl(url);
            }
        });
    }

    /**
     * @return The original URL inside the radio before opening the menu
     */
    public String getInitialUrl() {
        return this.initialUrl;
    }

    public List<RadioStation> getInitialPresets() {
        return this.initialPresets;
    }

    public void setPreset(int index, RadioStation station, boolean activate) {
        this.access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof RadioBlockEntity be) {
                be.setPreset(index, station);
                if (activate) {
                    be.usePreset(index);
                }
            }
        });
    }
}
