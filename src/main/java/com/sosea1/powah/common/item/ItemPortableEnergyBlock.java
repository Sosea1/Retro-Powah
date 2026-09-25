package com.sosea1.powah.common.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.block.BlockPowahMachine;
import com.sosea1.powah.common.config.EnergyConfigSnapshot;
import com.sosea1.powah.common.energy.EnergyIntMath;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.content.discharger.BlockEnergyDischarger;
import com.sosea1.powah.content.energizing.BlockEnergizingRod;
import com.sosea1.powah.content.furnator.BlockFurnator;
import com.sosea1.powah.content.hopper.BlockEnergyHopper;
import com.sosea1.powah.content.magmator.BlockMagmator;
import com.sosea1.powah.content.solar.BlockSolarPanel;
import com.sosea1.powah.content.thermo.BlockThermoGenerator;
import com.sosea1.powah.content.transmitter.BlockPlayerTransmitter;

/**
 * Forge-1.12 item energy bridge for Powah machines that retain their FE buffer when broken.
 *
 * <p>The ItemStack NBT remains long-backed. Forge Energy is int-shaped, so each individual
 * receive/extract call is bounded by the Forge request while the stored amount and capacity
 * keep their full long precision. Transfer direction follows modern Powah's EnergyBlockItem:
 * generators and the Discharger are extract-only, while Rod/Hopper/Player Transmitter items
 * expose both directions.</p>
 */
public final class ItemPortableEnergyBlock extends ItemBlock {
    private final PowahTier tier;
    private final Profile profile;
    private final boolean canReceive;
    private final boolean canExtract;

    public ItemPortableEnergyBlock(Block block) {
        super(block);
        this.tier = tierOf(block);
        this.profile = requireProfile(block);
        this.canExtract = profile.canExtract;
        this.canReceive = profile.canReceive;
        setMaxStackSize(1);
    }

    public static boolean supports(Block block) {
        return profileOf(block) != null;
    }

    public PowahTier getTier() {
        return tier;
    }

    public static long getStoredEnergy(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? 0L : Math.max(0L, tag.getLong(BlockPowahMachine.NBT_PORTABLE_ENERGY));
    }

    public static void setStoredEnergy(ItemStack stack, long energy) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setLong(BlockPowahMachine.NBT_PORTABLE_ENERGY, Math.max(0L, energy));
    }

    private long capacityLong() {
        return Math.max(0L, profile.current().capacity().get(tier));
    }

    private long transferLong() {
        return Math.max(0L, profile.current().transfer().get(tier));
    }

    private long currentStored(ItemStack stack) {
        return Math.min(getStoredEnergy(stack), capacityLong());
    }

    @Override
    public ICapabilityProvider initCapabilities(final ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new ICapabilityProvider() {
            private final IEnergyStorage storage = new IEnergyStorage() {
                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    if (!canReceive || maxReceive <= 0) return 0;
                    long capacity = capacityLong();
                    long stored = currentStored(stack);
                    long accepted = Math.min((long) maxReceive, Math.min(transferLong(), capacity - stored));
                    if (!simulate && accepted > 0L) setStoredEnergy(stack, stored + accepted);
                    return EnergyIntMath.saturatedInt(accepted);
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    if (!canExtract || maxExtract <= 0) return 0;
                    long stored = currentStored(stack);
                    long extracted = Math.min((long) maxExtract, Math.min(transferLong(), stored));
                    if (!simulate && extracted > 0L) setStoredEnergy(stack, stored - extracted);
                    return EnergyIntMath.saturatedInt(extracted);
                }

                @Override
                public int getEnergyStored() {
                    return EnergyIntMath.saturatedInt(currentStored(stack));
                }

                @Override
                public int getMaxEnergyStored() {
                    return EnergyIntMath.saturatedInt(capacityLong());
                }

                @Override
                public boolean canExtract() {
                    return canExtract;
                }

                @Override
                public boolean canReceive() {
                    return canReceive;
                }
            };

            @Override
            public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
                return capability == CapabilityEnergy.ENERGY;
            }

            @Override
            public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
                return capability == CapabilityEnergy.ENERGY ? CapabilityEnergy.ENERGY.cast(storage) : null;
            }
        };
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        long capacity = capacityLong();
        tooltip.add(TextFormatting.GRAY + "Energy: " + currentStored(stack) + " / " + capacity + " FE");
        long transfer = transferLong();
        if (canReceive && canExtract) {
            tooltip.add(TextFormatting.DARK_GRAY + "Max I/O: " + transfer + " FE/t");
        } else if (canReceive) {
            tooltip.add(TextFormatting.DARK_GRAY + "Max receive: " + transfer + " FE/t");
        } else if (canExtract) {
            tooltip.add(TextFormatting.DARK_GRAY + "Max extract: " + transfer + " FE/t");
        }
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        long capacity = capacityLong();
        return capacity > 0L && currentStored(stack) < capacity;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        long capacity = capacityLong();
        return capacity <= 0L ? 1.0D : 1.0D - Math.min(1.0D, (double) currentStored(stack) / (double) capacity);
    }

    private static PowahTier tierOf(Block block) {
        if (block instanceof BlockPowahMachine) return ((BlockPowahMachine) block).getTier();
        if (block instanceof BlockPlayerTransmitter) return ((BlockPlayerTransmitter) block).getTier();
        throw new IllegalArgumentException("Unsupported portable Powah energy block: " + block.getClass().getName());
    }

    @Nullable
    private static Profile profileOf(Block block) {
        if (block instanceof BlockFurnator) return Profile.FURNATOR;
        if (block instanceof BlockMagmator) return Profile.MAGMATOR;
        if (block instanceof BlockSolarPanel) return Profile.SOLAR;
        if (block instanceof BlockThermoGenerator) return Profile.THERMO;
        if (block instanceof BlockEnergizingRod) return Profile.ENERGIZING_ROD;
        if (block instanceof BlockEnergyHopper) return Profile.ENERGY_HOPPER;
        if (block instanceof BlockEnergyDischarger) return Profile.DISCHARGER;
        if (block instanceof BlockPlayerTransmitter) return Profile.PLAYER_TRANSMITTER;
        return null;
    }

    private static Profile requireProfile(Block block) {
        Profile profile = profileOf(block);
        if (profile == null) {
            throw new IllegalArgumentException("Unsupported portable Powah energy block: " + block.getClass().getName());
        }
        return profile;
    }

    private enum Profile {
        FURNATOR(false, true) { @Override EnergyConfigSnapshot.Profile current() { return Powah.energyConfig().furnator(); } },
        MAGMATOR(false, true) { @Override EnergyConfigSnapshot.Profile current() { return Powah.energyConfig().magmator(); } },
        SOLAR(false, true) { @Override EnergyConfigSnapshot.Profile current() { return Powah.energyConfig().solarPanel(); } },
        THERMO(false, true) { @Override EnergyConfigSnapshot.Profile current() { return Powah.energyConfig().thermoGenerator(); } },
        ENERGIZING_ROD(true, true) { @Override EnergyConfigSnapshot.Profile current() { return Powah.energyConfig().energizingRod(); } },
        ENERGY_HOPPER(true, true) { @Override EnergyConfigSnapshot.Profile current() { return Powah.energyConfig().energyHopper(); } },
        DISCHARGER(false, true) { @Override EnergyConfigSnapshot.Profile current() { return Powah.energyConfig().discharger(); } },
        PLAYER_TRANSMITTER(true, true) { @Override EnergyConfigSnapshot.Profile current() { return Powah.energyConfig().playerTransmitter(); } };

        private final boolean canReceive;
        private final boolean canExtract;

        Profile(boolean canReceive, boolean canExtract) {
            this.canReceive = canReceive;
            this.canExtract = canExtract;
        }

        abstract EnergyConfigSnapshot.Profile current();
    }
}
