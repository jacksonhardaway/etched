package me.jaackson.etched.common.menu;

import com.mojang.datafixers.util.Pair;
import me.jaackson.etched.Etched;
import me.jaackson.etched.EtchedRegistry;
import me.jaackson.etched.common.item.BlankMusicDiscItem;
import me.jaackson.etched.common.item.EtchedMusicDiscItem;
import me.jaackson.etched.common.item.MusicLabelItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class EtchingMenu extends AbstractContainerMenu {
    public static final ResourceLocation EMPTY_SLOT_MUSIC_DISC = new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_disc");
    public static final ResourceLocation EMPTY_SLOT_MUSIC_LABEL = new ResourceLocation(Etched.MOD_ID, "item/empty_etching_table_slot_music_label");

    private final ContainerLevelAccess access;
    private final DataSlot labelIndex;
    private final Slot discSlot;
    private final Slot labelSlot;
    private final Slot resultSlot;
    private final Container input;
    private final Container result;
    private final String author;

    private String url;

    private final Runnable slotUpdateListener = () -> {
    };
    private long lastSoundTime;

    public EtchingMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public EtchingMenu(int id, Inventory inventory, ContainerLevelAccess containerLevelAccess) {
        super(EtchedRegistry.ETCHING_MENU.get(), id);
        this.author = inventory.player.getDisplayName().getString();
        this.labelIndex = DataSlot.standalone();
        this.input = new SimpleContainer(2) {
            public void setChanged() {
                super.setChanged();
                EtchingMenu.this.slotsChanged(this);
                EtchingMenu.this.slotUpdateListener.run();
            }
        };
        this.result = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                EtchingMenu.this.slotUpdateListener.run();
            }
        };

        this.access = containerLevelAccess;

        this.discSlot = this.addSlot(new Slot(this.input, 0, 44, 43) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == EtchedRegistry.BLANK_MUSIC_DISC.get() || stack.getItem() == EtchedRegistry.ETCHED_MUSIC_DISC.get();
            }

            @Override
            @Environment(EnvType.CLIENT)
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_SLOT_MUSIC_DISC);
            }
        });
        this.labelSlot = this.addSlot(new Slot(this.input, 1, 62, 43) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof MusicLabelItem;
            }

            @Override
            @Environment(EnvType.CLIENT)
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_SLOT_MUSIC_LABEL);
            }
        });

        this.resultSlot = this.addSlot(new Slot(this.result, 0, 116, 43) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public ItemStack onTake(Player player, ItemStack stack) {
                EtchingMenu.this.discSlot.remove(1);
                EtchingMenu.this.labelSlot.remove(1);
                if (!EtchingMenu.this.discSlot.hasItem() || !EtchingMenu.this.labelSlot.hasItem()) {
                    EtchingMenu.this.labelIndex.set(0);
                }

                containerLevelAccess.execute((level, pos) -> {
                    long l = level.getGameTime();
                    if (EtchingMenu.this.lastSoundTime != l) {
                        level.playSound(null, pos, EtchedRegistry.UI_ETCHER_TAKE_RESULT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                        EtchingMenu.this.lastSoundTime = l;
                    }

                });
                return super.onTake(player, stack);
            }
        });

        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(inventory, x + y * 9 + 9, 8 + x * 18, 98 + y * 18));
            }
        }
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(inventory, x, 8 + x * 18, 156));
        }

        this.addDataSlot(this.labelIndex);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, EtchedRegistry.ETCHING_TABLE.get());
    }

    public boolean clickMenuButton(Player player, int index) {
        if (index >= 0 && index < 6) {
            this.labelIndex.set(index);
            this.setupResultSlot();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void slotsChanged(Container container) {
        ItemStack discStack = this.discSlot.getItem();
        ItemStack labelStack = this.labelSlot.getItem();
        ItemStack resultStack = this.resultSlot.getItem();

        if (resultStack.isEmpty() && labelStack.isEmpty()) {
            if (!discStack.isEmpty() && discStack.getItem() == EtchedRegistry.ETCHED_MUSIC_DISC.get()) {
                this.labelIndex.set(EtchedMusicDiscItem.getPattern(discStack).ordinal());
            } else {
                this.labelIndex.set(0);
            }
        }

        this.setupResultSlot();
        this.broadcastChanges();
    }

    private void setupResultSlot() {
        this.resultSlot.set(ItemStack.EMPTY);
        if (this.labelIndex.get() >= 0 && this.labelIndex.get() < EtchedMusicDiscItem.LabelPattern.values().length) {
            ItemStack discStack = this.discSlot.getItem();
            ItemStack labelStack = this.labelSlot.getItem();
            ItemStack resultStack = ItemStack.EMPTY;

            if (discStack.getItem() == EtchedRegistry.ETCHED_MUSIC_DISC.get() || (!discStack.isEmpty() && !labelStack.isEmpty())) {
                resultStack = new ItemStack(EtchedRegistry.ETCHED_MUSIC_DISC.get());
                resultStack.setCount(1);

                int discColor = 0x515151;
                int labelColor = 0xFFFFFF;
                if (discStack.getItem() == EtchedRegistry.ETCHED_MUSIC_DISC.get()) {
                    discColor = EtchedMusicDiscItem.getPrimaryColor(discStack);
                    labelColor = EtchedMusicDiscItem.getSecondaryColor(discStack);
                }
                if (discStack.getItem() instanceof BlankMusicDiscItem)
                    discColor = ((BlankMusicDiscItem) discStack.getItem()).getColor(discStack);
                if (labelStack.getItem() instanceof MusicLabelItem)
                    labelColor = ((MusicLabelItem) labelStack.getItem()).getColor(labelStack);

                EtchedMusicDiscItem.MusicInfo info = new EtchedMusicDiscItem.MusicInfo();
                if (discStack.getItem() == EtchedRegistry.BLANK_MUSIC_DISC.get())
                    info.setAuthor(this.author);
                if (labelStack.hasCustomHoverName())
                    info.setTitle(labelStack.getHoverName().getString());
                info.setUrl(this.url);

                EtchedMusicDiscItem.setMusic(resultStack, info);
                EtchedMusicDiscItem.setColor(resultStack, discColor, labelColor);
                EtchedMusicDiscItem.setPattern(resultStack, EtchedMusicDiscItem.LabelPattern.values()[this.labelIndex.get()]);
            }

            if (!ItemStack.matches(resultStack, this.resultSlot.getItem())) {
                this.resultSlot.set(resultStack);
            }
        }

    }

    public int getLabelIndex() {
        return labelIndex.get();
    }

    public void setUrl(String string) {
        this.url = string;
        this.setupResultSlot();
    }
}
