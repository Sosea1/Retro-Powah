package com.sosea1.powah.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import com.sosea1.powah.Powah;
import com.sosea1.powah.api.PowahApi;
import com.sosea1.powah.common.reactor.ReactorFuelSpec;
import com.sosea1.powah.common.reactor.SolidCoolantSpec;
import com.sosea1.powah.common.recipe.EnergizingRecipe;
import com.sosea1.powah.common.tier.PowahTier;
import com.sosea1.powah.registry.ModContent;

@JEIPlugin
public final class PowahJeiPlugin implements IModPlugin {
    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        mezz.jei.api.IGuiHelper helper = registry.getJeiHelpers().getGuiHelper();
        registry.addRecipeCategories(
                new EnergizingRecipeCategory(helper),
                new HeatSourceCategory(helper),
                new MagmatorCategory(helper),
                new FluidCoolantCategory(helper),
                new SolidCoolantCategory(helper),
                new ReactorFuelCategory(helper)
        );
    }

    @Override
    public void register(IModRegistry registry) {
        List<EnergizingRecipeWrapper> energizingWrappers = new ArrayList<EnergizingRecipeWrapper>();
        for (EnergizingRecipe recipe : PowahApi.getEnergizingRecipes()) {
            energizingWrappers.add(new EnergizingRecipeWrapper(recipe));
        }
        registry.addRecipes(energizingWrappers, EnergizingRecipeCategory.UID);
        registry.addRecipeCatalyst(new ItemStack(ModContent.energizingOrb()), EnergizingRecipeCategory.UID);
        for (PowahTier tier : PowahTier.normalValues()) {
            registry.addRecipeCatalyst(new ItemStack(ModContent.energizingRod(tier)), EnergizingRecipeCategory.UID);
        }

        List<HeatSourceWrapper> heatWrappers = new ArrayList<HeatSourceWrapper>();
        for (Map.Entry<Block, Integer> entry : PowahApi.getHeatSources().entrySet()) {
            heatWrappers.add(new HeatSourceWrapper(entry.getKey(), entry.getValue().intValue()));
        }
        registry.addRecipes(heatWrappers, HeatSourceCategory.UID);
        for (PowahTier tier : PowahTier.normalValues()) {
            registry.addRecipeCatalyst(new ItemStack(ModContent.thermoGenerator(tier)), HeatSourceCategory.UID);
        }

        List<MagmatorWrapper> magmatorWrappers = new ArrayList<MagmatorWrapper>();
        for (Map.Entry<Fluid, Long> entry : PowahApi.getMagmaticFluids().entrySet()) {
            magmatorWrappers.add(new MagmatorWrapper(entry.getKey(), entry.getValue().longValue()));
        }
        registry.addRecipes(magmatorWrappers, MagmatorCategory.UID);
        for (PowahTier tier : PowahTier.normalValues()) {
            registry.addRecipeCatalyst(new ItemStack(ModContent.magmator(tier)), MagmatorCategory.UID);
        }

        List<FluidCoolantWrapper> fluidCoolantWrappers = new ArrayList<FluidCoolantWrapper>();
        for (Map.Entry<Fluid, Integer> entry : PowahApi.getFluidCoolants().entrySet()) {
            fluidCoolantWrappers.add(new FluidCoolantWrapper(entry.getKey(), entry.getValue().intValue()));
        }
        registry.addRecipes(fluidCoolantWrappers, FluidCoolantCategory.UID);
        for (PowahTier tier : PowahTier.normalValues()) {
            registry.addRecipeCatalyst(new ItemStack(ModContent.thermoGenerator(tier)), FluidCoolantCategory.UID);
            registry.addRecipeCatalyst(new ItemStack(ModContent.reactor(tier)), FluidCoolantCategory.UID);
        }

        List<SolidCoolantWrapper> solidCoolantWrappers = new ArrayList<SolidCoolantWrapper>();
        for (Map.Entry<Item, SolidCoolantSpec> entry : PowahApi.getSolidCoolants().entrySet()) {
            solidCoolantWrappers.add(new SolidCoolantWrapper(entry.getKey(), entry.getValue()));
        }
        registry.addRecipes(solidCoolantWrappers, SolidCoolantCategory.UID);
        for (PowahTier tier : PowahTier.normalValues()) {
            registry.addRecipeCatalyst(new ItemStack(ModContent.reactor(tier)), SolidCoolantCategory.UID);
        }

        List<ReactorFuelWrapper> reactorFuelWrappers = new ArrayList<ReactorFuelWrapper>();
        for (Map.Entry<Item, ReactorFuelSpec> entry : PowahApi.getReactorFuels().entrySet()) {
            reactorFuelWrappers.add(new ReactorFuelWrapper(entry.getKey(), entry.getValue()));
        }
        registry.addRecipes(reactorFuelWrappers, ReactorFuelCategory.UID);
        for (PowahTier tier : PowahTier.normalValues()) {
            registry.addRecipeCatalyst(new ItemStack(ModContent.reactor(tier)), ReactorFuelCategory.UID);
        }

        registry.addIngredientInfo(new ItemStack(ModContent.playerAerialPearl()), mezz.jei.api.ingredients.VanillaTypes.ITEM, "jei.powah.player_aerial_pearl");
        registry.addIngredientInfo(new ItemStack(ModContent.dimensionalBindingCard()), mezz.jei.api.ingredients.VanillaTypes.ITEM, "jei.powah.binding_card_dim");
        registry.addIngredientInfo(new ItemStack(ModContent.lensOfEnder()), mezz.jei.api.ingredients.VanillaTypes.ITEM, "jei.powah.lens_of_ender");
    }
}
