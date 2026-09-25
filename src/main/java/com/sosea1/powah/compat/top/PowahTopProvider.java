package com.sosea1.powah.compat.top;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import com.sosea1.powah.common.block.entity.AbstractEnergyTile;
import com.sosea1.powah.content.energizing.TileEnergizingOrb;
import com.sosea1.powah.content.reactor.TileReactor;

/** TOP-side server provider. Text keeps the full long FE value instead of truncating to Forge's int API. */
public final class PowahTopProvider implements IProbeInfoProvider {
    @Override public String getID() { return "powah:machines"; }
    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo info, EntityPlayer player, World world, IBlockState state, IProbeHitData data) {
        TileEntity tile=world.getTileEntity(data.getPos());
        if(tile instanceof AbstractEnergyTile){
            AbstractEnergyTile e=(AbstractEnergyTile)tile;
            info.text("Energy: "+format(e.getEnergyBuffer().energy())+" / "+format(e.getEnergyBuffer().capacity())+" FE");
        }
        if(tile instanceof TileReactor){ TileReactor r=(TileReactor)tile; info.text("Reactor: "+(r.isBuilt()?"formed":"unformed")+", "+format(r.getCurrentProduction())+" FE/t"); }
        if(tile instanceof TileEnergizingOrb){ TileEnergizingOrb o=(TileEnergizingOrb)tile; if(o.containsRecipe()) info.text("Energizing: "+format(o.getProgress())+" / "+format(o.getRequiredEnergy())+" FE"); }
    }

    private static String format(long value) {
        return String.format(java.util.Locale.ROOT, "%,d", Math.max(0L, value));
    }
}
