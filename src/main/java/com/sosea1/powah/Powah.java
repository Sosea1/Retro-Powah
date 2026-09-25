package com.sosea1.powah;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.sosea1.powah.common.config.EnergyConfigSnapshot;
import com.sosea1.powah.common.config.PowahConfig;
import com.sosea1.powah.compat.PowahCompatBootstrap;
import com.sosea1.powah.registry.PowahEntityRegistry;
import com.sosea1.powah.common.container.PowahGuiHandler;
import com.sosea1.powah.common.proxy.CommonProxy;
import com.sosea1.powah.api.PowahApi;
import com.sosea1.powah.content.cable.TileCable;
import com.sosea1.powah.content.energycell.TileEnergyCell;
import com.sosea1.powah.content.ender.TileEnderCell;
import com.sosea1.powah.content.ender.TileEnderGate;
import com.sosea1.powah.content.discharger.TileEnergyDischarger;
import com.sosea1.powah.content.hopper.TileEnergyHopper;
import com.sosea1.powah.content.transmitter.TilePlayerTransmitter;
import com.sosea1.powah.content.energizing.DefaultEnergizingRecipes;
import com.sosea1.powah.content.energizing.TileEnergizingOrb;
import com.sosea1.powah.content.energizing.TileEnergizingRod;
import com.sosea1.powah.content.furnator.TileFurnator;
import com.sosea1.powah.content.magmator.TileMagmator;
import com.sosea1.powah.content.solar.TileSolarPanel;
import com.sosea1.powah.content.thermo.TileThermoGenerator;
import com.sosea1.powah.content.reactor.TileReactor;
import com.sosea1.powah.content.reactor.TileReactorPart;
import com.sosea1.powah.registry.ModContent;
import com.sosea1.powah.world.PowahWorldGenerator;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = Powah.MOD_ID,
        name = Powah.MOD_NAME,
        version = Tags.VERSION,
        acceptedMinecraftVersions = "[1.12.2]"
)
public final class Powah {
    public static final String MOD_ID = "powah";
    public static final String MOD_NAME = "Powah! 1.12.2 Backport";
    public static final int GUI_MACHINE = 0;
    public static final int GUI_CABLE_BASE = 10;

    @Mod.Instance(MOD_ID)
    public static Powah INSTANCE;

    @SidedProxy(
            clientSide = "com.sosea1.powah.common.proxy.ClientProxy",
            serverSide = "com.sosea1.powah.common.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    private static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        PowahConfig.initialize(event.getSuggestedConfigurationFile());
        PowahEntityRegistry.register();
        PowahCompatBootstrap.preInit();
        GameRegistry.registerTileEntity(TileEnergyCell.class, new ResourceLocation(MOD_ID, "energy_cell"));
        GameRegistry.registerTileEntity(TileFurnator.class, new ResourceLocation(MOD_ID, "furnator"));
        GameRegistry.registerTileEntity(TileCable.class, new ResourceLocation(MOD_ID, "cable"));
        GameRegistry.registerTileEntity(TileMagmator.class, new ResourceLocation(MOD_ID, "magmator"));
        GameRegistry.registerTileEntity(TileSolarPanel.class, new ResourceLocation(MOD_ID, "solar_panel"));
        GameRegistry.registerTileEntity(TileThermoGenerator.class, new ResourceLocation(MOD_ID, "thermo_generator"));
        GameRegistry.registerTileEntity(TileEnergizingOrb.class, new ResourceLocation(MOD_ID, "energizing_orb"));
        GameRegistry.registerTileEntity(TileEnergizingRod.class, new ResourceLocation(MOD_ID, "energizing_rod"));
        GameRegistry.registerTileEntity(TileReactor.class, new ResourceLocation(MOD_ID, "reactor"));
        GameRegistry.registerTileEntity(TileReactorPart.class, new ResourceLocation(MOD_ID, "reactor_part"));
        proxy.preInit();
        GameRegistry.registerTileEntity(TileEnderCell.class, new ResourceLocation(MOD_ID, "ender_cell"));
        GameRegistry.registerTileEntity(TileEnderGate.class, new ResourceLocation(MOD_ID, "ender_gate"));
        GameRegistry.registerTileEntity(TilePlayerTransmitter.class, new ResourceLocation(MOD_ID, "player_transmitter"));
        GameRegistry.registerTileEntity(TileEnergyHopper.class, new ResourceLocation(MOD_ID, "energy_hopper"));
        GameRegistry.registerTileEntity(TileEnergyDischarger.class, new ResourceLocation(MOD_ID, "energy_discharger"));
        GameRegistry.registerWorldGenerator(new PowahWorldGenerator(), 0);
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new PowahGuiHandler());
        logger.info("Initializing {}", MOD_NAME);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        DefaultEnergizingRecipes.registerAll();
        PowahApi.registerHeatSource(ModContent.blazingCrystalBlock(), 2_800);
        PowahApi.registerReactorFuel(ModContent.uraninite(), 100.0D, 700);
        PowahApi.registerSolidCoolant(Item.getItemFromBlock(ModContent.dryIce()), 712.0D, -32);
        OreDictionary.registerOre("oreUraninitePoor", ModContent.uraniniteOrePoor());
        OreDictionary.registerOre("oreUraninite", ModContent.uraniniteOre());
        OreDictionary.registerOre("oreUraniniteDense", ModContent.uraniniteOreDense());
        OreDictionary.registerOre("uraniniteRaw", ModContent.uraniniteRaw());
        OreDictionary.registerOre("uraninite", ModContent.uraninite());
        GameRegistry.addSmelting(new ItemStack(ModContent.uraniniteRaw()), new ItemStack(ModContent.uraninite()), 0.7F);
    }

    public static Logger logger() {
        return logger;
    }

    public static EnergyConfigSnapshot energyConfig() {
        return PowahConfig.energy();
    }

    public static long configRevision() { return PowahConfig.revision(); }
}
