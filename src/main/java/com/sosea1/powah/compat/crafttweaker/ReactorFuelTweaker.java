package com.sosea1.powah.compat.crafttweaker;

import com.sosea1.powah.api.PowahApi;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import net.minecraft.item.Item;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/** Registrations apply to the item type, including all metadata and NBT variants. */
@ZenRegister
@ZenClass("mods.powah.ReactorFuel")
public final class ReactorFuelTweaker {
    private ReactorFuelTweaker() {}

    @ZenMethod
    public static void add(IItemStack input, double fuelAmount, int temperature) {
        final Item item = CraftTweakerSupport.stack(input, "Reactor fuel").getItem();
        ScriptValidation.positiveAmount(fuelAmount);
        ScriptValidation.fuelTemperature(temperature);
        CraftTweakerSupport.apply("Adding/replacing Powah reactor fuel " + item.getRegistryName(),
                () -> PowahApi.registerReactorFuel(item, fuelAmount, temperature));
    }

    @ZenMethod
    public static void remove(IItemStack input) {
        final Item item = CraftTweakerSupport.stack(input, "Reactor fuel").getItem();
        CraftTweakerSupport.apply("Removing Powah reactor fuel " + item.getRegistryName(),
                () -> PowahApi.removeReactorFuel(item));
    }

    @ZenMethod
    public static void clear() {
        CraftTweakerSupport.apply("Clearing all Powah reactor fuels", PowahApi::clearReactorFuels);
    }
}
