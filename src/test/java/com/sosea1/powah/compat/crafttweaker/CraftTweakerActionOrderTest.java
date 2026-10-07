package com.sosea1.powah.compat.crafttweaker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sosea1.powah.api.PowahApi;
import com.sosea1.powah.common.recipe.EnergizingIngredient;
import com.sosea1.powah.common.recipe.EnergizingRecipe;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderState;
import org.junit.jupiter.api.Test;

final class CraftTweakerActionOrderTest {
    static { Bootstrap.register(); }

    @Test
    void earlyClearRunsAfterDefaultsAndBeforeTheFollowingAddition() throws Exception {
        try (RealCraftTweakerActions actions = new RealCraftTweakerActions()) {
            PowahApi.registerEnergizingRecipe(recipe("preexisting", 1L));
            PowahCraftTweaker.clear();
            add(recipe("script_result", 99L));
            assertEquals(Collections.singletonList("powah:preexisting"), recipeIds());

            PowahApi.registerEnergizingRecipe(recipe("default_registered_later", 1L));
            actions.applyLateActions();
            assertEquals(Collections.singletonList("powah:script_result"), recipeIds());
            assertEquals(99L, PowahApi.getEnergizingRecipes().get(0).energy());
        }
    }

    @Test
    void earlyRemovalAndReplacementRespectScriptOrderAfterDefaults() throws Exception {
        try (RealCraftTweakerActions actions = new RealCraftTweakerActions()) {
            PowahCraftTweaker.remove("removed_default");
            add(recipe("replaced_default", 99L));
            add(recipe("added_then_removed", 123L));
            PowahCraftTweaker.remove("added_then_removed");
            assertTrue(PowahApi.getEnergizingRecipes().isEmpty());

            PowahApi.registerEnergizingRecipe(recipe("removed_default", 1L));
            PowahApi.registerEnergizingRecipe(recipe("replaced_default", 1L));
            PowahApi.registerEnergizingRecipe(recipe("untouched_default", 1L));
            actions.applyLateActions();
            assertEquals(java.util.Arrays.asList("powah:replaced_default", "powah:untouched_default"), recipeIds());
            assertEquals(99L, PowahApi.getEnergizingRecipes().get(0).energy());
        }
    }

    @Test
    void actionsAfterPostinitApplyImmediatelyInsteadOfWaitingForAnotherStartup() throws Exception {
        try (RealCraftTweakerActions actions = new RealCraftTweakerActions()) {
            actions.setPhase(LoaderState.POSTINITIALIZATION);
            EnergizingRecipe immediate = recipe("immediate", 99L);
            CraftTweakerSupport.apply("Adding test Powah recipe", () -> PowahApi.replaceEnergizingRecipe(immediate));
            assertEquals(Collections.singletonList("powah:immediate"), recipeIds());
            assertTrue(actions.pending.isEmpty());
        }
    }

    private static void add(EnergizingRecipe recipe) throws Exception {
        // Exercise Energizing's real AddAction without the published jar's SRG-only item converter.
        Method method = PowahCraftTweaker.class.getDeclaredMethod("queueAdd",
                String.class, ItemStack.class, long.class, List.class);
        method.setAccessible(true);
        method.invoke(null, recipe.id().toString(), recipe.output(), recipe.energy(), recipe.ingredients());
    }

    private static EnergizingRecipe recipe(String name, long energy) {
        return new EnergizingRecipe(new ResourceLocation("powah", name),
                Collections.singletonList(EnergizingIngredient.item(Items.IRON_INGOT)),
                new ItemStack(Items.GOLD_INGOT), energy);
    }

    private static List<String> recipeIds() {
        List<String> result = new ArrayList<String>();
        for (EnergizingRecipe recipe : PowahApi.getEnergizingRecipes()) result.add(recipe.id().toString());
        return result;
    }

    /** Controls only Forge's lifecycle phase; the CT action objects, queue and application are real. */
    private static final class RealCraftTweakerActions implements AutoCloseable {
        private final Loader loader = Loader.instance();
        private final Field controllerField = field(Loader.class, "modController");
        private final Object previousController = controllerField.get(loader);
        private final LoadController controller = new LoadController(loader);
        private final List<EnergizingRecipe> previousRecipes = PowahApi.getEnergizingRecipes();
        private final List<Object> previousPending;
        private final List<Object> pending;
        private final Method apply;

        @SuppressWarnings("unchecked")
        RealCraftTweakerActions() throws Exception {
            controllerField.set(loader, controller);
            setPhase(LoaderState.PREINITIALIZATION);
            try {
                // Reflection keeps CT compileOnly: it is required only on this test's runtime path.
                Class<?> ct = Class.forName("crafttweaker.mc1120.CraftTweaker");
                pending = (List<Object>) ct.getField("LATE_ACTIONS").get(null);
                previousPending = new ArrayList<Object>(pending);
                apply = Class.forName("crafttweaker.CraftTweakerAPI").getMethod("apply",
                        Class.forName("crafttweaker.IAction"));
                pending.clear();
                PowahApi.clearEnergizingRecipes();
            } catch (Throwable failure) {
                controllerField.set(loader, previousController);
                throw failure;
            }
        }

        void setPhase(LoaderState phase) throws Exception {
            field(LoadController.class, "state").set(controller, phase);
        }

        void applyLateActions() throws Exception {
            setPhase(LoaderState.POSTINITIALIZATION);
            for (Object action : new ArrayList<Object>(pending)) apply.invoke(null, action);
            pending.clear();
        }

        @Override
        public void close() throws Exception {
            pending.clear();
            pending.addAll(previousPending);
            PowahApi.clearEnergizingRecipes();
            for (EnergizingRecipe recipe : previousRecipes) PowahApi.registerEnergizingRecipe(recipe);
            controllerField.set(loader, previousController);
        }
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field result = owner.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }
}
