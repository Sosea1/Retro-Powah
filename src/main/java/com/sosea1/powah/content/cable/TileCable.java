package com.sosea1.powah.content.cable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.energy.EnergyIntMath;
import com.sosea1.powah.common.energy.EnergyPortMode;
import com.sosea1.powah.common.energy.RedstoneControlMode;
import com.sosea1.powah.common.tier.PowahTier;

/**
 * Stateless cable. FE is routed directly between cached network endpoints.
 *
 * <p>The receive path mirrors modern Powah. The extract path is deliberately symmetric for
 * Forge 1.12 interoperability: many legacy machines pull FE instead of asking their source to
 * push, so a downstream consumer may pull through the passive cable network without turning
 * every cable into a ticking TileEntity.</p>
 */
public final class TileCable extends TileEntity {
    private static final String NBT_TIER = "PowahTier";
    private static final String NBT_SIDES = "PowahSides";
    private static final String NBT_REDSTONE = "PowahRedstone";

    private final IEnergyStorage[] adapters = new IEnergyStorage[7];
    private final EnergyPortMode[] sideModes = new EnergyPortMode[6];
    private PowahTier tier;
    private RedstoneControlMode redstoneControlMode = RedstoneControlMode.IGNORED;
    private CableNetwork network;
    private int roundRobinStart;
    private boolean registered;

    public TileCable() {
        this(PowahTier.STARTER);
    }

    public TileCable(PowahTier tier) {
        if (tier == null || !tier.isNormal()) {
            throw new IllegalArgumentException("Cable requires a normal Powah tier");
        }
        this.tier = tier;
        for (EnumFacing side : EnumFacing.values()) {
            sideModes[side.ordinal()] = EnergyPortMode.BOTH;
        }
        createAdapters();
    }

    private void createAdapters() {
        for (final EnumFacing side : EnumFacing.values()) {
            adapters[side.ordinal()] = new CableEnergyAdapter(side);
        }
        adapters[6] = new CableEnergyAdapter(null);
    }

    public PowahTier getTier() {
        return tier;
    }

    public boolean canConnectTo(TileCable other) {
        return other != null && tier == other.tier;
    }

    public EnergyPortMode getSideMode(EnumFacing side) {
        return side == null ? EnergyPortMode.BOTH : sideModes[side.ordinal()];
    }

    public void setSideMode(EnumFacing side, EnergyPortMode mode) {
        if (side == null || mode == null) {
            throw new NullPointerException(side == null ? "side" : "mode");
        }
        if (sideModes[side.ordinal()] == mode) {
            return;
        }
        sideModes[side.ordinal()] = mode;
        markDirty();
        if (world != null) {
            if (!world.isRemote) {
                net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
                world.notifyBlockUpdate(pos, state, state, 3);
            }
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    public EnergyPortMode cycleSideMode(EnumFacing side) {
        EnergyPortMode current = getSideMode(side);
        EnergyPortMode next = current.next();
        setSideMode(side, next);
        return next;
    }

    public RedstoneControlMode getRedstoneControlMode() {
        return redstoneControlMode;
    }

    public void setRedstoneControlMode(RedstoneControlMode mode) {
        if (mode == null) throw new NullPointerException("mode");
        if (redstoneControlMode == mode) return;
        redstoneControlMode = mode;
        markDirty();
        if (world != null) {
            if (!world.isRemote) {
                net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
                world.notifyBlockUpdate(pos, state, state, 3);
            }
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    public boolean isOperationAllowed() {
        if (redstoneControlMode == RedstoneControlMode.IGNORED || world == null) return true;
        boolean powered = world.isBlockPowered(pos);
        return redstoneControlMode == RedstoneControlMode.HIGH ? powered : !powered;
    }

    public boolean hasExternalEnergySide(EnumFacing side) {
        if (world == null || side == null) {
            return false;
        }
        BlockPos targetPos = pos.offset(side);
        if (!world.isBlockLoaded(targetPos)) {
            return false;
        }
        TileEntity target = world.getTileEntity(targetPos);
        if (target == null || target instanceof TileCable) {
            return false;
        }
        EnumFacing targetSide = side.getOpposite();
        return target.hasCapability(CapabilityEnergy.ENERGY, targetSide)
                && target.getCapability(CapabilityEnergy.ENERGY, targetSide) != null;
    }

    CableNetwork getNetworkIfPresent() {
        return network;
    }

    void setNetwork(CableNetwork network) {
        this.network = network;
    }

    public void onNeighborChanged() {
        if (world != null && !world.isRemote) {
            CableNetworkManager.neighborChanged(this);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        registerCable();
    }

    @Override
    public void invalidate() {
        unregisterCable();
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        unregisterCable();
        super.onChunkUnload();
    }

    private void registerCable() {
        if (!registered && world != null && !world.isRemote) {
            registered = true;
            CableNetworkManager.add(this);
        }
    }

    private void unregisterCable() {
        if (registered) {
            registered = false;
            CableNetworkManager.remove(this);
        }
    }

    private long transferRate() {
        return Powah.energyConfig().cableTransfer().get(tier);
    }

    private int routeEnergyIn(int maxReceive, boolean simulate, EnumFacing ingressSide) {
        if (maxReceive <= 0 || world == null || world.isRemote || !isOperationAllowed()) {
            return 0;
        }
        registerCable();

        CableNetwork net = CableNetworkManager.networkFor(this);
        if (net.insertionGuard || net.endpoints.isEmpty()) {
            return 0;
        }

        int budget = EnergyIntMath.saturatedInt(Math.min((long) maxReceive, transferRate()));
        if (budget <= 0) {
            return 0;
        }

        BlockPos sourcePos = ingressSide == null ? null : pos.offset(ingressSide);
        int received = 0;
        int endpointCount = net.endpoints.size();
        int start = endpointCount == 0 ? 0 : roundRobinStart % endpointCount;

        net.insertionGuard = true;
        try {
            for (int i = 0; i < endpointCount && received < budget; i++) {
                CableEndpoint endpoint = net.endpoints.get((start + i) % endpointCount);
                if (sourcePos != null && sourcePos.equals(endpoint.pos)) {
                    continue;
                }
                if (!world.isBlockLoaded(endpoint.cablePos)) {
                    continue;
                }
                TileEntity sourceCableTile = world.getTileEntity(endpoint.cablePos);
                if (!(sourceCableTile instanceof TileCable)
                        || !((TileCable) sourceCableTile).isOperationAllowed()
                        || !((TileCable) sourceCableTile).getSideMode(endpoint.cableSide).canExtract()) {
                    continue;
                }
                if (!world.isBlockLoaded(endpoint.pos)) {
                    continue;
                }
                TileEntity targetTile = world.getTileEntity(endpoint.pos);
                if (targetTile == null || !targetTile.hasCapability(CapabilityEnergy.ENERGY, endpoint.side)) {
                    continue;
                }
                IEnergyStorage target = targetTile.getCapability(CapabilityEnergy.ENERGY, endpoint.side);
                if (target == null || !target.canReceive()) {
                    continue;
                }
                int accepted = target.receiveEnergy(budget - received, simulate);
                if (accepted > 0) {
                    received += accepted;
                }
            }
            if (!simulate && endpointCount > 1 && received > 0) {
                roundRobinStart = (roundRobinStart + 1) % endpointCount;
            }
        } finally {
            net.insertionGuard = false;
        }
        return received;
    }

    /**
     * Reverse/passive routing for legacy FE consumers that actively pull from their source.
     * This adds no ticker and never loads chunks; it only walks the same cached endpoint list
     * that the normal receive-driven route uses.
     */
    private int routeEnergyOut(int maxExtract, boolean simulate, EnumFacing egressSide) {
        if (maxExtract <= 0 || world == null || world.isRemote || !isOperationAllowed()) {
            return 0;
        }
        registerCable();

        CableNetwork net = CableNetworkManager.networkFor(this);
        if (net.insertionGuard || net.endpoints.isEmpty()) {
            return 0;
        }

        int budget = EnergyIntMath.saturatedInt(Math.min((long) maxExtract, transferRate()));
        if (budget <= 0) {
            return 0;
        }

        BlockPos consumerPos = egressSide == null ? null : pos.offset(egressSide);
        int extracted = 0;
        int endpointCount = net.endpoints.size();
        int start = endpointCount == 0 ? 0 : roundRobinStart % endpointCount;

        net.insertionGuard = true;
        try {
            for (int i = 0; i < endpointCount && extracted < budget; i++) {
                CableEndpoint endpoint = net.endpoints.get((start + i) % endpointCount);
                if (consumerPos != null && consumerPos.equals(endpoint.pos)) {
                    continue;
                }
                if (!world.isBlockLoaded(endpoint.cablePos)) {
                    continue;
                }
                TileEntity sourceCableTile = world.getTileEntity(endpoint.cablePos);
                if (!(sourceCableTile instanceof TileCable)
                        || !((TileCable) sourceCableTile).isOperationAllowed()
                        || !((TileCable) sourceCableTile).getSideMode(endpoint.cableSide).canReceive()) {
                    continue;
                }
                if (!world.isBlockLoaded(endpoint.pos)) {
                    continue;
                }
                TileEntity sourceTile = world.getTileEntity(endpoint.pos);
                if (sourceTile == null || !sourceTile.hasCapability(CapabilityEnergy.ENERGY, endpoint.side)) {
                    continue;
                }
                IEnergyStorage source = sourceTile.getCapability(CapabilityEnergy.ENERGY, endpoint.side);
                if (source == null || !source.canExtract()) {
                    continue;
                }
                int supplied = source.extractEnergy(budget - extracted, simulate);
                if (supplied > 0) {
                    extracted += supplied;
                }
            }
            if (!simulate && endpointCount > 1 && extracted > 0) {
                roundRobinStart = (roundRobinStart + 1) % endpointCount;
            }
        } finally {
            net.insertionGuard = false;
        }
        return extracted;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(adapters[facing == null ? 6 : facing.ordinal()]);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean shouldRefresh(net.minecraft.world.World world, BlockPos pos, net.minecraft.block.state.IBlockState oldState, net.minecraft.block.state.IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
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
        if (world != null) {
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setByte(NBT_TIER, (byte) tier.ordinal());
        compound.setInteger(NBT_SIDES, packSideModes());
        compound.setByte(NBT_REDSTONE, (byte) redstoneControlMode.ordinal());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        int ordinal = compound.getByte(NBT_TIER) & 0xFF;
        PowahTier[] tiers = PowahTier.values();
        if (ordinal < tiers.length && tiers[ordinal].isNormal()) {
            tier = tiers[ordinal];
        }
        if (compound.hasKey(NBT_SIDES)) {
            unpackSideModes(compound.getInteger(NBT_SIDES));
        }
        if (compound.hasKey(NBT_REDSTONE)) {
            int redstoneOrdinal = compound.getByte(NBT_REDSTONE) & 0xFF;
            RedstoneControlMode[] modes = RedstoneControlMode.values();
            redstoneControlMode = redstoneOrdinal < modes.length
                    ? modes[redstoneOrdinal] : RedstoneControlMode.IGNORED;
        }
    }

    private int packSideModes() {
        int packed = 0;
        for (EnumFacing side : EnumFacing.values()) {
            packed |= (sideModes[side.ordinal()].ordinal() & 0x3) << (side.ordinal() * 2);
        }
        return packed;
    }

    private void unpackSideModes(int packed) {
        EnergyPortMode[] modes = EnergyPortMode.values();
        for (EnumFacing side : EnumFacing.values()) {
            int value = (packed >>> (side.ordinal() * 2)) & 0x3;
            sideModes[side.ordinal()] = value < modes.length ? modes[value] : EnergyPortMode.BOTH;
        }
    }

    private final class CableEnergyAdapter implements IEnergyStorage {
        private final EnumFacing side;

        private CableEnergyAdapter(EnumFacing side) {
            this.side = side;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (side != null && !getSideMode(side).canReceive()) {
                return 0;
            }
            return routeEnergyIn(maxReceive, simulate, side);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (side != null && !getSideMode(side).canExtract()) {
                return 0;
            }
            return routeEnergyOut(maxExtract, simulate, side);
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return 0;
        }

        @Override
        public boolean canExtract() {
            return transferRate() > 0L && (side == null || getSideMode(side).canExtract());
        }

        @Override
        public boolean canReceive() {
            return transferRate() > 0L && (side == null || getSideMode(side).canReceive());
        }
    }
}
