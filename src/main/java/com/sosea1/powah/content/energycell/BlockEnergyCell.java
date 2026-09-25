package com.sosea1.powah.content.energycell;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.stats.StatList;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import com.sosea1.powah.common.block.BlockPowahMachine;
import com.sosea1.powah.common.tier.PowahTier;

public final class BlockEnergyCell extends BlockPowahMachine {
    public BlockEnergyCell(PowahTier tier) { super(tier, 4.0F, 10.0F); }
    @Override protected TileEntity createPowahTile(PowahTier tier) { return new TileEnergyCell(tier); }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        TileEntity raw = world.getTileEntity(pos);
        if (raw instanceof TileEnergyCell) {
            TileEnergyCell cell = (TileEnergyCell) raw;
            cell.setStoredEnergy(getTier().isCreative() ? Long.MAX_VALUE : ItemEnergyCell.getStoredEnergy(stack));
        }
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        Item item = Item.getItemFromBlock(this);
        if (item == null || item == Items.AIR) return;
        ItemStack stack = new ItemStack(item);
        TileEntity raw = world.getTileEntity(pos);
        if (raw instanceof TileEnergyCell) {
            ItemEnergyCell.setStoredEnergy(stack, ((TileEnergyCell) raw).getStoredEnergy());
        }
        drops.add(stack);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state,
                             TileEntity tile, ItemStack tool) {
        if (!(tile instanceof TileEnergyCell)) {
            super.harvestBlock(world, player, pos, state, tile, tool);
            return;
        }
        Item item = Item.getItemFromBlock(this);
        if (item != null && item != Items.AIR) {
            ItemStack drop = new ItemStack(item);
            ItemEnergyCell.setStoredEnergy(drop, ((TileEnergyCell) tile).getStoredEnergy());
            spawnAsEntity(world, pos, drop);
        }
        player.addStat(StatList.getBlockStats(this));
        player.addExhaustion(0.005F);
    }
}
