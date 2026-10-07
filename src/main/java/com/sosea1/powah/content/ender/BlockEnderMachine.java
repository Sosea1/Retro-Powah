package com.sosea1.powah.content.ender;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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
    protected BlockEnderMachine(PowahTier tier) { super(tier, 2.0F, 20.0F); }

    @Override
    protected boolean keepsPortableStateOnBreak() { return true; }

    @Override
    protected void writePortableState(ItemStack stack, TileEntity tile) {
        if (!(tile instanceof AbstractEnderTile)) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        // FE belongs to the owner's shared network. Inventory already drops in breakBlock.
        ((AbstractEnderTile) tile).writePortableData(tag);
    }

    @Override
    protected void readPortableState(ItemStack stack, TileEntity tile) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tile instanceof AbstractEnderTile) {
            ((AbstractEnderTile) tile).readPortableData(tag);
        }
    }

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
