package com.sosea1.powah.compat.crafttweaker;

import com.sosea1.powah.api.PowahApi;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.liquid.ILiquidStack;
import net.minecraftforge.fluids.Fluid;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.powah.Coolant")
public final class CoolantTweaker {
    private CoolantTweaker() {}

    @ZenMethod
    public static void add(ILiquidStack input, int temperature) {
        final Fluid fluid = CraftTweakerSupport.fluid(input);
        CraftTweakerSupport.apply("Adding/replacing Powah fluid coolant " + fluid.getName(),
                () -> PowahApi.registerCoolant(fluid, temperature));
    }

    @ZenMethod
    public static void remove(ILiquidStack input) {
        final Fluid fluid = CraftTweakerSupport.fluid(input);
        CraftTweakerSupport.apply("Removing Powah fluid coolant " + fluid.getName(),
                () -> PowahApi.removeCoolant(fluid));
    }

    @ZenMethod
    public static void clear() {
        CraftTweakerSupport.apply("Clearing all Powah fluid coolants", PowahApi::clearCoolants);
    }
}
