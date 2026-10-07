package com.sosea1.powah.compat.crafttweaker;

import java.util.ArrayList;
import java.util.List;
import crafttweaker.IAction;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import com.sosea1.powah.Powah;
import com.sosea1.powah.api.PowahApi;
import com.sosea1.powah.common.recipe.EnergizingIngredient;
import com.sosea1.powah.common.recipe.EnergizingRecipe;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.powah.Energizing")
public final class PowahCraftTweaker {
    private PowahCraftTweaker() {}

    @ZenMethod
    public static void addExact(String name, IItemStack output, long energy, IItemStack... inputs) {
        ScriptValidation.ingredientCount(inputs == null ? 0 : inputs.length);
        List<EnergizingIngredient> ingredients = new ArrayList<EnergizingIngredient>();
        for (IItemStack input : inputs) {
            ItemStack stack = CraftTweakerSupport.stack(input, "Input");
            if (stack.getCount() != 1) {
                throw new IllegalArgumentException("Each Energizing input stack must contain exactly one item");
            }
            ingredients.add(EnergizingIngredient.stack(stack));
        }
        queueAdd(name, CraftTweakerSupport.stack(output, "Output"), energy, ingredients);
    }

    @ZenMethod
    public static void addOre(String name, IItemStack output, long energy, String... oreNames) {
        ScriptValidation.ingredientCount(oreNames == null ? 0 : oreNames.length);
        List<EnergizingIngredient> ingredients = new ArrayList<EnergizingIngredient>();
        for (String ore : oreNames) {
            ScriptValidation.oreName(ore);
            ingredients.add(EnergizingIngredient.ore(ore));
        }
        queueAdd(name, CraftTweakerSupport.stack(output, "Output"), energy, ingredients);
    }

    @ZenMethod
    public static void remove(String name) {
        CraftTweakerSupport.submit(new RemoveAction(id(name)));
    }

    @ZenMethod
    public static void clear() {
        CraftTweakerSupport.apply("Clearing all Powah Energizing recipes", PowahApi::clearEnergizingRecipes);
    }

    private static void queueAdd(String name, ItemStack output, long energy, List<EnergizingIngredient> ingredients) {
        ScriptValidation.positiveEnergy(energy);
        CraftTweakerSupport.submit(new AddAction(new EnergizingRecipe(id(name), ingredients, output, energy)));
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(ScriptValidation.resourceName(name, Powah.MOD_ID));
    }

    private static final class AddAction implements IAction {
        private final EnergizingRecipe recipe;

        private AddAction(EnergizingRecipe recipe) {
            this.recipe = recipe;
        }

        @Override
        public void apply() {
            PowahApi.replaceEnergizingRecipe(recipe);
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
            PowahApi.removeEnergizingRecipe(id);
        }

        @Override
        public String describe() {
            return "Removing Powah Energizing recipe " + id;
        }
    }
}
