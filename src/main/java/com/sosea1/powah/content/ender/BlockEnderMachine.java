package com.sosea1.powah.content.ender;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.block.BlockPowahMachine;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.content.material.ItemWrench;

abstract class BlockEnderMachine extends BlockPowahMachine {
    protected BlockEnderMachine(PowahTier tier) { super(tier, 4.0F, 10.0F); }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof AbstractEnderTile && placer instanceof EntityPlayer && !(placer instanceof FakePlayer)) {
            EntityPlayer player = (EntityPlayer) placer;
            ((AbstractEnderTile) tile).claim(player.getUniqueID(), player.getName());
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (ItemWrench.isWrench(player.getHeldItem(hand))) {
            return true;
        }
        if (world.isRemote) return true;
        TileEntity raw = world.getTileEntity(pos);
        if (!(raw instanceof AbstractEnderTile)) return true;
        AbstractEnderTile tile = (AbstractEnderTile) raw;
        if (tile.getOwner() == null) tile.claim(player.getUniqueID(), player.getName());
        if (!tile.isOwner(player.getUniqueID())) return true;
        if (player.isSneaking()) {
            tile.cycleChannel();
            return true;
        }
        player.openGui(Powah.INSTANCE, Powah.GUI_MACHINE, world, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }
}
