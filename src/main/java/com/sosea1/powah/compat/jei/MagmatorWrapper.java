package com.sosea1.powah.compat.jei;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public final class MagmatorWrapper implements IRecipeWrapper {
    private final FluidStack fluid;
    private final long energyPer100Mb;

    public MagmatorWrapper(Fluid fluid, long energyPer100Mb) {
        this.fluid = new FluidStack(fluid, 1000);
        this.energyPer100Mb = energyPer100Mb;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.FLUID, fluid);
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        long totalEnergy = energyPer100Mb * 10L;
        String text = String.format(java.util.Locale.ROOT, "%,d FE / 1,000 mB", totalEnergy);
        minecraft.fontRenderer.drawString(text, 30, 9, 0x444444);
    }
}
