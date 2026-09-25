package com.sosea1.powah.compat.jei;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class HeatSourceWrapper implements IRecipeWrapper {
    private final ItemStack stack;
    private final int temperature;

    public HeatSourceWrapper(Block block, int temperature) {
        if (block == Blocks.LAVA || block == Blocks.FLOWING_LAVA) {
            this.stack = new ItemStack(Items.LAVA_BUCKET);
        } else {
            Item item = Item.getItemFromBlock(block);
            this.stack = item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        }
        this.temperature = temperature;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        if (!stack.isEmpty()) {
            ingredients.setInput(VanillaTypes.ITEM, stack);
        }
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        String text = I18n.format("info.powah.temperature", temperature + "°C");
        minecraft.fontRenderer.drawString(text, 30, 9, 0xc43400);
    }
}
