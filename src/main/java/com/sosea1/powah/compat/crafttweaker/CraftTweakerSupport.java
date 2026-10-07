package com.sosea1.powah.compat.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.liquid.ILiquidStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.mc1120.CraftTweaker;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderState;

/** Loaded only by CraftTweaker's optional ZenRegister integration. */
final class CraftTweakerSupport {
    private CraftTweakerSupport() {}

    static void submit(IAction action) {
        // The preinit and recipeevent loaders run before Powah.init registers defaults.
        // CraftTweaker processes this ordered list in postinit, after every mod's init.
        if (Loader.instance().hasReachedState(LoaderState.POSTINITIALIZATION)) {
            CraftTweakerAPI.apply(action);
        } else {
            CraftTweaker.LATE_ACTIONS.add(action);
        }
    }

    static void apply(final String description, final Runnable change) {
        submit(new IAction() {
            @Override
            public void apply() { change.run(); }

            @Override
            public String describe() { return description; }
        });
    }

    static ItemStack stack(IItemStack input, String argument) {
        if (input == null) throw new IllegalArgumentException(argument + " must be a non-empty item stack");
        ItemStack stack = CraftTweakerMC.getItemStack(input);
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException(argument + " must be a non-empty item stack");
        }
        return stack;
    }

    static Fluid fluid(ILiquidStack input) {
        if (input == null) throw new IllegalArgumentException("Fluid must be a non-empty liquid stack");
        FluidStack stack = CraftTweakerMC.getLiquidStack(input);
        if (stack == null || stack.getFluid() == null || stack.amount <= 0) {
            throw new IllegalArgumentException("Fluid must be a non-empty liquid stack");
        }
        return stack.getFluid();
    }

    static Block block(IItemStack input) {
        ItemStack stack = stack(input, "Heat source");
        if (!(stack.getItem() instanceof ItemBlock)) {
            throw new IllegalArgumentException("Heat source must be a block item; use its registry name for blocks without an item");
        }
        return ((ItemBlock) stack.getItem()).getBlock();
    }

    static Block block(String name) {
        ResourceLocation id = new ResourceLocation(ScriptValidation.resourceName(name, "minecraft"));
        if (!Block.REGISTRY.containsKey(id)) {
            throw new IllegalArgumentException("Unknown heat source block: " + id);
        }
        return Block.REGISTRY.getObject(id);
    }
}
