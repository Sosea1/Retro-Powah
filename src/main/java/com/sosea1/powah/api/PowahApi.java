package com.sosea1.powah.api;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.init.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import com.sosea1.powah.common.recipe.EnergizingRecipe;
import com.sosea1.powah.common.recipe.EnergizingRecipeManager;
import com.sosea1.powah.common.reactor.ReactorFuelSpec;
import com.sosea1.powah.common.reactor.SolidCoolantSpec;

/**
 * Small 1.12.2 compatibility registry for data that modern Powah sources from data maps.
 * Integrations can register additional fluids/blocks without depending on machine internals.
 */
public final class PowahApi {
    private static final Map<Fluid, Long> MAGMATIC_ENERGY_PER_100_MB = new IdentityHashMap<Fluid, Long>();
    private static final Map<Fluid, Integer> COOLANT_TEMPERATURE = new IdentityHashMap<Fluid, Integer>();
    private static final Map<Block, Integer> HEAT_SOURCE_TEMPERATURE = new IdentityHashMap<Block, Integer>();
    private static final Map<Item, ReactorFuelSpec> REACTOR_FUELS = new IdentityHashMap<Item, ReactorFuelSpec>();
    private static final Map<Item, SolidCoolantSpec> SOLID_COOLANTS = new IdentityHashMap<Item, SolidCoolantSpec>();

    static {
        registerMagmaticFluid(FluidRegistry.LAVA, 10_000L);
        registerCoolant(FluidRegistry.WATER, 0);
        registerHeatSource(Blocks.LAVA, 1_000);
        registerHeatSource(Blocks.FLOWING_LAVA, 1_000);
        registerHeatSource(Blocks.MAGMA, 800);
        registerSolidCoolant(Items.SNOWBALL, 12.0D, -3);
        registerSolidCoolant(Item.getItemFromBlock(Blocks.SNOW), 48.0D, -3);
        registerSolidCoolant(Item.getItemFromBlock(Blocks.ICE), 48.0D, -5);
        registerSolidCoolant(Item.getItemFromBlock(Blocks.PACKED_ICE), 192.0D, -8);
    }

    private PowahApi() {
    }

    public static void registerMagmaticFluid(Fluid fluid, long energyPer100Mb) {
        if (fluid == null) {
            throw new NullPointerException("fluid");
        }
        if (energyPer100Mb <= 0L) {
            throw new IllegalArgumentException("energyPer100Mb must be > 0");
        }
        MAGMATIC_ENERGY_PER_100_MB.put(fluid, energyPer100Mb);
    }

    public static long getMagmaticFluidEnergyPer100Mb(Fluid fluid) {
        Long value = MAGMATIC_ENERGY_PER_100_MB.get(fluid);
        return value == null ? 0L : value.longValue();
    }

    public static boolean isMagmaticFluid(Fluid fluid) {
        return getMagmaticFluidEnergyPer100Mb(fluid) > 0L;
    }

    public static void registerCoolant(Fluid fluid, int temperature) {
        if (fluid == null) {
            throw new NullPointerException("fluid");
        }
        COOLANT_TEMPERATURE.put(fluid, Integer.valueOf(temperature));
    }

    public static boolean isCoolant(Fluid fluid) {
        return fluid != null && COOLANT_TEMPERATURE.containsKey(fluid);
    }

    public static int getCoolantTemperature(Fluid fluid) {
        Integer value = COOLANT_TEMPERATURE.get(fluid);
        return value == null ? Integer.MAX_VALUE : value.intValue();
    }

    public static void registerHeatSource(Block block, int temperature) {
        if (block == null) {
            throw new NullPointerException("block");
        }
        if (temperature <= 0) {
            throw new IllegalArgumentException("temperature must be > 0");
        }
        HEAT_SOURCE_TEMPERATURE.put(block, Integer.valueOf(temperature));
    }

    public static int getHeatSourceTemperature(Block block) {
        Integer value = HEAT_SOURCE_TEMPERATURE.get(block);
        return value == null ? 0 : value.intValue();
    }


    public static void registerReactorFuel(Item item, double fuelAmount, int temperature) {
        if (item == null) {
            throw new NullPointerException("item");
        }
        REACTOR_FUELS.put(item, new ReactorFuelSpec(fuelAmount, temperature));
    }

    public static ReactorFuelSpec getReactorFuel(Item item) {
        return item == null ? null : REACTOR_FUELS.get(item);
    }

    public static void registerSolidCoolant(Item item, double amount, int temperature) {
        if (item == null) {
            throw new NullPointerException("item");
        }
        SOLID_COOLANTS.put(item, new SolidCoolantSpec(amount, temperature));
    }

    public static SolidCoolantSpec getSolidCoolant(Item item) {
        return item == null ? null : SOLID_COOLANTS.get(item);
    }

    public static Map<Fluid, Long> getMagmaticFluids() {
        return java.util.Collections.unmodifiableMap(MAGMATIC_ENERGY_PER_100_MB);
    }

    public static Map<Fluid, Integer> getFluidCoolants() {
        return java.util.Collections.unmodifiableMap(COOLANT_TEMPERATURE);
    }

    public static Map<Block, Integer> getHeatSources() {
        return java.util.Collections.unmodifiableMap(HEAT_SOURCE_TEMPERATURE);
    }

    public static Map<Item, ReactorFuelSpec> getReactorFuels() {
        return java.util.Collections.unmodifiableMap(REACTOR_FUELS);
    }

    public static Map<Item, SolidCoolantSpec> getSolidCoolants() {
        return java.util.Collections.unmodifiableMap(SOLID_COOLANTS);
    }

    /** Registers an Energizing recipe for integrations. */
    public static void registerEnergizingRecipe(EnergizingRecipe recipe) {
        EnergizingRecipeManager.instance().register(recipe);
    }

    public static void replaceEnergizingRecipe(EnergizingRecipe recipe) {
        EnergizingRecipeManager.instance().replace(recipe);
    }

    public static boolean removeEnergizingRecipe(ResourceLocation id) {
        return EnergizingRecipeManager.instance().remove(id);
    }

    /** Returns an immutable snapshot of registered Energizing recipes. */
    public static List<EnergizingRecipe> getEnergizingRecipes() {
        return EnergizingRecipeManager.instance().all();
    }
}
