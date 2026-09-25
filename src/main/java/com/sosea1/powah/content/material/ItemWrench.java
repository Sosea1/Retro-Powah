package com.sosea1.powah.content.material;

import com.sosea1.powah.registry.PowahCreativeTab;

import java.util.List;
import java.util.Locale;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import com.sosea1.powah.common.block.BlockPowahMachine;
import com.sosea1.powah.common.block.BlockProtection;
import com.sosea1.powah.common.block.entity.AbstractEnergyTile;
import com.sosea1.powah.common.energy.EnergyPortMode;
import com.sosea1.powah.content.cable.BlockCable;
import com.sosea1.powah.content.cable.TileCable;
import com.sosea1.powah.content.energizing.BlockEnergizingOrb;
import com.sosea1.powah.content.energizing.TileEnergizingOrb;
import com.sosea1.powah.content.energizing.TileEnergizingRod;
import com.sosea1.powah.content.reactor.BlockReactorPart;
import com.sosea1.powah.content.reactor.TileReactor;
import com.sosea1.powah.content.reactor.TileReactorPart;
import com.sosea1.powah.content.transmitter.BlockPlayerTransmitter;

/**
 * Wrench with CONFIG, LINK, ROTATE, and shift-dismantle modes.
 * Dismantling uses the normal block drop path to preserve portable machine state.
 */
public final class ItemWrench extends Item {
    private static final String NBT_MODE = "PowahWrenchMode";
    private static final String NBT_LINK_TYPE = "PowahWrenchLinkType";
    private static final String NBT_LINK_X = "PowahWrenchLinkX";
    private static final String NBT_LINK_Y = "PowahWrenchLinkY";
    private static final String NBT_LINK_Z = "PowahWrenchLinkZ";
    private static final int LINK_NONE = 0;
    private static final int LINK_ROD = 1;
    private static final int LINK_ORB = 2;

    public enum Mode {
        CONFIG,
        LINK,
        ROTATE;

        private Mode next() {
            Mode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public ItemWrench() {
        setMaxStackSize(1);
        setCreativeTab(PowahCreativeTab.INSTANCE);
    }

    public Mode getMode(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) return Mode.CONFIG;
        int index = stack.getTagCompound().getByte(NBT_MODE) & 0xFF;
        Mode[] values = Mode.values();
        return values[index % values.length];
    }

    private void setMode(ItemStack stack, Mode mode) {
        NBTTagCompound tag = getOrCreateTag(stack);
        tag.setByte(NBT_MODE, (byte) mode.ordinal());
        if (mode != Mode.LINK) clearLink(tag);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!player.isSneaking()) return new ActionResult<ItemStack>(EnumActionResult.PASS, stack);
        if (!world.isRemote) {
            Mode next = getMode(stack).next();
            setMode(stack, next);
            player.sendStatusMessage(new TextComponentTranslation("info.powah.wrench.mode",
                    new TextComponentTranslation("info.powah.wrench.mode." + next.name().toLowerCase(Locale.ROOT))), true);
        }
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                                           float hitX, float hitY, float hitZ, EnumHand hand) {
        return handleBlockUse(player, world, pos, side, hand);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        return handleBlockUse(player, world, pos, facing, hand);
    }

    private EnumActionResult handleBlockUse(EntityPlayer player, World world, BlockPos pos, EnumFacing side, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!BlockProtection.canEdit(player, world, pos, side, stack)) return EnumActionResult.FAIL;
        if (player.isSneaking()) {
            return removeWithWrench(player, world, pos, side, stack) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
        }

        switch (getMode(stack)) {
            case CONFIG:
                TileEntity tile = resolveConfigTile(world, pos);
                return configure(player, world, tile, side) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
            case LINK:
                return link(player, world, pos, side, stack) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
            case ROTATE:
                return rotate(player, world, pos) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
            default:
                return EnumActionResult.PASS;
        }
    }

    public static boolean isWrench(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemWrench;
    }

    /** CONFIG mode: cable face or machine FE face cycles through the supported I/O states. */
    public static boolean configure(EntityPlayer player, World world, TileEntity tile, EnumFacing facing) {
        if (tile instanceof TileCable) {
            if (!world.isRemote) {
                TileCable cable = (TileCable) tile;
                EnergyPortMode next = cable.cycleSideMode(facing);
                player.sendStatusMessage(new TextComponentTranslation("chat.powah.wrench.config",
                        facing.getName(), next.name()), true);
            }
            return true;
        }
        if (!(tile instanceof AbstractEnergyTile)) return false;
        AbstractEnergyTile energyTile = (AbstractEnergyTile) tile;
        if (!energyTile.canConfigureSide(facing)) {
            if (!world.isRemote) player.sendStatusMessage(new TextComponentTranslation("chat.powah.wrench.config.fixed"), true);
            return true;
        }
        if (!world.isRemote) {
            EnergyPortMode current = energyTile.getSideMode(facing);
            EnergyPortMode next = current;
            EnergyPortMode candidate = current;
            for (int step = 0; step < EnergyPortMode.values().length; step++) {
                candidate = candidate.next();
                if (energyTile.isSideModeSupported(facing, candidate)) {
                    next = candidate;
                    break;
                }
            }
            energyTile.setSideMode(facing, next);
            player.sendStatusMessage(new TextComponentTranslation("chat.powah.wrench.config",
                    facing.getName(), next.name()), true);
        }
        return true;
    }

    private static TileEntity resolveConfigTile(World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileReactorPart) {
            TileReactor core = ((TileReactorPart) tile).getCore();
            if (core != null) return core;
        }
        return tile;
    }

    private boolean link(EntityPlayer player, World world, BlockPos pos, EnumFacing side, ItemStack wrench) {
        TileEntity tile = world.getTileEntity(pos);
        boolean rod = tile instanceof TileEnergizingRod;
        boolean orb = tile instanceof TileEnergizingOrb;
        if (!rod && !orb) return false;
        if (world.isRemote) return true;

        NBTTagCompound tag = getOrCreateTag(wrench);
        int storedType = tag.getByte(NBT_LINK_TYPE) & 0xFF;
        if (rod && storedType == LINK_ORB) {
            BlockPos orbPos = readLinkPos(tag);
            boolean ok = BlockProtection.canEdit(player, world, orbPos, side, wrench)
                    && linkRodToOrb(world, pos, orbPos);
            clearLink(tag);
            sendLinkResult(player, ok);
            return true;
        }
        if (orb && storedType == LINK_ROD) {
            BlockPos rodPos = readLinkPos(tag);
            boolean ok = BlockProtection.canEdit(player, world, rodPos, side, wrench)
                    && linkRodToOrb(world, rodPos, pos);
            clearLink(tag);
            sendLinkResult(player, ok);
            return true;
        }

        writeLinkPos(tag, rod ? LINK_ROD : LINK_ORB, pos);
        player.sendStatusMessage(colored("chat.powah.wrench.link.start", TextFormatting.YELLOW), true);
        return true;
    }

    private static boolean linkRodToOrb(World world, BlockPos rodPos, BlockPos orbPos) {
        if (rodPos == null || orbPos == null || !world.isBlockLoaded(rodPos) || !world.isBlockLoaded(orbPos)) return false;
        TileEntity rodRaw = world.getTileEntity(rodPos);
        TileEntity orbRaw = world.getTileEntity(orbPos);
        if (!(rodRaw instanceof TileEnergizingRod) || !(orbRaw instanceof TileEnergizingOrb)) return false;
        long dx = rodPos.getX() - orbPos.getX();
        long dy = rodPos.getY() - orbPos.getY();
        long dz = rodPos.getZ() - orbPos.getZ();
        int distance = (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > TileEnergizingRod.configuredRange()) return false;
        ((TileEnergizingRod) rodRaw).setOrbPos(orbPos);
        return true;
    }

    private static void sendLinkResult(EntityPlayer player, boolean success) {
        player.sendStatusMessage(colored(success ? "chat.powah.wrench.link.done" : "chat.powah.wrench.link.fail",
                success ? TextFormatting.GOLD : TextFormatting.RED), true);
    }

    private static TextComponentTranslation colored(String key, TextFormatting color) {
        TextComponentTranslation component = new TextComponentTranslation(key);
        component.getStyle().setColor(color);
        return component;
    }

    private boolean rotate(EntityPlayer player, World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (!isRotatableBlock(state.getBlock())) return false;
        PropertyDirection facing = null;
        for (IProperty<?> property : state.getPropertyKeys()) {
            if (property instanceof PropertyDirection && "facing".equals(property.getName())) {
                facing = (PropertyDirection) property;
                break;
            }
        }
        if (facing == null) return false;

        Block block = state.getBlock();
        IBlockState rotated = state;
        int attempts = facing.getAllowedValues().size();
        for (int i = 0; i < attempts; i++) {
            rotated = rotated.cycleProperty(facing);
            if (!(block instanceof BlockPowahMachine) || ((BlockPowahMachine) block).canSurviveAt(world, pos, rotated)) {
                if (!world.isRemote) {
                    world.setBlockState(pos, rotated, 3);
                    SoundType sound = state.getBlock().getSoundType(state, world, pos, player);
                    world.playSound(null, pos, sound.getPlaceSound(), SoundCategory.BLOCKS, 1.0F, 1.0F);
                }
                return true;
            }
        }
        return false;
    }

    private boolean removeWithWrench(EntityPlayer player, World world, BlockPos clickedPos, EnumFacing side, ItemStack wrench) {
        BlockPos target = removalTarget(world, clickedPos);
        if (target == null) return false;
        IBlockState state = world.getBlockState(target);
        if (!isRemovableBlock(state.getBlock())) return false;
        if (!BlockProtection.canEdit(player, world, target, side, wrench)) return false;
        if (!world.isRemote && BlockProtection.canBreak(player, world, target)) {
            world.destroyBlock(target, !player.capabilities.isCreativeMode);
        }
        return true;
    }

    private static BlockPos removalTarget(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof BlockPlayerTransmitter && state.getPropertyKeys().contains(BlockPlayerTransmitter.TOP)
                && state.getValue(BlockPlayerTransmitter.TOP).booleanValue()) {
            return pos.down();
        }
        if (block instanceof BlockReactorPart) {
            if (world.isRemote) return pos;
            TileEntity raw = world.getTileEntity(pos);
            if (raw instanceof TileReactorPart) {
                TileReactor core = ((TileReactorPart) raw).getCore();
                return core == null ? null : core.getPos();
            }
            return null;
        }
        return pos;
    }

    private static boolean isRemovableBlock(Block block) {
        return block instanceof BlockPowahMachine || block instanceof BlockCable
                || block instanceof BlockEnergizingOrb || block instanceof BlockPlayerTransmitter
                || block instanceof BlockReactorPart;
    }

    private static boolean isPowahMachineBlock(Block block) {
        return block instanceof BlockPowahMachine;
    }

    private static boolean isRotatableBlock(Block block) {
        return block instanceof BlockPowahMachine || block instanceof BlockEnergizingOrb;
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        return tag;
    }

    private static void writeLinkPos(NBTTagCompound tag, int type, BlockPos pos) {
        tag.setByte(NBT_LINK_TYPE, (byte) type);
        tag.setInteger(NBT_LINK_X, pos.getX());
        tag.setInteger(NBT_LINK_Y, pos.getY());
        tag.setInteger(NBT_LINK_Z, pos.getZ());
    }

    private static BlockPos readLinkPos(NBTTagCompound tag) {
        if ((tag.getByte(NBT_LINK_TYPE) & 0xFF) == LINK_NONE) return null;
        return new BlockPos(tag.getInteger(NBT_LINK_X), tag.getInteger(NBT_LINK_Y), tag.getInteger(NBT_LINK_Z));
    }

    private static void clearLink(NBTTagCompound tag) {
        tag.removeTag(NBT_LINK_TYPE);
        tag.removeTag(NBT_LINK_X);
        tag.removeTag(NBT_LINK_Y);
        tag.removeTag(NBT_LINK_Z);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        Mode mode = getMode(stack);
        tooltip.add(TextFormatting.GRAY + new TextComponentTranslation("info.powah.wrench.mode",
                new TextComponentTranslation("info.powah.wrench.mode." + mode.name().toLowerCase(Locale.ROOT))).getUnformattedText());
        if (mode == Mode.LINK && stack.hasTagCompound() && (stack.getTagCompound().getByte(NBT_LINK_TYPE) & 0xFF) != LINK_NONE) {
            tooltip.add(TextFormatting.DARK_GRAY + new TextComponentTranslation("chat.powah.wrench.link.start").getUnformattedText());
        }
    }
}
