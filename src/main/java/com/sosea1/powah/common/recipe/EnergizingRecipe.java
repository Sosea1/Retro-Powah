package com.sosea1.powah.common.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;
import com.sosea1.powah.Powah;

/** Unordered exact-count recipe used by the Energizing Orb. */
public final class EnergizingRecipe {
    private final ResourceLocation id;
    private final List<EnergizingIngredient> ingredients;
    private final ItemStack output;
    private final long energy;

    public EnergizingRecipe(ResourceLocation id, List<EnergizingIngredient> ingredients, ItemStack output, long energy) {
        if (id == null) throw new NullPointerException("id");
        if (ingredients == null || ingredients.isEmpty() || ingredients.size() > 6) {
            throw new IllegalArgumentException("Energizing recipes require 1..6 ingredients");
        }
        if (output == null || output.isEmpty()) throw new IllegalArgumentException("output");
        if (energy <= 0L) throw new IllegalArgumentException("energy must be > 0");
        this.id = id;
        this.ingredients = Collections.unmodifiableList(new ArrayList<EnergizingIngredient>(ingredients));
        this.output = output.copy();
        this.energy = energy;
    }

    public ResourceLocation id() { return id; }
    public List<EnergizingIngredient> ingredients() { return ingredients; }
    public ItemStack output() { return output.copy(); }
    public long energy() { return energy; }

    public long scaledEnergy() {
        double scaled = energy * Powah.energyConfig().energizingRatio();
        if (scaled >= Long.MAX_VALUE) return Long.MAX_VALUE;
        return Math.max(1L, (long) scaled);
    }

    /** Slot 0 is output; slots 1..6 are one-item inputs. */
    public boolean matches(IItemHandler inventory) {
        if (inventory == null || inventory.getSlots() < 7 || !inventory.getStackInSlot(0).isEmpty()) {
            return false;
        }
        List<ItemStack> inputs = new ArrayList<ItemStack>(6);
        for (int slot = 1; slot < 7; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                if (stack.getCount() != 1) {
                    return false;
                }
                inputs.add(stack);
            }
        }
        if (inputs.size() != ingredients.size()) {
            return false;
        }
        return matchInput(0, inputs, new boolean[ingredients.size()]);
    }

    public boolean matchesPartial(List<ItemStack> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return true;
        }
        if (inputs.size() > ingredients.size()) {
            return false;
        }
        for (ItemStack stack : inputs) {
            if (stack == null || stack.isEmpty() || stack.getCount() != 1) {
                return false;
            }
        }
        return matchInput(0, inputs, new boolean[ingredients.size()]);
    }

    private boolean matchInput(int inputIndex, List<ItemStack> inputs, boolean[] used) {
        if (inputIndex >= inputs.size()) {
            return true;
        }
        ItemStack input = inputs.get(inputIndex);
        for (int i = 0; i < ingredients.size(); i++) {
            if (!used[i] && ingredients.get(i).matches(input)) {
                used[i] = true;
                if (matchInput(inputIndex + 1, inputs, used)) {
                    return true;
                }
                used[i] = false;
            }
        }
        return false;
    }
}
