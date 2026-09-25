package com.sosea1.powah.compat.jei;

import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import com.sosea1.powah.common.recipe.EnergizingIngredient;
import com.sosea1.powah.common.recipe.EnergizingRecipe;

/** JEI 4 wrapper for the unordered six-slot Energizing recipe model. */
public final class EnergizingRecipeWrapper implements IRecipeWrapper {
    private final EnergizingRecipe recipe;

    public EnergizingRecipeWrapper(EnergizingRecipe recipe) {
        if (recipe == null) throw new NullPointerException("recipe");
        this.recipe = recipe;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        List<List<ItemStack>> inputs = new ArrayList<List<ItemStack>>(recipe.ingredients().size());
        for (EnergizingIngredient ingredient : recipe.ingredients()) {
            inputs.add(ingredient.examples());
        }
        ingredients.setInputLists(VanillaTypes.ITEM, inputs);
        ingredients.setOutput(VanillaTypes.ITEM, recipe.output());
    }

    public long energy() {
        return recipe.scaledEnergy();
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        minecraft.fontRenderer.drawString(
                I18n.format("gui.powah.jei.energy", formatEnergy(recipe.scaledEnergy())), 2, 29, 0x444444);
    }

    private static String formatEnergy(long energy) {
        return String.format(java.util.Locale.ROOT, "%,d", Math.max(0L, energy));
    }
}
