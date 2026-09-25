package com.sosea1.powah.content.special;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/** Charged snowball: vanilla thrown-hit semantics plus a real lightning strike on impact. */
public final class EntityChargedSnowball extends EntitySnowball {
    public EntityChargedSnowball(World world) { super(world); }
    public EntityChargedSnowball(World world, EntityLivingBase thrower) { super(world, thrower); }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (result.entityHit != null) {
            Entity target = result.entityHit;
            float damage = target instanceof EntityBlaze ? 3.0F : 0.0F;
            target.attackEntityFrom(DamageSource.causeThrownDamage(this, getThrower()), damage);
        }
        if (!world.isRemote) {
            world.addWeatherEffect(new EntityLightningBolt(world, posX, posY, posZ, false));
            world.setEntityState(this, (byte) 3);
            setDead();
        }
    }
}
