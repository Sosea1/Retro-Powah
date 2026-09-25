package com.sosea1.powah.compat.crafttweaker;

import java.util.ArrayList;
import java.util.List;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.recipe.EnergizingIngredient;
import com.sosea1.powah.common.recipe.EnergizingRecipe;
import com.sosea1.powah.common.recipe.EnergizingRecipeManager;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.powah.Energizing")
public final class PowahCraftTweaker {
    private PowahCraftTweaker() {}

    @ZenMethod
    public static void addExact(String name, IItemStack output, long energy, IItemStack... inputs) {
        List<EnergizingIngredient> ingredients = new ArrayList<EnergizingIngredient>();
        for (IItemStack input : inputs) {
            ingredients.add(EnergizingIngredient.stack(CraftTweakerMC.getItemStack(input)));
        }
        queueAdd(name, CraftTweakerMC.getItemStack(output), energy, ingredients);
    }

    @ZenMethod
    public static void addOre(String name, IItemStack output, long energy, String... oreNames) {
        List<EnergizingIngredient> ingredients = new ArrayList<EnergizingIngredient>();
        for (String ore : oreNames) {
            ingredients.add(EnergizingIngredient.ore(ore));
        }
        queueAdd(name, CraftTweakerMC.getItemStack(output), energy, ingredients);
    }

    @ZenMethod
    public static void remove(String name) {
        CraftTweakerAPI.apply(new RemoveAction(id(name)));
    }

    private static void queueAdd(String name, ItemStack output, long energy, List<EnergizingIngredient> ingredients) {
        CraftTweakerAPI.apply(new AddAction(new EnergizingRecipe(id(name), ingredients, output, energy)));
    }

    private static ResourceLocation id(String name) {
        return name.indexOf(':') >= 0 ? new ResourceLocation(name) : new ResourceLocation(Powah.MOD_ID, name);
    }

    private static final class AddAction implements IAction {
        private final EnergizingRecipe recipe;

        private AddAction(EnergizingRecipe recipe) {
            this.recipe = recipe;
        }

        @Override
        public void apply() {
            EnergizingRecipeManager.instance().replace(recipe);
        }

        @Override
        public String describe() {
            return "Adding/replacing Powah Energizing recipe " + recipe.id();
        }
    }

    private static final class RemoveAction implements IAction {
        private final ResourceLocation id;

        private RemoveAction(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public void apply() {
            EnergizingRecipeManager.instance().remove(id);
        }

        @Override
        public String describe() {
            return "Removing Powah Energizing recipe " + id;
        }
    }
}
