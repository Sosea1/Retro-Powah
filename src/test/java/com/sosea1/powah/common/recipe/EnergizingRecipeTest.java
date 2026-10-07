package com.sosea1.powah.common.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

final class EnergizingRecipeTest {
    static { Bootstrap.register(); }

    @Test
    void repeatedIngredientsUseDistinctInputSlotsAndRejectExcessItems() {
        EnergizingRecipe recipe = new EnergizingRecipe(new ResourceLocation("powah:test"),
                Arrays.asList(EnergizingIngredient.item(Items.IRON_INGOT),
                        EnergizingIngredient.item(Items.IRON_INGOT), EnergizingIngredient.item(Items.GOLD_INGOT)),
                new ItemStack(Items.DIAMOND), 3_000_000_000L);
        ItemStackHandler inventory = new ItemStackHandler(7);
        inventory.setStackInSlot(1, new ItemStack(Items.GOLD_INGOT));
        inventory.setStackInSlot(4, new ItemStack(Items.IRON_INGOT));
        assertFalse(recipe.matches(inventory));
        inventory.setStackInSlot(6, new ItemStack(Items.IRON_INGOT));
        assertTrue(recipe.matches(inventory));
        inventory.setStackInSlot(6, new ItemStack(Items.IRON_INGOT, 2));
        assertFalse(recipe.matches(inventory));
        inventory.setStackInSlot(6, new ItemStack(Items.IRON_INGOT));
        inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND));
        assertFalse(recipe.matches(inventory));
        assertEquals(3_000_000_000L, recipe.energy());
        ItemStack output = recipe.output();
        output.setCount(64);
        assertEquals(1, recipe.output().getCount());
    }
}
