package com.sosea1.powah.content.reactor;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import com.sosea1.powah.common.tier.PowahTier;

/** Passive reactor-shell tile forwarding capabilities to its loaded core without chunk loading. */
public final class TileReactorPart extends TileEntity implements ITickable {
    private static final String NBT_CORE = "PowahReactorCore";
    private static final String NBT_EXTRACTOR = "PowahReactorExtractor";
    private static final String NBT_TIER = "PowahReactorTier";

    private BlockPos corePos = BlockPos.ORIGIN;
    private boolean extractor;
    private PowahTier tier = PowahTier.STARTER;

    public TileReactorPart() {
    }

    public TileReactorPart(PowahTier tier) {
        this.tier = tier == null ? PowahTier.STARTER : tier;
    }

    public void bind(BlockPos core, boolean extractor) {
        if (core == null) {
            throw new NullPointerException("core");
        }
        this.corePos = core.toImmutable();
        this.extractor = extractor;
        markDirty();
        if (world != null && !world.isRemote) {
            net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    public BlockPos getCorePos() { return corePos; }
    public boolean isExtractor() { return extractor; }
    public PowahTier getTier() { return tier; }
    public boolean isReactorBuilt() {
        TileReactor core = getCore();
        return core != null && core.isBuilt();
    }

    @Override
    public void update() {
        if (world == null || world.isRemote || world.getTotalWorldTime() % 40L != 0L
                || !world.isBlockLoaded(corePos)) {
            return;
        }
        if (!(world.getTileEntity(corePos) instanceof TileReactor)) {
            // Demolition cannot force-load distant shell chunks. Remove any leftover
            // shell piece when its chunk is next loaded and the core is absent.
            world.setBlockToAir(pos);
        }
    }

    public TileReactor getCore() {
        if (world == null || corePos == null || !world.isBlockLoaded(corePos)) {
            return null;
        }
        TileEntity raw = world.getTileEntity(corePos);
        return raw instanceof TileReactor ? (TileReactor) raw : null;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        TileReactor core = getCore();
        if (core != null && core.isBuilt()) {
            if (capability == CapabilityEnergy.ENERGY) {
                return extractor && core.hasCapability(capability, facing);
            }
            if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                    || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
                return core.hasCapability(capability, facing);
            }
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        TileReactor core = getCore();
        if (core != null && core.isBuilt()) {
            if (capability == CapabilityEnergy.ENERGY) {
                return extractor ? core.getCapability(capability, facing) : null;
            }
            if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                    || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
                return core.getCapability(capability, facing);
            }
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setLong(NBT_CORE, corePos.toLong());
        compound.setBoolean(NBT_EXTRACTOR, extractor);
        compound.setByte(NBT_TIER, (byte) tier.ordinal());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        corePos = BlockPos.fromLong(compound.getLong(NBT_CORE));
        extractor = compound.getBoolean(NBT_EXTRACTOR);
        int index = compound.getByte(NBT_TIER) & 0xFF;
        PowahTier[] values = PowahTier.values();
        tier = values[Math.min(index, values.length - 1)];
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
    }

    @Override
    public boolean shouldRefresh(net.minecraft.world.World world, BlockPos pos, net.minecraft.block.state.IBlockState oldState, net.minecraft.block.state.IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }
}
