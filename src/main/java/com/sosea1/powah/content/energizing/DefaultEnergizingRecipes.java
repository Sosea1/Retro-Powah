package com.sosea1.powah.content.energizing;

import java.util.Arrays;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.recipe.EnergizingIngredient;
import com.sosea1.powah.common.recipe.EnergizingRecipe;
import com.sosea1.powah.common.recipe.EnergizingRecipeManager;
import com.sosea1.powah.registry.ModContent;

/** Baseline Powah energizing progression, retaining the classic Powah recipe values. */
public final class DefaultEnergizingRecipes {
    private static boolean registered;

    private DefaultEnergizingRecipes() {
    }

    public static synchronized void registerAll() {
        if (registered) {
            return;
        }
        EnergizingRecipeManager recipes = EnergizingRecipeManager.instance();
        recipes.register(recipe("energized_steel", 10_000L, new ItemStack(ModContent.energizedSteel(), 2),
                EnergizingIngredient.ore("ingotIron"), EnergizingIngredient.ore("ingotGold")));
        recipes.register(recipe("blazing_crystal_from_rod", 120_000L, new ItemStack(ModContent.blazingCrystal()),
                EnergizingIngredient.item(Items.BLAZE_ROD)));
        recipes.register(recipe("blazing_crystal_from_powder", 120_000L, new ItemStack(ModContent.blazingCrystal()),
                EnergizingIngredient.item(Items.BLAZE_POWDER), EnergizingIngredient.item(Items.BLAZE_POWDER),
                EnergizingIngredient.item(Items.BLAZE_POWDER), EnergizingIngredient.item(Items.BLAZE_POWDER)));
        recipes.register(recipe("niotic_crystal", 300_000L, new ItemStack(ModContent.nioticCrystal()),
                EnergizingIngredient.ore("gemDiamond")));
        recipes.register(recipe("spirited_crystal", 1_000_000L, new ItemStack(ModContent.spiritedCrystal()),
                EnergizingIngredient.ore("gemEmerald")));
        recipes.register(recipe("ender_core", 50_000L, new ItemStack(ModContent.enderCore()),
                EnergizingIngredient.item(Items.ENDER_EYE), EnergizingIngredient.item(ModContent.dielectricCasing()),
                EnergizingIngredient.item(ModContent.capacitorBasicTiny())));
        recipes.register(recipe("charged_snowball", 500_000L, new ItemStack(ModContent.chargedSnowball()),
                EnergizingIngredient.item(Items.SNOWBALL)));
        recipes.register(recipe("dry_ice", 10_000L, new ItemStack(ModContent.dryIce()),
                EnergizingIngredient.item(Item.getItemFromBlock(Blocks.PACKED_ICE)), EnergizingIngredient.item(Item.getItemFromBlock(Blocks.PACKED_ICE))));
        recipes.register(recipe("nitro_crystal", 20_000_000L, new ItemStack(ModContent.nitroCrystal(), 16),
                EnergizingIngredient.item(Items.NETHER_STAR),
                EnergizingIngredient.item(Item.getItemFromBlock(Blocks.REDSTONE_BLOCK)),
                EnergizingIngredient.item(Item.getItemFromBlock(Blocks.REDSTONE_BLOCK)),
                EnergizingIngredient.item(Item.getItemFromBlock(ModContent.blazingCrystalBlock()))));
        recipes.register(recipe("uraninite_from_raw", 2_000L, new ItemStack(ModContent.uraninite(), 2),
                EnergizingIngredient.item(ModContent.uraniniteRaw())));
        recipes.register(recipe("uraninite_from_ore_poor", 25_000L, new ItemStack(ModContent.uraninite(), 3),
                EnergizingIngredient.item(Item.getItemFromBlock(ModContent.uraniniteOrePoor()))));
        recipes.register(recipe("uraninite_from_ore", 50_000L, new ItemStack(ModContent.uraninite(), 5),
                EnergizingIngredient.item(Item.getItemFromBlock(ModContent.uraniniteOre()))));
        recipes.register(recipe("uraninite_from_ore_dense", 100_000L, new ItemStack(ModContent.uraninite(), 10),
                EnergizingIngredient.item(Item.getItemFromBlock(ModContent.uraniniteOreDense()))));
        recipes.register(recipe("uraninite_from_uranium", 30_000L, new ItemStack(ModContent.uraninite(), 1),
                EnergizingIngredient.ore("ingotUranium")));
        registered = true;
    }

    private static EnergizingRecipe recipe(String name, long energy, ItemStack output, EnergizingIngredient... ingredients) {
        return new EnergizingRecipe(new ResourceLocation(Powah.MOD_ID, name), Arrays.asList(ingredients), output, energy);
    }
}
