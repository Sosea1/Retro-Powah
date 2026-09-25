package com.sosea1.powah.content.energizing;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.block.entity.AbstractEnergyTile;
import com.sosea1.powah.common.energy.EnergyPortMode;
import com.sosea1.powah.common.tier.PowahTier;

/** Tiered FE buffer that periodically beams energy into one linked Energizing Orb. */
public final class TileEnergizingRod extends AbstractEnergyTile implements ITickable {
    public static final int DEFAULT_RANGE = 4;
    public static int configuredRange() { return com.sosea1.powah.common.config.PowahConfig.energizingRange(); }
    private static final String NBT_HAS_ORB = "PowahHasOrb";
    private static final String NBT_ORB_X = "PowahOrbX";
    private static final String NBT_ORB_Y = "PowahOrbY";
    private static final String NBT_ORB_Z = "PowahOrbZ";
    private static final String NBT_COOLDOWN = "PowahEnergizingCooldown";

    private BlockPos orbPos;
    private int transferCooldown;
    private int relinkCooldown;
    private long lastTransfer;
    private long lastSyncedEnergy = -1L;
    private int clientSyncCooldown = 0;

    public TileEnergizingRod() {
        this(PowahTier.STARTER);
    }

    public TileEnergizingRod(PowahTier tier) {
        super(tier, capacity(tier), transfer(tier), 0L, EnergyPortMode.INPUT, EnergyPortMode.NONE);
    }

    private static long capacity(PowahTier tier) {
        return Powah.energyConfig().energizingRod().capacity().get(tier);
    }

    private static long transfer(PowahTier tier) {
        return Powah.energyConfig().energizingRod().transfer().get(tier);
    }

    @Override
    protected long getCapacityForTier(PowahTier tier) {
        return capacity(tier);
    }

    @Override
    protected long getMaxReceiveForTier(PowahTier tier) {
        return transfer(tier);
    }

    @Override
    protected long getMaxExtractForTier(PowahTier tier) {
        return 0L;
    }

    private EnumFacing inputSide() {
        if (world == null) {
            return EnumFacing.DOWN;
        }
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BlockEnergizingRod && state.getPropertyKeys().contains(BlockEnergizingRod.FACING)) {
            return state.getValue(BlockEnergizingRod.FACING);
        }
        return EnumFacing.DOWN;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return facing != null && facing == inputSide();
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return facing != null && facing == inputSide()
                    ? CapabilityEnergy.ENERGY.cast(getEnergyAdapter(facing)) : null;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean isSideModeSupported(EnumFacing side, EnergyPortMode mode) {
        return mode == EnergyPortMode.NONE || mode == EnergyPortMode.INPUT;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }

        try {
            if (!isOperationAllowed()) {
                transferCooldown = 0;
                lastTransfer = 0L;
                return;
            }

            if (orbPos != null && !world.isBlockLoaded(orbPos)) {
                // Preserve the persistent link across a neighboring chunk unload; never force-load it.
                transferCooldown = 0;
                lastTransfer = 0L;
                return;
            }

            TileEnergizingOrb orb = getOrb();
            if (orb == null) {
                if (orbPos != null) {
                    setOrbPos(null);
                }
                if (++relinkCooldown >= 200) {
                    relinkCooldown = 0;
                    tryLinkNearestOrb(configuredRange());
                }
                transferCooldown = 0;
                lastTransfer = 0L;
                return;
            }
            relinkCooldown = 0;

            if (!orb.containsRecipe() || getEnergyBuffer().isEmpty()) {
                if (transferCooldown > 0) {
                    transferCooldown--;
                }
                lastTransfer = 0L;
                return;
            }

            if (transferCooldown < 20) {
                transferCooldown++;
                if (transferCooldown < 20) {
                    return;
                }
            }

            long offer = Math.min(getEnergyBuffer().energy(), transfer(getTier()));
            long accepted = orb.fillEnergy(offer);
            if (accepted > 0L) {
                getEnergyBuffer().consume(accepted, false);
                onEnergyChanged();
            }
            lastTransfer = accepted;
        } finally {
            long currentEnergy = getEnergyBuffer().energy();
            if (currentEnergy != lastSyncedEnergy) {
                if (++clientSyncCooldown >= 5 || currentEnergy == 0L || currentEnergy == getEnergyBuffer().capacity()) {
                    clientSyncCooldown = 0;
                    lastSyncedEnergy = currentEnergy;
                    syncClientState();
                }
            }
        }
    }

    public TileEnergizingOrb getOrb() {
        if (world == null || orbPos == null || !isInRange(orbPos) || !world.isBlockLoaded(orbPos)) {
            return null;
        }
        TileEntity raw = world.getTileEntity(orbPos);
        return raw instanceof TileEnergizingOrb ? (TileEnergizingOrb) raw : null;
    }

    private boolean isInRange(BlockPos target) {
        return Math.abs(target.getX() - pos.getX()) <= configuredRange()
                && Math.abs(target.getY() - pos.getY()) <= configuredRange()
                && Math.abs(target.getZ() - pos.getZ()) <= configuredRange();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if (orbPos == null) {
            return super.getRenderBoundingBox();
        }
        double minX = Math.min(pos.getX(), orbPos.getX()) - 0.25D;
        double minY = Math.min(pos.getY(), orbPos.getY()) - 0.25D;
        double minZ = Math.min(pos.getZ(), orbPos.getZ()) - 0.25D;
        double maxX = Math.max(pos.getX() + 1.0D, orbPos.getX() + 1.0D) + 0.25D;
        double maxY = Math.max(pos.getY() + 1.0D, orbPos.getY() + 1.0D) + 0.25D;
        double maxZ = Math.max(pos.getZ() + 1.0D, orbPos.getZ() + 1.0D) + 0.25D;
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public BlockPos getOrbPos() {
        return orbPos;
    }

    public void setOrbPos(BlockPos orbPos) {
        if (orbPos == null ? this.orbPos != null : !orbPos.equals(this.orbPos)) {
            this.orbPos = orbPos;
            relinkCooldown = 0;
            markDirty();
            syncClientState();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (orbPos == null) {
            relinkCooldown = 0;
        }
    }

    public boolean tryLinkNearestOrb(int range) {
        return tryLinkNearestOrb(range, null);
    }

    private boolean tryLinkNearestOrb(int range, BlockPos excludedOrb) {
        if (world == null) {
            return false;
        }
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos candidate = pos.add(dx, dy, dz);
                    if ((excludedOrb != null && excludedOrb.equals(candidate)) || !world.isBlockLoaded(candidate)) {
                        continue;
                    }
                    TileEntity raw = world.getTileEntity(candidate);
                    if (raw instanceof TileEnergizingOrb) {
                        double distance = pos.distanceSq(candidate);
                        if (distance < nearestDistance) {
                            nearest = candidate;
                            nearestDistance = distance;
                        }
                    }
                }
            }
        }
        setOrbPos(nearest);
        return nearest != null;
    }

    public static void linkNearbyUnlinkedRods(World world, BlockPos orbPos, int range) {
        if (world == null || orbPos == null) return;
        forEachLoadedRod(world, orbPos, range, new RodConsumer() {
            @Override
            public void accept(TileEnergizingRod rod) {
                if (rod.getOrbPos() == null) {
                    rod.setOrbPos(orbPos);
                }
            }
        });
    }

    public static void unlinkRodsFromOrb(World world, final BlockPos orbPos, final int range) {
        if (world == null || orbPos == null) return;
        forEachLoadedRod(world, orbPos, range, new RodConsumer() {
            @Override
            public void accept(TileEnergizingRod rod) {
                if (orbPos.equals(rod.getOrbPos())) {
                    // The removed Orb TileEntity still exists while Block#breakBlock runs in 1.12,
                    // so explicitly exclude it and immediately fall back to another loaded Orb.
                    // If no replacement exists, setOrbPos(null) keeps the normal bounded retry path.
                    rod.setOrbPos(null);
                    rod.tryLinkNearestOrb(range, orbPos);
                }
            }
        });
    }

    private static void forEachLoadedRod(World world, BlockPos center, int range, RodConsumer consumer) {
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos candidate = center.add(dx, dy, dz);
                    if (!world.isBlockLoaded(candidate)) continue;
                    TileEntity raw = world.getTileEntity(candidate);
                    if (raw instanceof TileEnergizingRod) {
                        consumer.accept((TileEnergizingRod) raw);
                    }
                }
            }
        }
    }

    private interface RodConsumer {
        void accept(TileEnergizingRod rod);
    }

    @Override
    public long getGuiAuxValue() {
        return lastTransfer;
    }

    @Override
    public int getGuiFlags() {
        return (getOrb() != null ? 1 : 0) | (lastTransfer > 0L ? 2 : 0);
    }

    @Override
    protected void writePowahData(NBTTagCompound compound) {
        compound.setBoolean(NBT_HAS_ORB, orbPos != null);
        if (orbPos != null) {
            compound.setInteger(NBT_ORB_X, orbPos.getX());
            compound.setInteger(NBT_ORB_Y, orbPos.getY());
            compound.setInteger(NBT_ORB_Z, orbPos.getZ());
        }
        compound.setInteger(NBT_COOLDOWN, transferCooldown);
    }

    @Override
    protected void readPowahData(NBTTagCompound compound) {
        orbPos = compound.getBoolean(NBT_HAS_ORB)
                ? new BlockPos(compound.getInteger(NBT_ORB_X), compound.getInteger(NBT_ORB_Y), compound.getInteger(NBT_ORB_Z))
                : null;
        transferCooldown = Math.max(0, Math.min(20, compound.getInteger(NBT_COOLDOWN)));
    }
}
