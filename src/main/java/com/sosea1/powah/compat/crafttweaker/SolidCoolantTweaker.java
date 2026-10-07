package com.sosea1.powah.compat.crafttweaker;

import com.sosea1.powah.api.PowahApi;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import net.minecraft.item.Item;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/** Registrations apply to the item type, including all metadata and NBT variants. */
@ZenRegister
@ZenClass("mods.powah.SolidCoolant")
public final class SolidCoolantTweaker {
    private SolidCoolantTweaker() {}

    @ZenMethod
    public static void add(IItemStack input, double amount, int temperature) {
        final Item item = CraftTweakerSupport.stack(input, "Solid coolant").getItem();
        ScriptValidation.positiveAmount(amount);
        ScriptValidation.solidCoolantTemperature(temperature);
        CraftTweakerSupport.apply("Adding/replacing Powah solid coolant " + item.getRegistryName(),
                () -> PowahApi.registerSolidCoolant(item, amount, temperature));
    }

    @ZenMethod
    public static void remove(IItemStack input) {
        final Item item = CraftTweakerSupport.stack(input, "Solid coolant").getItem();
        CraftTweakerSupport.apply("Removing Powah solid coolant " + item.getRegistryName(),
                () -> PowahApi.removeSolidCoolant(item));
    }

    @ZenMethod
    public static void clear() {
        CraftTweakerSupport.apply("Clearing all Powah solid coolants", PowahApi::clearSolidCoolants);
    }
}
