package com.sosea1.powah.common.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;

/** Deterministic insertion-ordered registry. CraftTweaker/JEI hooks can target this layer later. */
public final class EnergizingRecipeManager {
    private static final EnergizingRecipeManager INSTANCE = new EnergizingRecipeManager();
    private final Map<ResourceLocation, EnergizingRecipe> recipes = new LinkedHashMap<ResourceLocation, EnergizingRecipe>();

    private EnergizingRecipeManager() {
    }

    public static EnergizingRecipeManager instance() {
        return INSTANCE;
    }

    public synchronized void register(EnergizingRecipe recipe) {
        if (recipe == null) throw new NullPointerException("recipe");
        if (recipes.containsKey(recipe.id())) {
            throw new IllegalArgumentException("Duplicate energizing recipe: " + recipe.id());
        }
        recipes.put(recipe.id(), recipe);
    }

    public synchronized void replace(EnergizingRecipe recipe) {
        if (recipe == null) throw new NullPointerException("recipe");
        recipes.put(recipe.id(), recipe);
    }

    public synchronized boolean remove(ResourceLocation id) {
        return recipes.remove(id) != null;
    }

    public synchronized EnergizingRecipe find(IItemHandler inventory) {
        for (EnergizingRecipe recipe : recipes.values()) {
            if (recipe.matches(inventory)) {
                return recipe;
            }
        }
        return null;
    }

    public synchronized boolean hasPotentialRecipe(List<ItemStack> inputs) {
        if (inputs == null || inputs.isEmpty()) return true;
        for (EnergizingRecipe recipe : recipes.values()) {
            if (recipe.matchesPartial(inputs)) {
                return true;
            }
        }
        return false;
    }

    public synchronized List<EnergizingRecipe> all() {
        return Collections.unmodifiableList(new ArrayList<EnergizingRecipe>(recipes.values()));
    }

    public synchronized int size() {
        return recipes.size();
    }

    public synchronized void clearForTests() {
        recipes.clear();
    }
}
