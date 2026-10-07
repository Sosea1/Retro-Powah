package com.sosea1.powah.common.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.content.cable.BlockCable;
import com.sosea1.powah.content.ender.BlockEnderGate;
import com.sosea1.powah.content.energizing.BlockEnergizingRod;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;

final class EnergyAttachmentSupportTest {
    static { Bootstrap.register(); }
    private static final BlockPos ENDPOINT = new BlockPos(0, 64, 0);
    private final BlockEnderGate gate = new BlockEnderGate(PowahTier.STARTER);
    private final BlockEnergizingRod rod = new BlockEnergizingRod(PowahTier.STARTER);

    @Test
    void solidBlockWithoutEnergyCannotSupportEitherEndpoint() {
        SupportWorld world = new SupportWorld(new Block(Material.ROCK), null, true);
        assertFalse(gate.canSurviveAt(world, ENDPOINT, gate.getDefaultState().withProperty(BlockEnderGate.FACING, EnumFacing.EAST)));
        assertFalse(rod.canSurviveAt(world, ENDPOINT, rod.getDefaultState().withProperty(BlockEnergizingRod.FACING, EnumFacing.EAST)));
    }

    @Test
    void invalidSupportIsRejectedBeforeItemPlacementConsumesItsPortableState() {
        SupportWorld world = new SupportWorld(new Block(Material.ROCK), null, true);
        assertFalse(gate.canPlaceBlockOnSide(world, ENDPOINT, EnumFacing.EAST));
        assertFalse(rod.canPlaceBlockOnSide(world, ENDPOINT, EnumFacing.EAST));
    }

    @Test
    void placementChecksTheClickedSupportFaceRatherThanTheDefaultFacing() {
        EnergySupportTile support = new EnergySupportTile(EnumFacing.EAST);
        SupportWorld world = new SupportWorld(new Block(Material.GLASS), support, true);
        assertTrue(gate.canPlaceBlockOnSide(world, ENDPOINT, EnumFacing.EAST));
        assertTrue(rod.canPlaceBlockOnSide(world, ENDPOINT, EnumFacing.EAST));
        assertFalse(gate.canPlaceBlockOnSide(world, ENDPOINT, EnumFacing.WEST));
        assertFalse(rod.canPlaceBlockOnSide(world, ENDPOINT, EnumFacing.WEST));
    }

    @Test
    void cableSupportsEitherEndpointWithoutAnEnabledEnergyFace() {
        SupportWorld world = new SupportWorld(new BlockCable(PowahTier.STARTER), null, true);
        assertTrue(gate.canSurviveAt(world, ENDPOINT, gate.getDefaultState()));
        assertTrue(rod.canSurviveAt(world, ENDPOINT, rod.getDefaultState()));
        assertEquals(0, world.tileQueries);
    }

    @Test
    void energyMustBeExposedOnTheFaceTouchingTheEndpoint() {
        EnergySupportTile support = new EnergySupportTile(EnumFacing.WEST);
        SupportWorld world = new SupportWorld(new Block(Material.GLASS), support, true);
        assertTrue(gate.canSurviveAt(world, ENDPOINT, gate.getDefaultState().withProperty(BlockEnderGate.FACING, EnumFacing.EAST)));
        assertTrue(rod.canSurviveAt(world, ENDPOINT, rod.getDefaultState().withProperty(BlockEnergizingRod.FACING, EnumFacing.EAST)));
        assertFalse(gate.canSurviveAt(world, ENDPOINT, gate.getDefaultState().withProperty(BlockEnderGate.FACING, EnumFacing.WEST)));
    }

    @Test
    void unloadedSupportIsDeferredWithoutQueryingOrLoadingItsBlock() {
        SupportWorld world = new SupportWorld(new Block(Material.ROCK), null, false);
        assertTrue(gate.canSurviveAt(world, ENDPOINT, gate.getDefaultState()));
        assertTrue(rod.canSurviveAt(world, ENDPOINT, rod.getDefaultState()));
        assertEquals(0, world.stateQueries);
        assertEquals(0, world.tileQueries);
    }

    private static final class SupportWorld extends World {
        private final IBlockState supportState;
        private final TileEntity supportTile;
        private final boolean loaded;
        private int stateQueries;
        private int tileQueries;

        SupportWorld(Block support, TileEntity tile, boolean loaded) {
            super(null, new WorldInfo(new NBTTagCompound()), new WorldProviderSurface(), new Profiler(), false);
            this.supportState = support.getDefaultState();
            this.supportTile = tile;
            this.loaded = loaded;
        }
        @Override protected IChunkProvider createChunkProvider() { throw new AssertionError("Must not load chunks"); }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return loaded; }
        @Override public boolean isBlockLoaded(BlockPos pos) { return loaded; }
        @Override public IBlockState getBlockState(BlockPos pos) {
            if (ENDPOINT.equals(pos)) return Blocks.AIR.getDefaultState();
            if (!loaded) throw new AssertionError("Must not query an unloaded block");
            stateQueries++;
            return supportState;
        }
        @Override public TileEntity getTileEntity(BlockPos pos) {
            if (!loaded) throw new AssertionError("Must not query an unloaded tile");
            tileQueries++;
            return supportTile;
        }
    }

    private static final class EnergySupportTile extends TileEntity {
        private final EnumFacing energyFace;
        private final EnergyStorage energy = new EnergyStorage(1000);
        EnergySupportTile(EnumFacing energyFace) { this.energyFace = energyFace; }
        @Override public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY && facing == energyFace;
        }
        @Override @SuppressWarnings("unchecked")
        public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
            return hasCapability(capability, facing) ? (T) energy : null;
        }
    }
}
