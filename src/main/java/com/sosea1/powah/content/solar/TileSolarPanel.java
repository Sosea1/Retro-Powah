package com.sosea1.powah.content.solar;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.block.entity.AbstractChargeableGeneratorTile;
import com.sosea1.powah.common.energy.EnergyPortMode;
import com.sosea1.powah.common.tier.PowahTier;

/** Passive generator. Sky visibility is cached and refreshed every 40 ticks. */
public final class TileSolarPanel extends AbstractChargeableGeneratorTile implements ITickable {
    private static final String NBT_VISIBLE = "PowahCanSeeSky";
    private static final String NBT_LENS = "PowahLensOfEnder";
    private boolean canSeeSky;
    private boolean lensOfEnder;

    public TileSolarPanel() {
        this(PowahTier.STARTER);
    }

    public TileSolarPanel(PowahTier tier) {
        super(tier, capacity(tier), transfer(tier), EnergyPortMode.NONE, EnergyPortMode.NONE);
        for (EnumFacing side : EnumFacing.values()) {
            setSideMode(side, side == EnumFacing.DOWN ? EnergyPortMode.OUTPUT : EnergyPortMode.NONE);
        }
    }

    private static long capacity(PowahTier tier) {
        return Powah.energyConfig().solarPanel().capacity().get(tier);
    }

    private static long transfer(PowahTier tier) {
        return Powah.energyConfig().solarPanel().transfer().get(tier);
    }

    private static long generation(PowahTier tier) {
        return Powah.energyConfig().solarPanel().generation().get(tier);
    }

    @Override
    protected long getCapacityForTier(PowahTier tier) {
        return capacity(tier);
    }

    @Override
    protected long getMaxReceiveForTier(PowahTier tier) {
        return 0L;
    }

    @Override
    protected long getMaxExtractForTier(PowahTier tier) {
        return transfer(tier);
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        // Discharge first, then use the newly freed capacity for solar production.
        chargeStoredItem(transfer(getTier()));
        pushEnergyToAdjacent(transfer(getTier()));

        if (isOperationAllowed()) {
            if (!lensOfEnder && world.getTotalWorldTime() % 40L == 0L) {
                boolean visible = world.canBlockSeeSky(pos.up());
                if (visible != canSeeSky) {
                    canSeeSky = visible;
                    markDirty();
                }
            }

            boolean daytime = world.provider.hasSkyLight() && world.getSkylightSubtracted() < 4;
            if ((canSeeSky || lensOfEnder) && daytime && !getEnergyBuffer().isFull()) {
                long produced = getEnergyBuffer().generate(generation(getTier()), false);
                if (produced > 0L) {
                    onEnergyChanged();
                }
            }
        }
    }


    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return facing == EnumFacing.DOWN;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return facing == EnumFacing.DOWN ? CapabilityEnergy.ENERGY.cast(getEnergyAdapter(facing)) : null;
        }
        return super.getCapability(capability, facing);
    }

    public boolean canSeeSky() { return canSeeSky; }
    public boolean hasLensOfEnder() { return lensOfEnder; }
    public void setLensOfEnder(boolean installed) {
        if (lensOfEnder != installed) { lensOfEnder = installed; markDirty(); }
    }

    @Override
    public int getGuiFlags() {
        return (canSeeSky ? 1 : 0) | (lensOfEnder ? 2 : 0);
    }

    @Override
    protected void writeGeneratorData(NBTTagCompound compound) {
        compound.setBoolean(NBT_VISIBLE, canSeeSky);
        compound.setBoolean(NBT_LENS, lensOfEnder);
    }

    @Override
    protected void readGeneratorData(NBTTagCompound compound) {
        canSeeSky = compound.getBoolean(NBT_VISIBLE);
        lensOfEnder = compound.getBoolean(NBT_LENS);
    }
}
