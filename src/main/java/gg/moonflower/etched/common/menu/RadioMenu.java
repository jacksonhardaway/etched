package gg.moonflower.etched.common.menu;

import gg.moonflower.etched.common.blockentity.RadioBlockEntity;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import gg.moonflower.etched.core.registry.EtchedMenus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/**
 * @author Ocelot
 */
public class RadioMenu extends AbstractContainerMenu implements UrlMenu, SoundSettingsMenu {

    private final Predicate<Player> validator;
    private final Consumer<String> urlSetter;
    private final Consumer<Float> volumeSetter;
    private final IntConsumer radiusSetter;
    private String initialUrl;
    private float initialVolume;
    private int initialRadius;

    public RadioMenu(int id, String url, float volume, int radius) {
        this(id, ContainerLevelAccess.NULL);
        this.initialUrl = url;
        this.initialVolume = volume;
        this.initialRadius = radius;
    }

    public RadioMenu(int id, ContainerLevelAccess access) {
        this(id, player -> stillValid(access, player, EtchedBlocks.RADIO.get()), url -> access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof RadioBlockEntity be) {
                be.setUrl(url);
            }
        }), volume -> access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof RadioBlockEntity be) {
                be.setVolume(volume);
            }
        }), radius -> access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof RadioBlockEntity be) {
                be.setRadius(radius);
            }
        }));
    }

    /**
     * Creates a radio menu for a radio that is not a placed block, such as one mounted on a moving contraption.
     *
     * @param id        The id of the menu
     * @param validator Whether the player can still use the menu
     * @param urlSetter Applies the URL chosen by the player
     */
    public RadioMenu(int id, Predicate<Player> validator, Consumer<String> urlSetter) {
        this(id, validator, urlSetter, volume -> {
        }, radius -> {
        });
    }

    /**
     * Creates a radio menu for a radio that is not a placed block, such as one mounted on a moving contraption.
     *
     * @param id           The id of the menu
     * @param validator    Whether the player can still use the menu
     * @param urlSetter    Applies the URL chosen by the player
     * @param volumeSetter Applies the volume chosen by the player
     * @param radiusSetter Applies the radius chosen by the player
     * @since 5.5.0
     */
    public RadioMenu(int id, Predicate<Player> validator, Consumer<String> urlSetter, Consumer<Float> volumeSetter, IntConsumer radiusSetter) {
        super(EtchedMenus.RADIO_MENU.get(), id);
        this.validator = validator;
        this.urlSetter = urlSetter;
        this.volumeSetter = volumeSetter;
        this.radiusSetter = radiusSetter;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.validator.test(player);
    }

    @Override
    public void setUrl(String url) {
        this.urlSetter.accept(url);
    }

    @Override
    public void setVolume(float volume) {
        this.volumeSetter.accept(volume);
    }

    @Override
    public void setRadius(int radius) {
        this.radiusSetter.accept(radius);
    }

    /**
     * @return The original URL inside the radio before opening the menu
     */
    public String getInitialUrl() {
        return this.initialUrl;
    }

    /**
     * @return The volume of the radio before opening the menu
     */
    public float getInitialVolume() {
        return this.initialVolume;
    }

    /**
     * @return The radius of the radio before opening the menu
     */
    public int getInitialRadius() {
        return this.initialRadius;
    }
}