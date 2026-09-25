package com.sosea1.powah.compat.waila;

import java.util.List;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.sosea1.powah.common.block.entity.AbstractEnergyTile;
import com.sosea1.powah.content.energizing.TileEnergizingOrb;
import com.sosea1.powah.content.reactor.TileReactor;
import com.sosea1.powah.content.reactor.TileReactorPart;
import com.sosea1.powah.registry.ModContent;

public final class PowahWailaProvider implements IWailaDataProvider {
    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        TileEntity tile = accessor.getTileEntity();
        if (tile instanceof TileReactorPart) {
            TileReactorPart part = (TileReactorPart) tile;
            TileReactor core = part.getCore();
            return new ItemStack(ModContent.reactor(core == null ? part.getTier() : core.getTier()));
        }
        return null;
    }
    @Override public List<String> getWailaHead(ItemStack stack,List<String> tip,IWailaDataAccessor accessor,IWailaConfigHandler config){ return tip; }
    @Override public List<String> getWailaTail(ItemStack stack,List<String> tip,IWailaDataAccessor accessor,IWailaConfigHandler config){ return tip; }

    @Override
    public List<String> getWailaBody(ItemStack stack,List<String> tip,IWailaDataAccessor accessor,IWailaConfigHandler config){
        NBTTagCompound tag=accessor.getNBTData();
        if(tag.hasKey("PowahEnergy")) tip.add(net.minecraft.client.resources.I18n.format("tooltip.powah.energy_stored", format(tag.getLong("PowahEnergy")), format(tag.getLong("PowahCapacity"))));
        if(tag.hasKey("PowahProduction")) tip.add(net.minecraft.client.resources.I18n.format("tooltip.powah.gen_rate", format(tag.getLong("PowahProduction"))));
        if(tag.hasKey("PowahEnergizingRequired")) tip.add(net.minecraft.client.resources.I18n.format("gui.powah.hud.energizing", format(tag.getLong("PowahEnergizingProgress")), format(tag.getLong("PowahEnergizingRequired"))));
        return tip;
    }

    private static String format(long value) {
        return String.format(java.util.Locale.ROOT, "%,d", Math.max(0L, value));
    }

    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player,TileEntity tile,NBTTagCompound tag,World world,BlockPos pos){
        if(tile instanceof AbstractEnergyTile){ AbstractEnergyTile e=(AbstractEnergyTile)tile; tag.setLong("PowahEnergy",e.getEnergyBuffer().energy()); tag.setLong("PowahCapacity",e.getEnergyBuffer().capacity()); }
        if(tile instanceof TileReactor) tag.setLong("PowahProduction",((TileReactor)tile).getCurrentProduction());
        if(tile instanceof TileEnergizingOrb){ TileEnergizingOrb o=(TileEnergizingOrb)tile; if(o.containsRecipe()){tag.setLong("PowahEnergizingProgress",o.getProgress());tag.setLong("PowahEnergizingRequired",o.getRequiredEnergy());}}
        return tag;
    }
}
