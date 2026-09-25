package com.sosea1.powah.compat.jei;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import com.sosea1.powah.common.reactor.SolidCoolantSpec;

public final class SolidCoolantWrapper implements IRecipeWrapper {
    private final ItemStack stack;
    private final SolidCoolantSpec spec;

    public SolidCoolantWrapper(Item item, SolidCoolantSpec spec) {
        this.stack = new ItemStack(item);
        this.spec = spec;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.ITEM, stack);
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        String tempText = spec.temperature() + "°C";
        String amountText = (long) spec.amount() + " cold";
        minecraft.fontRenderer.drawString(tempText + " (" + amountText + ")", 30, 9, 0x0055aa);
    }
}
