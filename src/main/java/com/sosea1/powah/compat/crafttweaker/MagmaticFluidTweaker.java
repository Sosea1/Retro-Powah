package com.sosea1.powah.compat.crafttweaker;

import com.sosea1.powah.api.PowahApi;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.liquid.ILiquidStack;
import net.minecraftforge.fluids.Fluid;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/** Fuel energy is measured per 100 mB, independently of the liquid stack's amount. */
@ZenRegister
@ZenClass("mods.powah.MagmaticFluid")
public final class MagmaticFluidTweaker {
    private MagmaticFluidTweaker() {}

    @ZenMethod
    public static void add(ILiquidStack input, long energyPer100Mb) {
        final Fluid fluid = CraftTweakerSupport.fluid(input);
        ScriptValidation.positiveEnergy(energyPer100Mb);
        CraftTweakerSupport.apply("Adding/replacing Powah magmatic fluid " + fluid.getName(),
                () -> PowahApi.registerMagmaticFluid(fluid, energyPer100Mb));
    }

    @ZenMethod
    public static void remove(ILiquidStack input) {
        final Fluid fluid = CraftTweakerSupport.fluid(input);
        CraftTweakerSupport.apply("Removing Powah magmatic fluid " + fluid.getName(),
                () -> PowahApi.removeMagmaticFluid(fluid));
    }

    @ZenMethod
    public static void clear() {
        CraftTweakerSupport.apply("Clearing all Powah magmatic fluids", PowahApi::clearMagmaticFluids);
    }
}
