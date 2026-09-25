package com.sosea1.powah.compat.jei;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import com.sosea1.powah.common.reactor.ReactorFuelSpec;

public final class ReactorFuelWrapper implements IRecipeWrapper {
    private final ItemStack stack;
    private final ReactorFuelSpec spec;

    public ReactorFuelWrapper(Item item, ReactorFuelSpec spec) {
        this.stack = new ItemStack(item);
        this.spec = spec;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.ITEM, stack);
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        String fuelText = (long) spec.fuelAmount() + " fuel";
        String tempText = spec.temperature() + "°C";
        minecraft.fontRenderer.drawString(fuelText + " (" + tempText + ")", 30, 9, 0x44aa44);
    }
}
