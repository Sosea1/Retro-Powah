package com.sosea1.powah.content.reactor;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraft.tileentity.TileEntity;

/** Reactor item consumes all 36 shell blocks atomically, matching modern Powah's footprint. */
public final class ItemReactorBlock extends ItemBlock {
    public static final int STRUCTURE_BLOCKS = 36;

    public ItemReactorBlock(BlockReactor block) {
        super(block);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (player instanceof FakePlayer) {
            return EnumActionResult.FAIL;
        }

        ItemStack held = player.getHeldItem(hand);
        Item reactorItem = held.getItem();
        if (!player.capabilities.isCreativeMode && countAvailable(player, reactorItem) < STRUCTURE_BLOCKS) {
            return EnumActionResult.FAIL;
        }

        BlockPos corePos = placementPosition(world, pos, facing);
        if (!canFitStructure(world, corePos) || hasLivingEntityInStructure(world, corePos)) {
            return EnumActionResult.FAIL;
        }

        EnumActionResult result = super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
        if (result == EnumActionResult.SUCCESS && !world.isRemote) {
            TileEntity raw = world.getTileEntity(corePos);
            if (raw instanceof TileReactor) {
                ((TileReactor) raw).authorizeAssembly(!player.capabilities.isCreativeMode);
                if (!player.capabilities.isCreativeMode) {
                    // ItemBlock already consumed the core item. Consume the remaining shell blocks
                    // only after the expected Reactor TileEntity exists on the authoritative server.
                    consumeAdditional(player, reactorItem, STRUCTURE_BLOCKS - 1);
                }
            }
        }
        return result;
    }

    public static int countAvailable(EntityPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (!stack.isEmpty() && stack.getItem() == item) count += stack.getCount();
        }
        for (ItemStack stack : player.inventory.offHandInventory) {
            if (!stack.isEmpty() && stack.getItem() == item) count += stack.getCount();
        }
        return count;
    }

    private static void consumeAdditional(EntityPlayer player, Item item, int amount) {
        int remaining = amount;
        remaining = consumeFrom(player.inventory.mainInventory, item, remaining);
        consumeFrom(player.inventory.offHandInventory, item, remaining);
        player.inventory.markDirty();
    }

    private static int consumeFrom(java.util.List<ItemStack> stacks, Item item, int amount) {
        int remaining = amount;
        for (ItemStack stack : stacks) {
            if (remaining <= 0) break;
            if (stack.isEmpty() || stack.getItem() != item) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        return remaining;
    }

    public static BlockPos placementPosition(World world, BlockPos clicked, EnumFacing facing) {
        IBlockState state = world.getBlockState(clicked);
        return state.getBlock().isReplaceable(world, clicked) ? clicked : clicked.offset(facing);
    }


    public static boolean hasLivingEntityInStructure(World world, BlockPos core) {
        AxisAlignedBB structureBounds = new AxisAlignedBB(
                core.getX() - 1.0D, core.getY(), core.getZ() - 1.0D,
                core.getX() + 2.0D, core.getY() + 4.0D, core.getZ() + 2.0D);
        return !world.getEntitiesWithinAABB(EntityLivingBase.class, structureBounds).isEmpty();
    }

    public static boolean canFitStructure(World world, BlockPos core) {
        for (int y = 0; y < 4; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos target = core.add(x, y, z);
                    if (!world.isBlockLoaded(target)
                            || !world.getBlockState(target).getBlock().isReplaceable(world, target)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
