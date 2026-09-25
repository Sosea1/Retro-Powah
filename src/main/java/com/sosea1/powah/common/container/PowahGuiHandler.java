package com.sosea1.powah.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.block.entity.AbstractEnergyTile;
import com.sosea1.powah.content.cable.TileCable;

public final class PowahGuiHandler implements IGuiHandler {
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (id == Powah.GUI_MACHINE) {
            return tile instanceof AbstractEnergyTile
                    ? new ContainerPowahMachine(player.inventory, (AbstractEnergyTile) tile)
                    : null;
        }
        EnumFacing cableSide = decodeCableSide(id);
        return cableSide != null && tile instanceof TileCable
                ? new ContainerCableConfig(player.inventory, (TileCable) tile, cableSide)
                : null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (id == Powah.GUI_MACHINE) {
            if (!(tile instanceof AbstractEnergyTile)) return null;
            ContainerPowahMachine container = new ContainerPowahMachine(player.inventory, (AbstractEnergyTile) tile);
            return Powah.proxy.createMachineGui(player, world, container);
        }
        EnumFacing cableSide = decodeCableSide(id);
        if (cableSide == null || !(tile instanceof TileCable)) return null;
        ContainerCableConfig container = new ContainerCableConfig(player.inventory, (TileCable) tile, cableSide);
        return Powah.proxy.createCableGui(player, world, container);
    }

    private static EnumFacing decodeCableSide(int id) {
        int ordinal = id - Powah.GUI_CABLE_BASE;
        return ordinal >= 0 && ordinal < EnumFacing.values().length ? EnumFacing.byIndex(ordinal) : null;
    }
}
