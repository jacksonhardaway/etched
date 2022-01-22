package gg.moonflower.etched.common.item;

import gg.moonflower.etched.client.screen.EditMusicLabelScreen;
import gg.moonflower.etched.core.Etched;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SimpleMusicLabelItem extends Item {

    public SimpleMusicLabelItem(Properties properties) {
        super(properties);
    }

    public static String getAuthor(ItemStack stack) {
        if (!(stack.getItem() instanceof SimpleMusicLabelItem))
            return "";
        return stack.getOrCreateTagElement("Label").getString("Author");
    }

    public static String getTitle(ItemStack stack) {
        if (!(stack.getItem() instanceof SimpleMusicLabelItem))
            return "";
        return stack.getOrCreateTagElement("Label").getString("Title");
    }

    public static void setAuthor(ItemStack stack, String author) {
        if (!(stack.getItem() instanceof SimpleMusicLabelItem))
            return;

        CompoundTag tag = stack.getOrCreateTagElement("Label");
        tag.putString("Author", author);
    }

    public static void setTitle(ItemStack stack, String title) {
        if (!(stack.getItem() instanceof SimpleMusicLabelItem))
            return;

        CompoundTag tag = stack.getOrCreateTagElement("Label");
        tag.putString("Title", title);
    }

    @Environment(EnvType.CLIENT)
    private void openMusicLabelEditScreen(Player player, InteractionHand hand, ItemStack stack) {
        Minecraft.getInstance().setScreen(new EditMusicLabelScreen(player, hand, stack));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            openMusicLabelEditScreen(player, hand, stack);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack itemStack, Level level, Entity entity, int i, boolean bl) {
        if (getAuthor(itemStack).isEmpty())
            setAuthor(itemStack, entity.getDisplayName().getString());
        if (getTitle(itemStack).isEmpty())
            setTitle(itemStack, "Custom Music");
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> list, TooltipFlag tooltipFlag) {
        if (!getAuthor(itemStack).isEmpty() && !getTitle(itemStack).isEmpty()) {
            list.add(new TranslatableComponent("sound_source." + Etched.MOD_ID + ".info", getAuthor(itemStack), getTitle(itemStack)).withStyle(ChatFormatting.GRAY));
        }
    }
}
