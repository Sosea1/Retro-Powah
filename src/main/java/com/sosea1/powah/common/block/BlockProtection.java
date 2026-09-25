package com.sosea1.powah.common.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;

/** Shared Forge-1.12 protection checks for actions that bypass vanilla mining. */
public final class BlockProtection {
    private BlockProtection() { }

    public static boolean canEdit(EntityPlayer player, World world, BlockPos pos,
                                  EnumFacing side, ItemStack tool) {
        return world.isRemote || (player.canPlayerEdit(pos, side, tool)
                && world.isBlockModifiable(player, pos));
    }

    public static boolean canBreak(EntityPlayer player, World world, BlockPos pos) {
        if (world.isRemote) return true;
        if (!world.isBlockModifiable(player, pos)) return false;
        IBlockState state = world.getBlockState(pos);
        return !MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(world, pos, state, player));
    }
}
