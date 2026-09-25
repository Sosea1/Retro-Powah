package com.sosea1.powah.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.block.BlockPowahMachine;
import com.sosea1.powah.common.item.ItemPortableEnergyBlock;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.content.battery.ItemBattery;
import com.sosea1.powah.content.cable.BlockCable;
import com.sosea1.powah.content.energycell.BlockEnergyCell;
import com.sosea1.powah.content.energycell.ItemEnergyCell;
import com.sosea1.powah.content.ender.BlockEnderCell;
import com.sosea1.powah.content.ender.BlockEnderGate;
import com.sosea1.powah.content.discharger.BlockEnergyDischarger;
import com.sosea1.powah.content.hopper.BlockEnergyHopper;
import com.sosea1.powah.content.transmitter.BlockPlayerTransmitter;
import com.sosea1.powah.content.transmitter.ItemBindingCard;
import com.sosea1.powah.content.energizing.BlockEnergizingOrb;
import com.sosea1.powah.content.energizing.BlockEnergizingRod;
import com.sosea1.powah.content.furnator.BlockFurnator;
import com.sosea1.powah.content.magmator.BlockMagmator;
import com.sosea1.powah.content.material.BlockCrystalStorage;
import com.sosea1.powah.content.material.BlockDryIce;
import com.sosea1.powah.content.material.BlockUraniniteOre;
import com.sosea1.powah.content.material.ItemPowahMaterial;
import com.sosea1.powah.content.material.ItemWrench;
import com.sosea1.powah.content.reactor.BlockReactor;
import com.sosea1.powah.content.reactor.BlockReactorPart;
import com.sosea1.powah.content.reactor.ItemReactorBlock;
import com.sosea1.powah.content.solar.BlockSolarPanel;
import com.sosea1.powah.content.thermo.BlockThermoGenerator;
import com.sosea1.powah.content.special.ItemChargedSnowball;

@Mod.EventBusSubscriber(modid = Powah.MOD_ID)
public final class ModContent {
    private static final List<Block> BLOCKS = new ArrayList<Block>();
    private static final List<Item> ITEMS = new ArrayList<Item>();
    private static final Map<PowahTier, BlockEnergyCell> ENERGY_CELLS = new EnumMap<PowahTier, BlockEnergyCell>(PowahTier.class);
    private static final Map<PowahTier, BlockFurnator> FURNATORS = new EnumMap<PowahTier, BlockFurnator>(PowahTier.class);
    private static final Map<PowahTier, BlockCable> CABLES = new EnumMap<PowahTier, BlockCable>(PowahTier.class);
    private static final Map<PowahTier, BlockMagmator> MAGMATORS = new EnumMap<PowahTier, BlockMagmator>(PowahTier.class);
    private static final Map<PowahTier, BlockSolarPanel> SOLAR_PANELS = new EnumMap<PowahTier, BlockSolarPanel>(PowahTier.class);
    private static final Map<PowahTier, BlockThermoGenerator> THERMO_GENERATORS = new EnumMap<PowahTier, BlockThermoGenerator>(PowahTier.class);
    private static final Map<PowahTier, ItemBattery> BATTERIES = new EnumMap<PowahTier, ItemBattery>(PowahTier.class);
    private static final Map<PowahTier, BlockEnergizingRod> ENERGIZING_RODS = new EnumMap<PowahTier, BlockEnergizingRod>(PowahTier.class);
    private static final Map<PowahTier, BlockReactor> REACTORS = new EnumMap<PowahTier, BlockReactor>(PowahTier.class);
    private static final Map<PowahTier, BlockReactorPart> REACTOR_PARTS = new EnumMap<PowahTier, BlockReactorPart>(PowahTier.class);
    private static final Map<PowahTier, BlockEnderCell> ENDER_CELLS = new EnumMap<PowahTier, BlockEnderCell>(PowahTier.class);
    private static final Map<PowahTier, BlockEnderGate> ENDER_GATES = new EnumMap<PowahTier, BlockEnderGate>(PowahTier.class);
    private static final Map<PowahTier, BlockPlayerTransmitter> PLAYER_TRANSMITTERS = new EnumMap<PowahTier, BlockPlayerTransmitter>(PowahTier.class);
    private static final Map<PowahTier, BlockEnergyHopper> ENERGY_HOPPERS = new EnumMap<PowahTier, BlockEnergyHopper>(PowahTier.class);
    private static final Map<PowahTier, BlockEnergyDischarger> ENERGY_DISCHARGERS = new EnumMap<PowahTier, BlockEnergyDischarger>(PowahTier.class);
    private static final List<Block> INTERNAL_BLOCKS = new ArrayList<Block>();

    private static final BlockEnergizingOrb ENERGIZING_ORB = named(new BlockEnergizingOrb(), "energizing_orb");
    private static final ItemPowahMaterial DIELECTRIC_PASTE = named(new ItemPowahMaterial(), "dielectric_paste");
    private static final ItemPowahMaterial DIELECTRIC_ROD = named(new ItemPowahMaterial(), "dielectric_rod");
    private static final ItemPowahMaterial DIELECTRIC_CASING = named(new ItemPowahMaterial(), "dielectric_casing");
    private static final ItemPowahMaterial CAPACITOR_BASIC_TINY = named(new ItemPowahMaterial(), "capacitor_basic_tiny");
    private static final ItemPowahMaterial CAPACITOR_BASIC = named(new ItemPowahMaterial(), "capacitor_basic");
    private static final ItemPowahMaterial CAPACITOR_BASIC_LARGE = named(new ItemPowahMaterial(), "capacitor_basic_large");
    private static final ItemPowahMaterial CAPACITOR_HARDENED = named(new ItemPowahMaterial(), "capacitor_hardened");
    private static final ItemPowahMaterial CAPACITOR_BLAZING = named(new ItemPowahMaterial(), "capacitor_blazing");
    private static final ItemPowahMaterial CAPACITOR_NIOTIC = named(new ItemPowahMaterial(), "capacitor_niotic");
    private static final ItemPowahMaterial CAPACITOR_SPIRITED = named(new ItemPowahMaterial(), "capacitor_spirited");
    private static final ItemPowahMaterial CAPACITOR_NITRO = named(new ItemPowahMaterial(), "capacitor_nitro");
    private static final ItemPowahMaterial PHOTOELECTRIC_PANE = named(new ItemPowahMaterial(), "photoelectric_pane");
    private static final ItemPowahMaterial THERMOELECTRIC_PLATE = named(new ItemPowahMaterial(), "thermoelectric_plate");
    private static final ItemPowahMaterial URANINITE_RAW = named(new ItemPowahMaterial(), "uraninite_raw");
    private static final ItemPowahMaterial BLANK_CARD = named(new ItemPowahMaterial(), "blank_card");
    private static final ItemWrench WRENCH = named(new ItemWrench(), "wrench");
    private static final ItemPowahMaterial ENERGIZED_STEEL = named(new ItemPowahMaterial(), "steel_energized");
    private static final ItemPowahMaterial BLAZING_CRYSTAL = named(new ItemPowahMaterial(), "crystal_blazing");
    private static final ItemPowahMaterial NIOTIC_CRYSTAL = named(new ItemPowahMaterial(), "crystal_niotic");
    private static final ItemPowahMaterial SPIRITED_CRYSTAL = named(new ItemPowahMaterial(), "crystal_spirited");
    private static final ItemPowahMaterial NITRO_CRYSTAL = named(new ItemPowahMaterial(), "crystal_nitro");
    private static final ItemPowahMaterial URANINITE = named(new ItemPowahMaterial(), "uraninite");
    private static final ItemBindingCard BINDING_CARD = named(new ItemBindingCard(false), "binding_card");
    private static final ItemBindingCard DIMENSIONAL_BINDING_CARD = named(new ItemBindingCard(true), "binding_card_dim");
    private static final BlockCrystalStorage BLAZING_CRYSTAL_BLOCK = named(new BlockCrystalStorage(), "blazing_crystal_block");
    private static final BlockCrystalStorage NIOTIC_CRYSTAL_BLOCK = named(new BlockCrystalStorage(), "niotic_crystal_block");
    private static final BlockCrystalStorage SPIRITED_CRYSTAL_BLOCK = named(new BlockCrystalStorage(), "spirited_crystal_block");
    private static final BlockCrystalStorage NITRO_CRYSTAL_BLOCK = named(new BlockCrystalStorage(), "nitro_crystal_block");
    private static final BlockUraniniteOre URANINITE_ORE_POOR = named(new BlockUraniniteOre(URANINITE_RAW, 1), "uraninite_ore_poor");
    private static final BlockUraniniteOre URANINITE_ORE = named(new BlockUraniniteOre(URANINITE_RAW, 2), "uraninite_ore");
    private static final BlockUraniniteOre URANINITE_ORE_DENSE = named(new BlockUraniniteOre(URANINITE_RAW, 4), "uraninite_ore_dense");
    private static final BlockDryIce DRY_ICE = named(new BlockDryIce(), "dry_ice");
    private static final BlockEnergyCell CREATIVE_ENERGY_CELL = named(new BlockEnergyCell(PowahTier.CREATIVE), "energy_cell_creative");
    private static final BlockCrystalStorage ENERGIZED_STEEL_BLOCK = named(new BlockCrystalStorage(), "energized_steel_block");
    private static final BlockCrystalStorage URANINITE_BLOCK = named(new BlockCrystalStorage(), "uraninite_block");
    private static final BlockCrystalStorage URANINITE_RAW_BLOCK = named(new BlockCrystalStorage(), "uraninite_raw_block");
    private static final ItemPowahMaterial DIELECTRIC_ROD_HORIZONTAL = named(new ItemPowahMaterial(), "dielectric_rod_horizontal");
    private static final ItemPowahMaterial ENDER_CORE = named(new ItemPowahMaterial(), "ender_core");
    private static final ItemPowahMaterial AERIAL_PEARL = named(new ItemPowahMaterial(), "aerial_pearl");
    private static final ItemPowahMaterial PLAYER_AERIAL_PEARL = named(new ItemPowahMaterial(), "player_aerial_pearl");
    private static final ItemPowahMaterial LENS_OF_ENDER = named(new ItemPowahMaterial(), "lens_of_ender");
    private static final ItemChargedSnowball CHARGED_SNOWBALL = named(new ItemChargedSnowball(), "charged_snowball");

    static {
        for (PowahTier tier : PowahTier.normalValues()) {
            String suffix = tier.name().toLowerCase(Locale.ROOT);
            BlockEnergyCell cell = named(new BlockEnergyCell(tier), "energy_cell_" + suffix);
            BlockFurnator furnator = named(new BlockFurnator(tier), "furnator_" + suffix);
            BlockCable cable = named(new BlockCable(tier), "energy_cable_" + suffix);
            BlockMagmator magmator = named(new BlockMagmator(tier), "magmator_" + suffix);
            BlockSolarPanel solar = named(new BlockSolarPanel(tier), "solar_panel_" + suffix);
            BlockThermoGenerator thermo = named(new BlockThermoGenerator(tier), "thermo_generator_" + suffix);
            ItemBattery battery = named(new ItemBattery(tier), "battery_" + suffix);
            BlockEnergizingRod rod = named(new BlockEnergizingRod(tier), "energizing_rod_" + suffix);
            BlockReactor reactor = named(new BlockReactor(tier), "reactor_" + suffix);
            BlockReactorPart reactorPart = named(new BlockReactorPart(tier), "reactor_part_" + suffix);
            BlockEnderCell enderCell = named(new BlockEnderCell(tier), "ender_cell_" + suffix);
            BlockEnderGate enderGate = named(new BlockEnderGate(tier), "ender_gate_" + suffix);
            BlockPlayerTransmitter transmitter = named(new BlockPlayerTransmitter(tier), "player_transmitter_" + suffix);
            BlockEnergyHopper hopper = named(new BlockEnergyHopper(tier), "energy_hopper_" + suffix);
            BlockEnergyDischarger discharger = named(new BlockEnergyDischarger(tier), "energy_discharger_" + suffix);

            ENERGY_CELLS.put(tier, cell);
            FURNATORS.put(tier, furnator);
            CABLES.put(tier, cable);
            MAGMATORS.put(tier, magmator);
            SOLAR_PANELS.put(tier, solar);
            THERMO_GENERATORS.put(tier, thermo);
            BATTERIES.put(tier, battery);
            ENERGIZING_RODS.put(tier, rod);
            REACTORS.put(tier, reactor);
            REACTOR_PARTS.put(tier, reactorPart);
            ENDER_CELLS.put(tier, enderCell);
            ENDER_GATES.put(tier, enderGate);
            PLAYER_TRANSMITTERS.put(tier, transmitter);
            ENERGY_HOPPERS.put(tier, hopper);
            ENERGY_DISCHARGERS.put(tier, discharger);

            BLOCKS.add(cell);
            BLOCKS.add(furnator);
            BLOCKS.add(cable);
            BLOCKS.add(magmator);
            BLOCKS.add(solar);
            BLOCKS.add(thermo);
            ITEMS.add(battery);
            BLOCKS.add(rod);
            BLOCKS.add(reactor);
            BLOCKS.add(enderCell);
            BLOCKS.add(enderGate);
            BLOCKS.add(transmitter);
            BLOCKS.add(hopper);
            BLOCKS.add(discharger);
            INTERNAL_BLOCKS.add(reactorPart);
        }

        BLOCKS.add(ENERGIZING_ORB);
        BLOCKS.add(BLAZING_CRYSTAL_BLOCK);
        BLOCKS.add(NIOTIC_CRYSTAL_BLOCK);
        BLOCKS.add(SPIRITED_CRYSTAL_BLOCK);
        BLOCKS.add(NITRO_CRYSTAL_BLOCK);
        BLOCKS.add(URANINITE_ORE_POOR);
        BLOCKS.add(URANINITE_ORE);
        BLOCKS.add(URANINITE_ORE_DENSE);
        BLOCKS.add(DRY_ICE);
        BLOCKS.add(CREATIVE_ENERGY_CELL);
        BLOCKS.add(ENERGIZED_STEEL_BLOCK);
        BLOCKS.add(URANINITE_BLOCK);
        BLOCKS.add(URANINITE_RAW_BLOCK);
        ITEMS.add(DIELECTRIC_PASTE);
        ITEMS.add(DIELECTRIC_ROD);
        ITEMS.add(DIELECTRIC_CASING);
        ITEMS.add(CAPACITOR_BASIC_TINY);
        ITEMS.add(CAPACITOR_BASIC);
        ITEMS.add(CAPACITOR_BASIC_LARGE);
        ITEMS.add(CAPACITOR_HARDENED);
        ITEMS.add(CAPACITOR_BLAZING);
        ITEMS.add(CAPACITOR_NIOTIC);
        ITEMS.add(CAPACITOR_SPIRITED);
        ITEMS.add(CAPACITOR_NITRO);
        ITEMS.add(PHOTOELECTRIC_PANE);
        ITEMS.add(THERMOELECTRIC_PLATE);
        ITEMS.add(URANINITE_RAW);
        ITEMS.add(BLANK_CARD);
        ITEMS.add(WRENCH);
        ITEMS.add(ENERGIZED_STEEL);
        ITEMS.add(BLAZING_CRYSTAL);
        ITEMS.add(NIOTIC_CRYSTAL);
        ITEMS.add(SPIRITED_CRYSTAL);
        ITEMS.add(NITRO_CRYSTAL);
        ITEMS.add(URANINITE);
        ITEMS.add(BINDING_CARD);
        ITEMS.add(DIMENSIONAL_BINDING_CARD);
        ITEMS.add(DIELECTRIC_ROD_HORIZONTAL);
        ITEMS.add(ENDER_CORE);
        ITEMS.add(AERIAL_PEARL);
        ITEMS.add(PLAYER_AERIAL_PEARL);
        ITEMS.add(LENS_OF_ENDER);
        ITEMS.add(CHARGED_SNOWBALL);
    }

    private ModContent() {
    }

    private static <T extends Block> T named(T block, String name) {
        block.setRegistryName(new ResourceLocation(Powah.MOD_ID, name));
        block.setTranslationKey(Powah.MOD_ID + "." + name);
        return block;
    }

    private static <T extends Item> T named(T item, String name) {
        item.setRegistryName(new ResourceLocation(Powah.MOD_ID, name));
        item.setTranslationKey(Powah.MOD_ID + "." + name);
        return item;
    }

    public static BlockEnergyCell energyCell(PowahTier tier) { return ENERGY_CELLS.get(tier); }
    public static BlockFurnator furnator(PowahTier tier) { return FURNATORS.get(tier); }
    public static BlockCable cable(PowahTier tier) { return CABLES.get(tier); }
    public static BlockMagmator magmator(PowahTier tier) { return MAGMATORS.get(tier); }
    public static BlockSolarPanel solarPanel(PowahTier tier) { return SOLAR_PANELS.get(tier); }
    public static BlockThermoGenerator thermoGenerator(PowahTier tier) { return THERMO_GENERATORS.get(tier); }
    public static ItemBattery battery(PowahTier tier) { return BATTERIES.get(tier); }
    public static BlockEnergizingRod energizingRod(PowahTier tier) { return ENERGIZING_RODS.get(tier); }
    public static BlockEnergizingOrb energizingOrb() { return ENERGIZING_ORB; }
    public static BlockReactor reactor(PowahTier tier) { return REACTORS.get(tier); }
    public static BlockReactorPart reactorPart(PowahTier tier) { return REACTOR_PARTS.get(tier); }
    public static BlockEnderCell enderCell(PowahTier tier) { return ENDER_CELLS.get(tier); }
    public static BlockEnderGate enderGate(PowahTier tier) { return ENDER_GATES.get(tier); }
    public static BlockPlayerTransmitter playerTransmitter(PowahTier tier) { return PLAYER_TRANSMITTERS.get(tier); }
    public static BlockEnergyHopper energyHopper(PowahTier tier) { return ENERGY_HOPPERS.get(tier); }
    public static BlockEnergyDischarger energyDischarger(PowahTier tier) { return ENERGY_DISCHARGERS.get(tier); }
    public static ItemPowahMaterial dielectricPaste() { return DIELECTRIC_PASTE; }
    public static ItemPowahMaterial dielectricRod() { return DIELECTRIC_ROD; }
    public static ItemPowahMaterial dielectricCasing() { return DIELECTRIC_CASING; }
    public static ItemPowahMaterial capacitorBasicTiny() { return CAPACITOR_BASIC_TINY; }
    public static ItemPowahMaterial capacitorBasic() { return CAPACITOR_BASIC; }
    public static ItemPowahMaterial capacitorBasicLarge() { return CAPACITOR_BASIC_LARGE; }
    public static ItemPowahMaterial capacitorHardened() { return CAPACITOR_HARDENED; }
    public static ItemPowahMaterial capacitorBlazing() { return CAPACITOR_BLAZING; }
    public static ItemPowahMaterial capacitorNiotic() { return CAPACITOR_NIOTIC; }
    public static ItemPowahMaterial capacitorSpirited() { return CAPACITOR_SPIRITED; }
    public static ItemPowahMaterial capacitorNitro() { return CAPACITOR_NITRO; }
    public static ItemPowahMaterial photoelectricPane() { return PHOTOELECTRIC_PANE; }
    public static ItemPowahMaterial thermoelectricPlate() { return THERMOELECTRIC_PLATE; }
    public static ItemPowahMaterial uraniniteRaw() { return URANINITE_RAW; }
    public static ItemPowahMaterial blankCard() { return BLANK_CARD; }
    public static ItemWrench wrench() { return WRENCH; }
    public static ItemPowahMaterial energizedSteel() { return ENERGIZED_STEEL; }
    public static ItemPowahMaterial blazingCrystal() { return BLAZING_CRYSTAL; }
    public static ItemPowahMaterial nioticCrystal() { return NIOTIC_CRYSTAL; }
    public static ItemPowahMaterial spiritedCrystal() { return SPIRITED_CRYSTAL; }
    public static ItemPowahMaterial nitroCrystal() { return NITRO_CRYSTAL; }
    public static ItemPowahMaterial uraninite() { return URANINITE; }
    public static ItemBindingCard bindingCard() { return BINDING_CARD; }
    public static ItemBindingCard dimensionalBindingCard() { return DIMENSIONAL_BINDING_CARD; }
    public static BlockCrystalStorage blazingCrystalBlock() { return BLAZING_CRYSTAL_BLOCK; }
    public static BlockCrystalStorage nioticCrystalBlock() { return NIOTIC_CRYSTAL_BLOCK; }
    public static BlockCrystalStorage spiritedCrystalBlock() { return SPIRITED_CRYSTAL_BLOCK; }
    public static BlockCrystalStorage nitroCrystalBlock() { return NITRO_CRYSTAL_BLOCK; }
    public static BlockUraniniteOre uraniniteOrePoor() { return URANINITE_ORE_POOR; }
    public static BlockUraniniteOre uraniniteOre() { return URANINITE_ORE; }
    public static BlockUraniniteOre uraniniteOreDense() { return URANINITE_ORE_DENSE; }
    public static BlockDryIce dryIce() { return DRY_ICE; }
    public static BlockEnergyCell creativeEnergyCell() { return CREATIVE_ENERGY_CELL; }
    public static BlockCrystalStorage energizedSteelBlock() { return ENERGIZED_STEEL_BLOCK; }
    public static BlockCrystalStorage uraniniteBlock() { return URANINITE_BLOCK; }
    public static BlockCrystalStorage uraniniteRawBlock() { return URANINITE_RAW_BLOCK; }
    public static ItemPowahMaterial dielectricRodHorizontal() { return DIELECTRIC_ROD_HORIZONTAL; }
    public static ItemPowahMaterial enderCore() { return ENDER_CORE; }
    public static ItemPowahMaterial aerialPearl() { return AERIAL_PEARL; }
    public static ItemPowahMaterial playerAerialPearl() { return PLAYER_AERIAL_PEARL; }
    public static ItemPowahMaterial lensOfEnder() { return LENS_OF_ENDER; }
    public static ItemChargedSnowball chargedSnowball() { return CHARGED_SNOWBALL; }

    public static List<Block> blocks() { return Collections.unmodifiableList(BLOCKS); }
    public static List<Block> internalBlocks() { return Collections.unmodifiableList(INTERNAL_BLOCKS); }
    public static List<Item> items() { return Collections.unmodifiableList(ITEMS); }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        for (Block block : BLOCKS) {
            event.getRegistry().register(block);
        }
        for (Block block : INTERNAL_BLOCKS) {
            event.getRegistry().register(block);
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (Block block : BLOCKS) {
            ItemBlock item;
            if (block instanceof BlockReactor) item = new ItemReactorBlock((BlockReactor) block);
            else if (block instanceof BlockEnergyCell) item = new ItemEnergyCell((BlockEnergyCell) block);
            else if (ItemPortableEnergyBlock.supports(block)) item = new ItemPortableEnergyBlock(block);
            else item = new ItemBlock(block);
            if (block instanceof BlockPowahMachine && ((BlockPowahMachine) block).keepsEnergyOnBreak()) {
                item.setMaxStackSize(1);
            }
            if (block instanceof BlockPlayerTransmitter || block instanceof BlockEnergyHopper || block instanceof BlockEnergyDischarger) {
                item.setMaxStackSize(1);
            }
            item.setRegistryName(block.getRegistryName());
            event.getRegistry().register(item);
        }
        for (Item item : ITEMS) {
            event.getRegistry().register(item);
        }
    }
}
