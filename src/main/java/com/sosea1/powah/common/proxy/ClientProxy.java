package com.sosea1.powah.common.proxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import com.sosea1.powah.client.render.ReactorPartRenderer;
import com.sosea1.powah.client.render.CableRenderer;
import com.sosea1.powah.client.render.EnergizingOrbRenderer;
import com.sosea1.powah.client.render.EnergizingRodRenderer;
import com.sosea1.powah.client.render.FurnatorRenderer;
import com.sosea1.powah.client.render.MagmatorRenderer;
import com.sosea1.powah.content.cable.TileCable;
import com.sosea1.powah.content.energizing.TileEnergizingOrb;
import com.sosea1.powah.content.energizing.TileEnergizingRod;
import com.sosea1.powah.content.furnator.TileFurnator;
import com.sosea1.powah.content.magmator.TileMagmator;
import com.sosea1.powah.client.render.ReactorRenderer;
import com.sosea1.powah.content.reactor.TileReactor;
import com.sosea1.powah.content.reactor.TileReactorPart;
import com.sosea1.powah.client.gui.GuiPowahMachine;
import com.sosea1.powah.client.gui.GuiCableConfig;
import com.sosea1.powah.common.container.ContainerPowahMachine;
import com.sosea1.powah.common.container.ContainerCableConfig;

public final class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileCable.class, new CableRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileReactor.class, new ReactorRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileReactorPart.class, new ReactorPartRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEnergizingOrb.class, new EnergizingOrbRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEnergizingRod.class, new EnergizingRodRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileFurnator.class, new FurnatorRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileMagmator.class, new MagmatorRenderer());
    }

    @Override
    public Object createMachineGui(EntityPlayer player, World world, ContainerPowahMachine container) {
        return new GuiPowahMachine(container, player.inventory);
    }

    @Override
    public Object createCableGui(EntityPlayer player, World world, ContainerCableConfig container) {
        return new GuiCableConfig(container);
    }
}
