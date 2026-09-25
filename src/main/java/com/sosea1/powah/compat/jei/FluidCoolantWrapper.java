package com.sosea1.powah.compat.jei;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public final class FluidCoolantWrapper implements IRecipeWrapper {
    private final FluidStack fluid;
    private final int temperature;

    public FluidCoolantWrapper(Fluid fluid, int temperature) {
        this.fluid = new FluidStack(fluid, 1000);
        this.temperature = temperature;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.FLUID, fluid);
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        String text = I18n.format("info.powah.temperature", temperature + "°C");
        minecraft.fontRenderer.drawString(text, 30, 9, 0x0055aa);
    }
}
