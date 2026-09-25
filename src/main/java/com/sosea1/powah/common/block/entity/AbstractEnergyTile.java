package com.sosea1.powah.common.block.entity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.energy.EnergyIntMath;
import com.sosea1.powah.common.energy.EnergyPortMode;
import com.sosea1.powah.common.energy.EnergyPortModeProvider;
import com.sosea1.powah.common.energy.ForgeEnergyAdapter;
import com.sosea1.powah.common.energy.LongEnergyBuffer;
import com.sosea1.powah.common.energy.RedstoneControlMode;
import com.sosea1.powah.common.tier.PowahTier;

/**
 * Shared 1.12.2 energy TileEntity foundation.
 *
 * The internal storage remains long-backed while Forge Energy is exposed through cached,
 * side-aware int adapters. Side configuration is persisted as two bits per direction.
 */
public abstract class AbstractEnergyTile extends TileEntity {
    private static final String NBT_ENERGY = "PowahEnergy";
    private static final String NBT_TIER = "PowahTier";
    private static final String NBT_SIDES = "PowahSides";
    private static final String NBT_REDSTONE = "PowahRedstone";
    private static final EnumFacing[] FACINGS = EnumFacing.values();

    private final EnergyPortMode[] sideModes = new EnergyPortMode[6];
    private final ForgeEnergyAdapter[] sideAdapters = new ForgeEnergyAdapter[7];
    private final LongEnergyBuffer energy;
    private final EnergyPortMode unsidedMode;

    private PowahTier tier;
    private RedstoneControlMode redstoneControlMode = RedstoneControlMode.IGNORED;
    private int lastComparatorPower = -1;
    private long appliedConfigRevision = Long.MIN_VALUE;

    protected AbstractEnergyTile(PowahTier tier, long capacity, long maxReceive, long maxExtract,
                                 EnergyPortMode defaultSideMode, EnergyPortMode unsidedMode) {
        if (tier == null) {
            throw new NullPointerException("tier");
        }
        if (defaultSideMode == null) {
            throw new NullPointerException("defaultSideMode");
        }
        if (unsidedMode == null) {
            throw new NullPointerException("unsidedMode");
        }
        this.tier = tier;
        this.unsidedMode = unsidedMode;
        this.energy = new LongEnergyBuffer(capacity, maxReceive, maxExtract);

        for (int i = 0; i < sideModes.length; i++) {
            sideModes[i] = defaultSideMode;
        }
        createCapabilityAdapters();
    }

    private void createCapabilityAdapters() {
        final Runnable mutationListener = new Runnable() {
            @Override
            public void run() {
                onEnergyChanged();
            }
        };

        for (final EnumFacing facing : FACINGS) {
            sideAdapters[facing.ordinal()] = new ForgeEnergyAdapter(
                    energy,
                    new EnergyPortModeProvider() {
                        @Override
                        public EnergyPortMode get() {
                            return isEnergyPortActive(facing) ? getSideMode(facing) : EnergyPortMode.NONE;
                        }
                    },
                    mutationListener
            );
        }

        sideAdapters[6] = new ForgeEnergyAdapter(
                energy,
                new EnergyPortModeProvider() {
                    @Override
                    public EnergyPortMode get() {
                        return isEnergyPortActive(null) ? unsidedMode : EnergyPortMode.NONE;
                    }
                },
                mutationListener
        );
    }

    protected abstract long getCapacityForTier(PowahTier tier);

    protected abstract long getMaxReceiveForTier(PowahTier tier);

    protected abstract long getMaxExtractForTier(PowahTier tier);

    protected final void refreshEnergyLimits() {
        energy.setLimits(
                getCapacityForTier(tier),
                getMaxReceiveForTier(tier),
                getMaxExtractForTier(tier)
        );
        appliedConfigRevision = Powah.configRevision();
    }

    protected final void setTier(PowahTier tier) {
        if (tier == null) {
            throw new NullPointerException("tier");
        }
        this.tier = tier;
        refreshEnergyLimits();
        markDirty();
    }

    public final PowahTier getTier() {
        return tier;
    }

    public final LongEnergyBuffer getEnergyBuffer() {
        long revision = Powah.configRevision();
        if (revision != appliedConfigRevision) {
            refreshEnergyLimits();
        }
        return energy;
    }

    public final EnergyPortMode getSideMode(EnumFacing side) {
        if (side == null) {
            return unsidedMode;
        }
        return sideModes[side.ordinal()];
    }

    public final void setSideMode(EnumFacing side, EnergyPortMode mode) {
        if (side == null) {
            throw new NullPointerException("side");
        }
        if (mode == null) {
            throw new NullPointerException("mode");
        }
        if (sideModes[side.ordinal()] != mode) {
            sideModes[side.ordinal()] = mode;
            markDirty();
            notifyClientStateChanged();
            onSideConfigurationChanged(side);
        }
    }

    protected void onSideConfigurationChanged(EnumFacing side) {
    }

    /** Whether the wrench may alter FE mode on this machine. Some endpoints have fixed semantics. */
    public boolean canConfigureSide(EnumFacing side) {
        return true;
    }

    /**
     * Runtime gate for external FE ports. Most generators keep output active while redstone only
     * controls generation; storage/network blocks may override this to gate their FE capability.
     */
    protected boolean isEnergyPortActive(EnumFacing side) {
        return true;
    }

    /** Restricts wrench choices without changing internal buffer semantics. */
    public boolean isSideModeSupported(EnumFacing side, EnergyPortMode mode) {
        return true;
    }

    public final RedstoneControlMode getRedstoneControlMode() {
        return redstoneControlMode;
    }

    public final void setRedstoneControlMode(RedstoneControlMode mode) {
        if (mode == null) throw new NullPointerException("mode");
        if (redstoneControlMode != mode) {
            redstoneControlMode = mode;
            markDirty();
            notifyClientStateChanged();
        }
    }

    /** True when active machine behavior is allowed by the configured redstone mode. */
    public final boolean isOperationAllowed() {
        if (redstoneControlMode == RedstoneControlMode.IGNORED || world == null) return true;
        boolean powered = world.isBlockPowered(pos);
        return redstoneControlMode == RedstoneControlMode.HIGH ? powered : !powered;
    }

    protected void onEnergyChanged() {
        markDirty();
        updateComparatorOutput();
    }

    public final int getComparatorPower() {
        LongEnergyBuffer buffer = getEnergyBuffer();
        long capacity = buffer.capacity();
        if (capacity <= 0L) return 0;
        return Math.max(0, Math.min(15, (int) (((double) buffer.energy() / (double) capacity) * 15.0D)));
    }

    /** Notify comparators only when the quantized 0..15 output actually changes. */
    protected final void updateComparatorOutput() {
        int power = getComparatorPower();
        if (power == lastComparatorPower) return;
        lastComparatorPower = power;
        if (world != null && !world.isRemote) {
            world.updateComparatorOutputLevel(pos, getBlockType());
        }
    }

    /** Pushes lightweight TileEntity state to watching clients without a custom packet.
     * Call this only for externally visible state transitions, not every FE mutation. */
    protected final void syncClientState() {
        notifyClientStateChanged();
    }

    /** Pushes lightweight configuration changes to watching clients without a custom packet. */
    private void notifyClientStateChanged() {
        if (world != null && !world.isRemote) {
            net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    protected final IEnergyStorage getEnergyAdapter(EnumFacing facing) {
        return sideAdapters[facing == null ? 6 : facing.ordinal()];
    }

    /** Lightweight values consumed by the generic 1.12 container sync layer. */
    public long getGuiAuxValue() {
        return 0L;
    }

    public int getGuiFlags() {
        return 0;
    }

    public int getGuiFluidAmount() {
        return 0;
    }

    public int getGuiFluidCapacity() {
        return 0;
    }

    /**
     * Pushes energy to directly adjacent FE receivers without loading chunks.
     * The budget is a total budget for this call, not a per-side budget.
     */
    protected final long pushEnergyToAdjacent(long budget) {
        if (budget <= 0L || world == null || world.isRemote || energy.isEmpty()) {
            return 0L;
        }

        long remaining = Math.min(budget, Math.min(energy.energy(), energy.maxExtract()));
        if (remaining <= 0L) {
            return 0L;
        }

        long transferred = 0L;
        int start = (int) (world.getTotalWorldTime() % FACINGS.length);

        for (int i = 0; i < FACINGS.length && remaining > 0L; i++) {
            EnumFacing side = FACINGS[(start + i) % FACINGS.length];
            if (!isEnergyPortActive(side) || !getSideMode(side).canExtract()) {
                continue;
            }

            BlockPos targetPos = pos.offset(side);
            if (!world.isBlockLoaded(targetPos)) {
                continue;
            }

            TileEntity targetTile = world.getTileEntity(targetPos);
            if (targetTile == null || !targetTile.hasCapability(CapabilityEnergy.ENERGY, side.getOpposite())) {
                continue;
            }

            IEnergyStorage target = targetTile.getCapability(CapabilityEnergy.ENERGY, side.getOpposite());
            if (target == null || !target.canReceive()) {
                continue;
            }

            int offer = EnergyIntMath.saturatedInt(remaining);
            if (offer <= 0) {
                break;
            }

            int accepted = target.receiveEnergy(offer, false);
            if (accepted <= 0) {
                continue;
            }

            long extracted = energy.extract(accepted, false);
            if (extracted <= 0L) {
                break;
            }
            transferred += extracted;
            remaining -= extracted;
        }

        if (transferred > 0L) {
            onEnergyChanged();
        }
        return transferred;
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
            return CapabilityEnergy.ENERGY.cast(getEnergyAdapter(facing));
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
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
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setLong(NBT_ENERGY, energy.energy());
        compound.setByte(NBT_TIER, (byte) tier.ordinal());
        compound.setInteger(NBT_SIDES, packSideModes());
        compound.setByte(NBT_REDSTONE, (byte) redstoneControlMode.ordinal());
        writePowahData(compound);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        int tierOrdinal = compound.getByte(NBT_TIER) & 0xFF;
        PowahTier[] tiers = PowahTier.values();
        if (tierOrdinal < tiers.length) {
            tier = tiers[tierOrdinal];
        }
        refreshEnergyLimits();
        energy.setEnergy(compound.getLong(NBT_ENERGY));

        if (compound.hasKey(NBT_SIDES)) {
            unpackSideModes(compound.getInteger(NBT_SIDES));
        }
        if (compound.hasKey(NBT_REDSTONE)) {
            int ordinal = compound.getByte(NBT_REDSTONE) & 0xFF;
            RedstoneControlMode[] modes = RedstoneControlMode.values();
            redstoneControlMode = ordinal < modes.length ? modes[ordinal] : RedstoneControlMode.IGNORED;
        }
        readPowahData(compound);
    }

    protected void writePowahData(NBTTagCompound compound) {
    }

    protected void readPowahData(NBTTagCompound compound) {
    }

    private int packSideModes() {
        int packed = 0;
        for (EnumFacing side : FACINGS) {
            packed |= (sideModes[side.ordinal()].ordinal() & 0x3) << (side.ordinal() * 2);
        }
        return packed;
    }

    private void unpackSideModes(int packed) {
        EnergyPortMode[] modes = EnergyPortMode.values();
        for (EnumFacing side : FACINGS) {
            int value = (packed >>> (side.ordinal() * 2)) & 0x3;
            sideModes[side.ordinal()] = value < modes.length ? modes[value] : EnergyPortMode.NONE;
        }
    }
}
