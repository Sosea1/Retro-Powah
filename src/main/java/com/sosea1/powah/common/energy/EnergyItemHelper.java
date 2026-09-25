package com.sosea1.powah.common.energy;

import java.util.function.Predicate;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import com.sosea1.powah.ChargeableItemsEvent;

public final class EnergyItemHelper {
    private EnergyItemHelper() {
    }

    public static boolean canReceiveEnergy(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasCapability(CapabilityEnergy.ENERGY, null)) {
            return false;
        }
        IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY, null);
        return storage != null && storage.canReceive();
    }

    public static boolean canExtractEnergy(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasCapability(CapabilityEnergy.ENERGY, null)) {
            return false;
        }
        IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY, null);
        return storage != null && storage.canExtract() && storage.getEnergyStored() > 0;
    }


    /**
     * Charges vanilla player inventory/equipment plus stacks contributed through the modern
     * {@link ChargeableItemsEvent} compatibility hook.
     */
    public static long chargePlayer(EntityPlayer player, LongEnergyBuffer source, long perStack) {
        if (player == null || source == null || source.isEmpty() || perStack <= 0L) return 0L;
        long moved = 0L;
        for (ItemStack stack : player.inventory.mainInventory) moved += charge(stack, source, perStack);
        for (ItemStack stack : player.inventory.armorInventory) moved += charge(stack, source, perStack);
        for (ItemStack stack : player.inventory.offHandInventory) moved += charge(stack, source, perStack);

        ChargeableItemsEvent event = new ChargeableItemsEvent(player);
        MinecraftForge.EVENT_BUS.post(event);
        for (ItemStack stack : event.getItems()) moved += charge(stack, source, perStack);
        return moved;
    }

    /**
     * Item-source counterpart used by active Batteries. The source capability itself enforces
     * its per-operation transfer limit, while the predicate mirrors modern Powah's exclusions.
     */
    public static long chargePlayer(EntityPlayer player, IEnergyStorage source, Predicate<ItemStack> allowStack) {
        if (player == null || source == null || !source.canExtract() || source.getEnergyStored() <= 0) return 0L;
        long moved = 0L;
        moved += chargeIterableFromItem(source, player.inventory.mainInventory, allowStack);
        moved += chargeIterableFromItem(source, player.inventory.armorInventory, allowStack);
        moved += chargeIterableFromItem(source, player.inventory.offHandInventory, allowStack);

        ChargeableItemsEvent event = new ChargeableItemsEvent(player);
        MinecraftForge.EVENT_BUS.post(event);
        moved += chargeIterableFromItem(source, event.getItems(), allowStack);
        return moved;
    }

    private static long chargeIterableFromItem(IEnergyStorage source, Iterable<ItemStack> stacks, Predicate<ItemStack> allowStack) {
        long moved = 0L;
        for (ItemStack targetStack : stacks) {
            if (source.getEnergyStored() <= 0) break;
            if (targetStack == null || targetStack.isEmpty() || (allowStack != null && !allowStack.test(targetStack))
                    || !targetStack.hasCapability(CapabilityEnergy.ENERGY, null)) {
                continue;
            }
            IEnergyStorage target = targetStack.getCapability(CapabilityEnergy.ENERGY, null);
            if (target == null || !target.canReceive()) continue;
            int available = source.extractEnergy(Integer.MAX_VALUE, true);
            if (available <= 0) break;
            int accepted = target.receiveEnergy(available, true);
            if (accepted <= 0) continue;
            int extracted = source.extractEnergy(accepted, false);
            if (extracted <= 0) break;
            int actual = target.receiveEnergy(extracted, false);
            int safeActual = Math.max(0, actual);
            if (safeActual < extracted && source.canReceive()) {
                source.receiveEnergy(extracted - safeActual, false);
            }
            moved += safeActual;
        }
        return moved;
    }

    /** Extracts FE from one item into a long-backed machine buffer. */
    public static long discharge(ItemStack stack, LongEnergyBuffer target, long budget) {
        if (target == null || budget <= 0L || target.isFull() || !canExtractEnergy(stack)) return 0L;
        IEnergyStorage source = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (source == null) return 0L;
        long wanted = Math.min(budget, target.space());
        int offer = EnergyIntMath.saturatedInt(wanted);
        if (offer <= 0) return 0L;
        int simulated = source.extractEnergy(offer, true);
        if (simulated <= 0) return 0L;
        int actual = source.extractEnergy(simulated, false);
        if (actual <= 0) return 0L;
        long accepted = target.generate(actual, false);
        if (accepted < actual && source.canReceive()) {
            source.receiveEnergy(EnergyIntMath.saturatedInt(actual - accepted), false);
        }
        return accepted;
    }

    /**
     * Charges one item from a machine buffer. The item is simulated first so the source extracts exactly
     * what the target accepted. The per-call budget is long-backed but safely clamped for Forge Energy.
     */
    public static long charge(ItemStack stack, LongEnergyBuffer source, long budget) {
        if (source == null || budget <= 0L || source.isEmpty() || !canReceiveEnergy(stack)) {
            return 0L;
        }
        IEnergyStorage target = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (target == null) {
            return 0L;
        }

        long available = Math.min(budget, Math.min(source.energy(), source.maxExtract()));
        int offer = EnergyIntMath.saturatedInt(available);
        if (offer <= 0) {
            return 0L;
        }

        int accepted = target.receiveEnergy(offer, true);
        if (accepted <= 0) {
            return 0L;
        }
        long extracted = source.extract(accepted, false);
        if (extracted <= 0L) {
            return 0L;
        }

        int actuallyAccepted = target.receiveEnergy(EnergyIntMath.saturatedInt(extracted), false);
        if (actuallyAccepted < extracted) {
            // A capability changing between simulation and execution is unusual, but don't lose FE.
            source.generate(extracted - Math.max(0, actuallyAccepted), false);
        }
        return Math.max(0, actuallyAccepted);
    }
}
