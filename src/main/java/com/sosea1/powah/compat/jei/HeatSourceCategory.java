package com.sosea1.powah.compat.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.resources.I18n;
import com.sosea1.powah.Powah;

public final class HeatSourceCategory implements IRecipeCategory<HeatSourceWrapper> {
    public static final String UID = Powah.MOD_ID + ".heat_source";
    private final IDrawable background;

    public HeatSourceCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(160, 26);
    }

    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return I18n.format("gui.powah.jei.category.heat.sources"); }
    @Override public String getModName() { return Powah.MOD_NAME; }
    @Override public IDrawable getBackground() { return background; }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, HeatSourceWrapper wrapper, IIngredients ingredients) {
        IGuiItemStackGroup stacks = recipeLayout.getItemStacks();
        stacks.init(0, true, 4, 4);
        stacks.set(ingredients);
    }
}
