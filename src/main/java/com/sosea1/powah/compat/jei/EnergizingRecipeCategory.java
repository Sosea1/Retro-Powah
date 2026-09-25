package com.sosea1.powah.compat.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import com.sosea1.powah.Powah;

/** JEI 4 recreation of Powah's original horizontal Energizing recipe diagram. */
public final class EnergizingRecipeCategory implements IRecipeCategory<EnergizingRecipeWrapper> {
    public static final String UID = Powah.MOD_ID + ".energizing";
    private static final ResourceLocation BACKGROUND =
            new ResourceLocation(Powah.MOD_ID, "textures/gui/jei/energizing.png");
    private final IDrawable background;

    public EnergizingRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(BACKGROUND, 0, 0, 160, 38);
    }

    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return I18n.format("gui.powah.jei.category.energizing"); }
    @Override public String getModName() { return Powah.MOD_NAME; }
    @Override public IDrawable getBackground() { return background; }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, EnergizingRecipeWrapper wrapper, IIngredients ingredients) {
        IGuiItemStackGroup stacks = recipeLayout.getItemStacks();
        for (int i = 0; i < 6; i++) {
            stacks.init(i, true, i * 20 + 3, 4);
        }
        stacks.init(6, false, 136, 4);
        stacks.set(ingredients);
    }
}
