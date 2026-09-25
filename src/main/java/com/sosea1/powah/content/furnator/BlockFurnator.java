package com.sosea1.powah.content.furnator;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.sosea1.powah.common.block.BlockHorizontalPowahMachine;
import com.sosea1.powah.common.tier.PowahTier;

public final class BlockFurnator extends BlockHorizontalPowahMachine {
    public BlockFurnator(PowahTier tier) {
        super(tier, 3.5F, 8.0F);
    }

    @Override
    protected TileEntity createPowahTile(PowahTier tier) {
        return new TileFurnator(tier);
    }

    @Override
    public boolean keepsEnergyOnBreak() {
        return true;
    }

    /** Modern Powah shows a small flame at the machine face while fuel is actively generating. */
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        TileEntity raw = world.getTileEntity(pos);
        if (!(raw instanceof TileFurnator) || !((TileFurnator) raw).isBurning()) {
            return;
        }

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        if (random.nextDouble() < 0.1D) {
            world.playSound(x, y, z, SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.BLOCKS,
                    1.0F, 1.0F, false);
        }

        EnumFacing direction = state.getValue(FACING);
        double side = random.nextDouble() * 0.6D - 0.3D;
        double frontX = direction.getAxis() == EnumFacing.Axis.X ? direction.getXOffset() * 0.52D : side;
        double frontY = random.nextDouble() * 6.0D / 16.0D;
        double frontZ = direction.getAxis() == EnumFacing.Axis.Z ? direction.getZOffset() * 0.52D : side;
        world.spawnParticle(EnumParticleTypes.FLAME, x + frontX, y + frontY, z + frontZ, 0.0D, 0.0D, 0.0D);
    }
}
