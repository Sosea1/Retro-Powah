package com.sosea1.powah.common.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/** Small immutable ingredient abstraction for 1.12 Energizing recipes. */
public abstract class EnergizingIngredient {
    public abstract boolean matches(ItemStack stack);

    /** Representative stacks for JEI/integration code. */
    public abstract List<ItemStack> examples();

    public static EnergizingIngredient item(final Item item) {
        if (item == null) {
            throw new NullPointerException("item");
        }
        return new EnergizingIngredient() {
            @Override
            public boolean matches(ItemStack stack) {
                return stack != null && !stack.isEmpty() && stack.getItem() == item;
            }

            @Override
            public List<ItemStack> examples() {
                return Collections.singletonList(new ItemStack(item));
            }
        };
    }

    public static EnergizingIngredient stack(final ItemStack required) {
        if (required == null || required.isEmpty()) {
            throw new IllegalArgumentException("required stack must be non-empty");
        }
        final ItemStack copy = required.copy();
        copy.setCount(1);
        return new EnergizingIngredient() {
            @Override
            public boolean matches(ItemStack stack) {
                return stack != null && !stack.isEmpty()
                        && OreDictionary.itemMatches(copy, stack, false)
                        && ItemStack.areItemStackTagsEqual(copy, stack);
            }

            @Override
            public List<ItemStack> examples() {
                return Collections.singletonList(copy.copy());
            }
        };
    }

    public static EnergizingIngredient ore(final String oreName) {
        if (oreName == null || oreName.isEmpty()) {
            throw new IllegalArgumentException("oreName");
        }
        return new EnergizingIngredient() {
            @Override
            public boolean matches(ItemStack stack) {
                if (stack == null || stack.isEmpty()) {
                    return false;
                }
                if (!OreDictionary.doesOreNameExist(oreName)) {
                    return false;
                }
                int expected = OreDictionary.getOreID(oreName);
                for (int id : OreDictionary.getOreIDs(stack)) {
                    if (id == expected) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public List<ItemStack> examples() {
                List<ItemStack> ores = OreDictionary.getOres(oreName, false);
                List<ItemStack> copy = new ArrayList<ItemStack>(ores.size());
                for (ItemStack stack : ores) {
                    if (stack != null && !stack.isEmpty()) {
                        ItemStack example = stack.copy();
                        example.setCount(1);
                        copy.add(example);
                    }
                }
                return Collections.unmodifiableList(copy);
            }
        };
    }
}
