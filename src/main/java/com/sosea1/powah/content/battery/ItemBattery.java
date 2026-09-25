package com.sosea1.powah.content.battery;

import com.sosea1.powah.registry.PowahCreativeTab;

import javax.annotation.Nullable;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import com.sosea1.powah.Powah;
import com.sosea1.powah.api.energy.endernetwork.IEnderExtender;
import com.sosea1.powah.common.energy.EnergyIntMath;
import com.sosea1.powah.common.energy.EnergyItemHelper;
import com.sosea1.powah.common.tier.PowahTier;

/** Tiered Powah battery backed directly by ItemStack NBT and exposed through standard Forge Energy. */
public final class ItemBattery extends Item implements IEnderExtender {
    private static final String NBT_ENERGY = "PowahEnergy";
    private static final String NBT_CHARGING = "PowahCharging";
    private final PowahTier tier;

    public ItemBattery(PowahTier tier) {
        this.tier = tier;
        setMaxStackSize(1);
        setCreativeTab(PowahCreativeTab.INSTANCE);
    }

    public PowahTier getTier() {
        return tier;
    }

    private long capacityLong() {
        return Math.max(0L, Powah.energyConfig().battery().capacity().get(tier));
    }

    private long transferLong() {
        return Math.max(0L, Powah.energyConfig().battery().transfer().get(tier));
    }

    @Override
    public ICapabilityProvider initCapabilities(final ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new ICapabilityProvider() {
            private final IEnergyStorage storage = new IEnergyStorage() {
                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    if (maxReceive <= 0) {
                        return 0;
                    }
                    long stored = currentStored(stack);
                    long accepted = Math.min((long) maxReceive, Math.min(transferLong(), capacityLong() - stored));
                    if (!simulate && accepted > 0L) {
                        setStored(stack, stored + accepted);
                    }
                    return EnergyIntMath.saturatedInt(Math.max(0L, accepted));
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    if (maxExtract <= 0) {
                        return 0;
                    }
                    long stored = currentStored(stack);
                    long extracted = Math.min((long) maxExtract, Math.min(transferLong(), stored));
                    if (!simulate && extracted > 0L) {
                        setStored(stack, stored - extracted);
                    }
                    return EnergyIntMath.saturatedInt(Math.max(0L, extracted));
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
                    return true;
                }

                @Override
                public boolean canReceive() {
                    return true;
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

    private static long stored(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? 0L : Math.max(0L, tag.getLong(NBT_ENERGY));
    }

    private long currentStored(ItemStack stack) {
        return Math.min(stored(stack), capacityLong());
    }

    private static void setStored(ItemStack stack, long energy) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setLong(NBT_ENERGY, Math.max(0L, energy));
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
        super.onUpdate(stack, world, entity, itemSlot, isSelected);
        if (world.isRemote || !(entity instanceof EntityPlayer) || !isCharging(stack)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) entity;
        IEnergyStorage source = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (source == null || !source.canExtract() || source.getEnergyStored() <= 0) {
            return;
        }
        EnergyItemHelper.chargePlayer(player, source, targetStack -> !(targetStack.getItem() instanceof ItemBattery));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            if (!world.isRemote) {
                setCharging(stack, !isCharging(stack));
            }
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
        }
        return super.onItemRightClick(world, player, hand);
    }

    public static boolean isCharging(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        return tag != null && tag.getBoolean(NBT_CHARGING);
    }

    public static void setCharging(ItemStack stack, boolean charging) {
        if (stack == null || stack.isEmpty()) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            if (!charging) return;
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        if (charging) tag.setBoolean(NBT_CHARGING, true);
        else tag.removeTag(NBT_CHARGING);
    }

    public static long getStoredEnergy(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemBattery)) return 0L;
        ItemBattery battery = (ItemBattery) stack.getItem();
        return Math.min(stored(stack), Math.max(0L, Powah.energyConfig().battery().capacity().get(battery.getTier())));
    }

    @Override
    public long getExtendedCapacity(ItemStack stack) {
        return tier.isCreative() ? 0L : Powah.energyConfig().battery().capacity().get(tier);
    }

    @Override
    public long getExtendedEnergy(ItemStack stack) {
        return tier.isCreative() ? 0L : getStoredEnergy(stack);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return isCharging(stack) || super.hasEffect(stack);
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return getStoredEnergy(stack) < capacityLong();
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        long capacity = capacityLong();
        if (capacity <= 0L) {
            return 1.0D;
        }
        return 1.0D - Math.min(1.0D, (double) getStoredEnergy(stack) / (double) capacity);
    }
}
