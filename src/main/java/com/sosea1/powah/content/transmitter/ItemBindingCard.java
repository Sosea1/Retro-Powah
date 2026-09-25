package com.sosea1.powah.content.transmitter;

import com.sosea1.powah.registry.PowahCreativeTab;

import java.util.List;
import java.util.UUID;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import com.sosea1.powah.Powah;

/** 1.12 NBT-backed binding card. A normal card binds itself to the player on use. */
public final class ItemBindingCard extends Item {
    private static final String NBT_MOST = "BoundPlayerMost";
    private static final String NBT_LEAST = "BoundPlayerLeast";
    private static final String NBT_NAME = "BoundPlayerName";
    private final boolean dimensional;

    public ItemBindingCard(boolean dimensional) {
        this.dimensional = dimensional;
        setMaxStackSize(1);
        setCreativeTab(PowahCreativeTab.INSTANCE);
        addPropertyOverride(new net.minecraft.util.ResourceLocation(Powah.MOD_ID, "bound"),
                (stack, world, entity) -> isBound(stack) ? 1.0F : 0.0F);
    }

    public boolean isDimensional() { return dimensional; }
    public boolean isBound(ItemStack stack) { return getBoundPlayer(stack) != null; }
    public UUID getBoundPlayer(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) return null;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(NBT_MOST) || !tag.hasKey(NBT_LEAST)) return null;
        return new UUID(tag.getLong(NBT_MOST), tag.getLong(NBT_LEAST));
    }
    public String getBoundName(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        return tag == null ? "" : tag.getString(NBT_NAME);
    }
    public void bind(ItemStack stack, UUID playerId, String playerName) {
        if (stack == null || stack.isEmpty() || playerId == null) return;
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setLong(NBT_MOST, playerId.getMostSignificantBits());
        tag.setLong(NBT_LEAST, playerId.getLeastSignificantBits());
        tag.setString(NBT_NAME, playerName == null ? "" : playerName);
        stack.setTagCompound(tag);
    }

    @Override public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        UUID current = getBoundPlayer(stack);
        if (current == null) {
            if (!world.isRemote) bind(stack, player.getUniqueID(), player.getName());
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
        }
        if (!current.equals(player.getUniqueID())) {
            if (!world.isRemote) {
                TextComponentTranslation message = new TextComponentTranslation("chat.powah.no.binding", getBoundName(stack));
                message.getStyle().setColor(TextFormatting.DARK_RED);
                player.sendStatusMessage(message, true);
            }
            return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
        }
        return new ActionResult<ItemStack>(EnumActionResult.PASS, stack);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        UUID owner = getBoundPlayer(stack);
        if (owner == null) {
            tooltip.add(TextFormatting.DARK_GRAY + new TextComponentTranslation("info.powah.click.to.bind").getUnformattedText());
        } else {
            tooltip.add(TextFormatting.GRAY + "Owner: " + TextFormatting.YELLOW + getBoundName(stack));
        }
    }
}
