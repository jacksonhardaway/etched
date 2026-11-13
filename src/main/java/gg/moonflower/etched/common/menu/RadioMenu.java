package gg.moonflower.etched.common.menu;

import gg.moonflower.etched.common.blockentity.RadioBlockEntity;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import gg.moonflower.etched.core.registry.EtchedMenus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/**
 * @author Ocelot
 */
public class RadioMenu extends AbstractContainerMenu implements UrlMenu {

    private final ContainerLevelAccess access;
    private String initialUrl;
    private int initialVolumePercent = RadioBlockEntity.DEFAULT_VOLUME_PERCENT;
    private int initialRange = RadioBlockEntity.DEFAULT_RANGE;

    public RadioMenu(int id, String url, int volumePercent, int range) {
        this(id, ContainerLevelAccess.NULL);
        this.initialUrl = url;
        this.initialVolumePercent = volumePercent;
        this.initialRange = range;
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

    public int getInitialVolumePercent() {
        return this.initialVolumePercent;
    }

    public int getInitialRange() {
        return this.initialRange;
    }

    public void setBroadcastSettings(int volumePercent, int range) {
        this.access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof RadioBlockEntity be) {
                be.setBroadcastSettings(volumePercent, range);
            }
        });
    }
}
