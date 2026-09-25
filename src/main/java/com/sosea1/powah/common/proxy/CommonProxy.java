package com.sosea1.powah.common.proxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import com.sosea1.powah.common.container.ContainerPowahMachine;
import com.sosea1.powah.common.container.ContainerCableConfig;

public class CommonProxy {
    public void preInit() {
    }

    public Object createMachineGui(EntityPlayer player, World world, ContainerPowahMachine container) {
        return null;
    }

    public Object createCableGui(EntityPlayer player, World world, ContainerCableConfig container) {
        return null;
    }
}
