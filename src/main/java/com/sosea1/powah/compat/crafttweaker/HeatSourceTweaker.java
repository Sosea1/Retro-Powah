package com.sosea1.powah.compat.crafttweaker;

import com.sosea1.powah.api.PowahApi;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import net.minecraft.block.Block;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.powah.HeatSource")
public final class HeatSourceTweaker {
    private HeatSourceTweaker() {}

    @ZenMethod
    public static void add(IItemStack input, int temperature) {
        addBlock(CraftTweakerSupport.block(input), temperature);
    }

    /** Registry-name overload also supports lava and other blocks without an item. */
    @ZenMethod
    public static void add(String blockName, int temperature) {
        addBlock(CraftTweakerSupport.block(blockName), temperature);
    }

    @ZenMethod
    public static void remove(IItemStack input) {
        removeBlock(CraftTweakerSupport.block(input));
    }

    @ZenMethod
    public static void remove(String blockName) {
        removeBlock(CraftTweakerSupport.block(blockName));
    }

    @ZenMethod
    public static void clear() {
        CraftTweakerSupport.apply("Clearing all Powah heat sources", PowahApi::clearHeatSources);
    }

    private static void addBlock(final Block block, final int temperature) {
        ScriptValidation.heatTemperature(temperature);
        CraftTweakerSupport.apply("Adding/replacing Powah heat source " + block.getRegistryName(),
                () -> PowahApi.registerHeatSource(block, temperature));
    }

    private static void removeBlock(final Block block) {
        CraftTweakerSupport.apply("Removing Powah heat source " + block.getRegistryName(),
                () -> PowahApi.removeHeatSource(block));
    }
}
