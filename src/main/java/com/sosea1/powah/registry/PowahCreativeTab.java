package com.sosea1.powah.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.tier.PowahTier;

public final class PowahCreativeTab extends CreativeTabs {
    public static final PowahCreativeTab INSTANCE = new PowahCreativeTab();

    private PowahCreativeTab() {
        super(Powah.MOD_ID);
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(ModContent.dielectricCasing());
    }

    /**
     * Keeps every stack contributed to the tab (including the optional Patchouli manual), but
     * presents Powah content in stable family groups instead of tier-by-tier registration order.
     * Registration order itself must not be changed because legacy worlds may retain numeric IDs.
     */
    @Override
    public void displayAllRelevantItems(NonNullList<ItemStack> stacks) {
        super.displayAllRelevantItems(stacks);

        Item guideBook = Item.REGISTRY.getObject(new ResourceLocation("patchouli", "guide_book"));
        if (guideBook != null && guideBook != Items.AIR) {
            ItemStack manual = new ItemStack(guideBook);
            net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
            tag.setString("patchouli:book", "powah:manual");
            manual.setTagCompound(tag);
            boolean found = false;
            for (int i = 0; i < stacks.size(); i++) {
                if (stacks.get(i).getItem() == guideBook) {
                    stacks.set(i, manual);
                    found = true;
                    break;
                }
            }
            if (!found) {
                stacks.add(manual);
            }
        }

        List<Item> order = new ArrayList<Item>();
        if (guideBook != null && guideBook != Items.AIR) addItem(order, guideBook);
        addItem(order, ModContent.wrench());

        for (PowahTier tier : PowahTier.normalValues()) addBlock(order, ModContent.cable(tier));
        for (PowahTier tier : PowahTier.normalValues()) addBlock(order, ModContent.reactor(tier));
        for (PowahTier tier : PowahTier.normalValues()) addBlock(order, ModContent.energyCell(tier));
        addBlock(order, ModContent.creativeEnergyCell());
        for (PowahTier tier : PowahTier.normalValues()) addBlock(order, ModContent.enderCell(tier));
        for (PowahTier tier : PowahTier.normalValues()) addBlock(order, ModContent.enderGate(tier));
        for (PowahTier tier : PowahTier.normalValues()) addBlock(order, ModContent.playerTransmitter(tier));
        for (PowahTier tier : PowahTier.normalValues()) addBlock(order, ModContent.furnator(tier));
        for (PowahTier tier : PowahTier.normalValues()) addBlock(order, ModContent.magmator(tier));
        for (PowahTier tier : PowahTier.normalValues()) addBlock(order, ModContent.thermoGenerator(tier));
        for (PowahTier tier : PowahTier.normalValues()) addBlock(order, ModContent.solarPanel(tier));

        addBlock(order, ModContent.energizingOrb());
        for (PowahTier tier : PowahTier.normalValues()) addBlock(order, ModContent.energizingRod(tier));
        for (PowahTier tier : PowahTier.normalValues()) addBlock(order, ModContent.energyHopper(tier));
        for (PowahTier tier : PowahTier.normalValues()) addBlock(order, ModContent.energyDischarger(tier));
        for (PowahTier tier : PowahTier.normalValues()) addItem(order, ModContent.battery(tier));

        addItem(order, ModContent.capacitorBasicTiny());
        addItem(order, ModContent.capacitorBasic());
        addItem(order, ModContent.capacitorBasicLarge());
        addItem(order, ModContent.capacitorHardened());
        addItem(order, ModContent.capacitorBlazing());
        addItem(order, ModContent.capacitorNiotic());
        addItem(order, ModContent.capacitorSpirited());
        addItem(order, ModContent.capacitorNitro());

        addItem(order, ModContent.blankCard());
        addItem(order, ModContent.bindingCard());
        addItem(order, ModContent.dimensionalBindingCard());
        addItem(order, ModContent.aerialPearl());
        addItem(order, ModContent.playerAerialPearl());
        addItem(order, ModContent.lensOfEnder());
        addItem(order, ModContent.enderCore());
        addItem(order, ModContent.photoelectricPane());
        addItem(order, ModContent.thermoelectricPlate());
        addItem(order, ModContent.dielectricPaste());
        addItem(order, ModContent.dielectricRod());
        addItem(order, ModContent.dielectricRodHorizontal());
        addItem(order, ModContent.dielectricCasing());
        addItem(order, ModContent.energizedSteel());
        addItem(order, ModContent.blazingCrystal());
        addItem(order, ModContent.nioticCrystal());
        addItem(order, ModContent.spiritedCrystal());
        addItem(order, ModContent.nitroCrystal());
        addItem(order, ModContent.uraniniteRaw());
        addItem(order, ModContent.uraninite());
        addItem(order, ModContent.chargedSnowball());

        addBlock(order, ModContent.energizedSteelBlock());
        addBlock(order, ModContent.blazingCrystalBlock());
        addBlock(order, ModContent.nioticCrystalBlock());
        addBlock(order, ModContent.spiritedCrystalBlock());
        addBlock(order, ModContent.nitroCrystalBlock());
        addBlock(order, ModContent.uraniniteRawBlock());
        addBlock(order, ModContent.uraniniteBlock());
        addBlock(order, ModContent.uraniniteOrePoor());
        addBlock(order, ModContent.uraniniteOre());
        addBlock(order, ModContent.uraniniteOreDense());
        addBlock(order, ModContent.dryIce());

        final Map<Item, Integer> rank = new IdentityHashMap<Item, Integer>();
        for (int index = 0; index < order.size(); index++) {
            if (!rank.containsKey(order.get(index))) rank.put(order.get(index), index);
        }
        Collections.sort(stacks, new Comparator<ItemStack>() {
            @Override
            public int compare(ItemStack left, ItemStack right) {
                Integer leftRank = rank.get(left.getItem());
                Integer rightRank = rank.get(right.getItem());
                int a = leftRank == null ? Integer.MAX_VALUE : leftRank;
                int b = rightRank == null ? Integer.MAX_VALUE : rightRank;
                return Integer.compare(a, b);
            }
        });
    }

    private static void addBlock(List<Item> order, Block block) {
        if (block != null) addItem(order, Item.getItemFromBlock(block));
    }

    private static void addItem(List<Item> order, Item item) {
        if (item != null && item != Items.AIR) order.add(item);
    }

    private static void addOptionalItem(List<Item> order, ResourceLocation id) {
        addItem(order, Item.REGISTRY.getObject(id));
    }
}
