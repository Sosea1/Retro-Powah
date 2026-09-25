package com.sosea1.powah.content.energycell;

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
import com.sosea1.powah.api.energy.endernetwork.IEnderExtender;
import com.sosea1.powah.common.energy.EnergyIntMath;
import com.sosea1.powah.common.tier.PowahTier;

/** Energy Cell block item. Storage stays long-backed even though Forge Energy is int-shaped. */
public final class ItemEnergyCell extends ItemBlock implements IEnderExtender {
    public static final String NBT_ENERGY = "PowahEnergy";
    private final PowahTier tier;

    public ItemEnergyCell(BlockEnergyCell block) {
        super(block);
        this.tier = block.getTier();
        setMaxStackSize(1);
    }

    public PowahTier getTier() { return tier; }

    public static long getStoredEnergy(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? 0L : Math.max(0L, tag.getLong(NBT_ENERGY));
    }

    public static void setStoredEnergy(ItemStack stack, long energy) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setLong(NBT_ENERGY, Math.max(0L, energy));
    }

    private long capacityLong() {
        return tier.isCreative() ? Long.MAX_VALUE : Powah.energyConfig().energyCell().capacity().get(tier);
    }

    private long transferLong() {
        return tier.isCreative() ? Integer.MAX_VALUE : Powah.energyConfig().energyCell().transfer().get(tier);
    }

    @Override
    public long getExtendedCapacity(ItemStack stack) {
        return tier.isCreative() ? 0L : capacityLong();
    }

    @Override
    public long getExtendedEnergy(ItemStack stack) {
        return tier.isCreative() ? 0L : Math.min(getStoredEnergy(stack), capacityLong());
    }

    @Override
    public ICapabilityProvider initCapabilities(final ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new ICapabilityProvider() {
            private final IEnergyStorage storage = new IEnergyStorage() {
                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    if (maxReceive <= 0 || tier.isCreative()) return 0;
                    long stored = Math.min(getStoredEnergy(stack), capacityLong());
                    long accepted = Math.min((long) maxReceive, Math.min(transferLong(), capacityLong() - stored));
                    if (!simulate && accepted > 0L) setStoredEnergy(stack, stored + accepted);
                    return EnergyIntMath.saturatedInt(accepted);
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    if (maxExtract <= 0) return 0;
                    if (tier.isCreative()) return Math.min(maxExtract, EnergyIntMath.saturatedInt(transferLong()));
                    long stored = Math.min(getStoredEnergy(stack), capacityLong());
                    long extracted = Math.min((long) maxExtract, Math.min(transferLong(), stored));
                    if (!simulate && extracted > 0L) setStoredEnergy(stack, stored - extracted);
                    return EnergyIntMath.saturatedInt(extracted);
                }

                @Override public int getEnergyStored() { return tier.isCreative() ? Integer.MAX_VALUE : EnergyIntMath.saturatedInt(Math.min(getStoredEnergy(stack), capacityLong())); }
                @Override public int getMaxEnergyStored() { return EnergyIntMath.saturatedInt(capacityLong()); }
                @Override public boolean canExtract() { return true; }
                @Override public boolean canReceive() { return !tier.isCreative(); }
            };

            @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) { return capability == CapabilityEnergy.ENERGY; }
            @Override public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
                return capability == CapabilityEnergy.ENERGY ? CapabilityEnergy.ENERGY.cast(storage) : null;
            }
        };
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        if (tier.isCreative()) {
            tooltip.add(TextFormatting.LIGHT_PURPLE + "Energy: Infinite");
        } else {
            tooltip.add(TextFormatting.GRAY + "Energy: " + getStoredEnergy(stack) + " / " + capacityLong() + " FE");
        }
    }

    @Override public boolean showDurabilityBar(ItemStack stack) { return !tier.isCreative() && getStoredEnergy(stack) < capacityLong(); }
    @Override public double getDurabilityForDisplay(ItemStack stack) {
        long capacity = capacityLong();
        return capacity <= 0L ? 1.0D : 1.0D - Math.min(1.0D, (double) getStoredEnergy(stack) / (double) capacity);
    }
}
