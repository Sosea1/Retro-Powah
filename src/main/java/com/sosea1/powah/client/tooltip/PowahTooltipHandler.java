package com.sosea1.powah.client.tooltip;

import java.util.List;
import java.util.Locale;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.block.BlockPowahMachine;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.content.battery.ItemBattery;
import com.sosea1.powah.content.cable.BlockCable;
import com.sosea1.powah.content.discharger.BlockEnergyDischarger;
import com.sosea1.powah.content.ender.BlockEnderCell;
import com.sosea1.powah.content.ender.BlockEnderGate;
import com.sosea1.powah.content.energizing.BlockEnergizingOrb;
import com.sosea1.powah.content.energizing.BlockEnergizingRod;
import com.sosea1.powah.content.energycell.BlockEnergyCell;
import com.sosea1.powah.content.furnator.BlockFurnator;
import com.sosea1.powah.content.hopper.BlockEnergyHopper;
import com.sosea1.powah.content.magmator.BlockMagmator;
import com.sosea1.powah.content.material.ItemWrench;
import com.sosea1.powah.content.reactor.BlockReactor;
import com.sosea1.powah.content.solar.BlockSolarPanel;
import com.sosea1.powah.content.thermo.BlockThermoGenerator;
import com.sosea1.powah.content.transmitter.BlockPlayerTransmitter;
import com.sosea1.powah.content.transmitter.ItemBindingCard;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = Powah.MOD_ID, value = Side.CLIENT)
public final class PowahTooltipHandler {
    private PowahTooltipHandler() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        Item item = stack.getItem();
        if (item.getRegistryName() == null || !Powah.MOD_ID.equals(item.getRegistryName().getNamespace())) return;
        List<String> tooltip = event.getToolTip();
        boolean shift = GuiScreen.isShiftKeyDown();
        if (item instanceof ItemBlock) {
            handleBlockTooltip(((ItemBlock) item).getBlock(), stack, tooltip, shift);
        } else {
            handleItemTooltip(item, stack, tooltip, shift);
        }
    }

    private static void handleBlockTooltip(Block block, ItemStack stack, List<String> tooltip, boolean shift) {
        if (block instanceof BlockFurnator) {
            PowahTier tier = ((BlockFurnator) block).getTier();
            long gen = Powah.energyConfig().furnator().generation().get(tier);
            if (!shift) { tooltip.add(I18n.format("tooltip.powah.hold_shift")); }
            else { tooltip.add(I18n.format("tooltip.powah.gen_rate", format(gen))); tooltip.add(I18n.format("tooltip.powah.furnator.fuel")); }
        } else if (block instanceof BlockMagmator) {
            PowahTier tier = ((BlockMagmator) block).getTier();
            long gen = Powah.energyConfig().magmator().generation().get(tier);
            if (!shift) { tooltip.add(I18n.format("tooltip.powah.hold_shift")); }
            else { tooltip.add(I18n.format("tooltip.powah.gen_rate", format(gen))); tooltip.add(I18n.format("tooltip.powah.magmator.fuel")); }
        } else if (block instanceof BlockThermoGenerator) {
            PowahTier tier = ((BlockThermoGenerator) block).getTier();
            long gen = Powah.energyConfig().thermoGenerator().generation().get(tier);
            if (!shift) { tooltip.add(I18n.format("tooltip.powah.hold_shift")); }
            else { tooltip.add(I18n.format("tooltip.powah.gen_rate", format(gen))); tooltip.add(I18n.format("tooltip.powah.thermo.source")); }
        } else if (block instanceof BlockSolarPanel) {
            PowahTier tier = ((BlockSolarPanel) block).getTier();
            long gen = Powah.energyConfig().solarPanel().generation().get(tier);
            if (!shift) { tooltip.add(I18n.format("tooltip.powah.hold_shift")); }
            else { tooltip.add(I18n.format("tooltip.powah.gen_rate", format(gen))); tooltip.add(I18n.format("tooltip.powah.solar.source")); }
        } else if (block instanceof BlockReactor) {
            PowahTier tier = ((BlockReactor) block).getTier();
            long gen = Powah.energyConfig().reactor().generation().get(tier);
            if (!shift) { tooltip.add(I18n.format("tooltip.powah.hold_shift")); }
            else { tooltip.add(I18n.format("tooltip.powah.gen_rate", format(gen))); tooltip.add(I18n.format("tooltip.powah.reactor.info")); }
        } else if (block instanceof BlockCable) {
            PowahTier tier = ((BlockCable) block).getTier();
            long transfer = Powah.energyConfig().cableTransfer().get(tier);
            if (!shift) { tooltip.add(I18n.format("tooltip.powah.hold_shift")); }
            else { tooltip.add(I18n.format("tooltip.powah.transfer_rate", format(transfer))); tooltip.add(I18n.format("tooltip.powah.cable.info")); }
        } else if (block instanceof BlockEnergyCell) {
            PowahTier tier = ((BlockEnergyCell) block).getTier();
            if (tier != PowahTier.CREATIVE) {
                long cap = Powah.energyConfig().energyCell().capacity().get(tier);
                long transfer = Powah.energyConfig().energyCell().transfer().get(tier);
                long stored = BlockPowahMachine.getPortableEnergy(stack);
                tooltip.add(I18n.format("tooltip.powah.energy_stored", format(stored), format(cap)));
                if (!shift) { tooltip.add(I18n.format("tooltip.powah.hold_shift")); }
                else { tooltip.add(I18n.format("tooltip.powah.max_io", format(transfer))); }
            }
        } else if (block instanceof BlockEnergizingRod) {
            PowahTier tier = ((BlockEnergizingRod) block).getTier();
            long transfer = Powah.energyConfig().energizingRod().transfer().get(tier);
            if (!shift) { tooltip.add(I18n.format("tooltip.powah.hold_shift")); }
            else { tooltip.add(I18n.format("tooltip.powah.energizing_speed", format(transfer))); tooltip.add(I18n.format("tooltip.powah.rod.info")); }
        } else if (block instanceof BlockEnergizingOrb) {
            if (!shift) { tooltip.add(I18n.format("tooltip.powah.hold_shift")); }
            else { tooltip.add(I18n.format("tooltip.powah.orb.info")); }
        } else if (block instanceof BlockPlayerTransmitter) {
            PowahTier tier = ((BlockPlayerTransmitter) block).getTier();
            long transfer = Powah.energyConfig().playerTransmitterCharging().get(tier);
            if (!shift) { tooltip.add(I18n.format("tooltip.powah.hold_shift")); }
            else { tooltip.add(I18n.format("tooltip.powah.transfer_rate", format(transfer))); tooltip.add(I18n.format("tooltip.powah.transmitter.info")); }
        } else if (block instanceof BlockEnderCell) {
            PowahTier tier = ((BlockEnderCell) block).getTier();
            long transfer = Powah.energyConfig().enderCellTransfer().get(tier);
            if (!shift) { tooltip.add(I18n.format("tooltip.powah.hold_shift")); }
            else { tooltip.add(I18n.format("tooltip.powah.transfer_rate", format(transfer))); tooltip.add(I18n.format("tooltip.powah.ender.info")); }
        } else if (block instanceof BlockEnderGate) {
            PowahTier tier = ((BlockEnderGate) block).getTier();
            long transfer = Powah.energyConfig().enderGateTransfer().get(tier);
            if (!shift) { tooltip.add(I18n.format("tooltip.powah.hold_shift")); }
            else { tooltip.add(I18n.format("tooltip.powah.transfer_rate", format(transfer))); tooltip.add(I18n.format("tooltip.powah.ender.info")); }
        } else if (block instanceof BlockEnergyHopper) {
            PowahTier tier = ((BlockEnergyHopper) block).getTier();
            long transfer = Powah.energyConfig().hopperCharging().get(tier);
            if (!shift) { tooltip.add(I18n.format("tooltip.powah.hold_shift")); }
            else { tooltip.add(I18n.format("tooltip.powah.transfer_rate", format(transfer))); tooltip.add(I18n.format("tooltip.powah.hopper.info")); }
        } else if (block instanceof BlockEnergyDischarger) {
            PowahTier tier = ((BlockEnergyDischarger) block).getTier();
            long transfer = Powah.energyConfig().discharger().transfer().get(tier);
            if (!shift) { tooltip.add(I18n.format("tooltip.powah.hold_shift")); }
            else { tooltip.add(I18n.format("tooltip.powah.transfer_rate", format(transfer))); tooltip.add(I18n.format("tooltip.powah.discharger.info")); }
        }
    }

    private static void handleItemTooltip(Item item, ItemStack stack, List<String> tooltip, boolean shift) {
        if (item instanceof ItemBattery) {
            PowahTier tier = ((ItemBattery) item).getTier();
            long cap = Powah.energyConfig().battery().capacity().get(tier);
            long transfer = Powah.energyConfig().battery().transfer().get(tier);
            long stored = ItemBattery.getStoredEnergy(stack);
            tooltip.add(I18n.format("tooltip.powah.energy_stored", format(stored), format(cap)));
            if (ItemBattery.isCharging(stack)) {
                tooltip.add(I18n.format("tooltip.powah.battery.charging"));
            } else {
                tooltip.add(I18n.format("tooltip.powah.battery.idle"));
            }
            if (!shift) { tooltip.add(I18n.format("tooltip.powah.hold_shift")); }
            else { tooltip.add(I18n.format("tooltip.powah.max_io", format(transfer))); }
        } else if (item instanceof ItemWrench) {
            if (!shift) { tooltip.add(I18n.format("tooltip.powah.hold_shift")); }
            else { tooltip.add(I18n.format("tooltip.powah.wrench.info")); }
        } else if (item instanceof ItemBindingCard) {
            boolean dim = ((ItemBindingCard) item).isDimensional();
            if (!shift) { tooltip.add(I18n.format("tooltip.powah.hold_shift")); }
            else { tooltip.add(I18n.format(dim ? "tooltip.powah.binding_card_dim.info" : "tooltip.powah.binding_card.info")); }
        }
    }

    private static String format(long val) {
        return String.format(Locale.ROOT, "%,d", val);
    }
}
