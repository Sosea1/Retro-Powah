package com.sosea1.powah.content.special;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityEndermite;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.sosea1.powah.Powah;
import com.sosea1.powah.common.config.PowahConfig;
import com.sosea1.powah.registry.ModContent;

@Mod.EventBusSubscriber(modid = Powah.MOD_ID)
public final class SpecialProgressionHandler {
    private SpecialProgressionHandler() {}

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;
        ItemStack held = event.getItemStack();
        if (held.isEmpty()) return;
        EntityLivingBase target = event.getTarget() instanceof EntityLivingBase ? (EntityLivingBase) event.getTarget() : null;
        if (target == null) return;

        if (PowahConfig.playerAerialPearl() && held.getItem() == ModContent.aerialPearl() && isZombieConversionTarget(target)) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
            consumeAndGive(player, held, new ItemStack(ModContent.playerAerialPearl()));
            finishConversion(target, SoundEvents.ENTITY_ZOMBIE_DEATH);
            return;
        }

        if (!isEnderConversionTarget(target)) return;
        if (PowahConfig.lensOfEnder() && held.getItem() == ModContent.photoelectricPane()) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
            consumeAndGive(player, held, new ItemStack(ModContent.lensOfEnder()));
            finishConversion(target, SoundEvents.ENTITY_ENDERMEN_DEATH);
            return;
        }

        if (PowahConfig.dimensionalBindingCard() && held.getItem() == ModContent.bindingCard()) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
            ItemStack result = new ItemStack(ModContent.dimensionalBindingCard());
            if (held.hasTagCompound()) result.setTagCompound(held.getTagCompound().copy());
            player.setHeldItem(event.getHand(), result);
            player.inventory.markDirty();
            finishConversion(target, SoundEvents.ENTITY_ENDERMEN_DEATH);
        }
    }

    private static boolean isZombieConversionTarget(EntityLivingBase target) {
        Class<?> type = target.getClass();
        return type == EntityZombie.class || type == EntityZombieVillager.class || type == EntityHusk.class;
    }

    private static boolean isEnderConversionTarget(EntityLivingBase target) {
        Class<?> type = target.getClass();
        return type == EntityEnderman.class || type == EntityEndermite.class;
    }

    private static void consumeAndGive(EntityPlayer player, ItemStack held, ItemStack result) {
        if (!player.capabilities.isCreativeMode) held.shrink(1);
        if (!player.inventory.addItemStackToInventory(result)) player.dropItem(result, false);
        player.inventory.markDirty();
    }

    private static void finishConversion(EntityLivingBase target, SoundEvent sound) {
        target.playSound(sound, 0.5F, 1.0F);
        target.setDead();
    }
}
