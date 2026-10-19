package com.crabmod.hotbath.items;

import com.crabmod.hotbath.HotBathConfig;
import com.crabmod.hotbath.dirtiness.DirtinessCapability;
import com.crabmod.hotbath.dirtiness.DirtinessData;
import com.crabmod.hotbath.dirtiness.DirtinessNetworking;
import com.crabmod.hotbath.registers.EntityRegister;
import com.crabmod.hotbath.registers.ItemRegister;
import com.crabmod.hotbath.registers.ParticleRegister;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class ThrownBathWater extends ThrowableItemProjectile {

    public ThrownBathWater(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public ThrownBathWater(Level level, LivingEntity shooter) {
        super(EntityRegister.THROWN_BATH_WATER.get(), shooter, level);
    }

    public ThrownBathWater(Level level, double x, double y, double z) {
        super(EntityRegister.THROWN_BATH_WATER.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ItemRegister.HOT_WATER_BOTTLE.get(); // Default fallback
    }

    @Override
    protected float getGravity() {
        return 0.05F;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sounds.SoundEvents.SPLASH_POTION_BREAK, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, this.random.nextFloat() * 0.1F + 0.9F);
            ItemStack stack = this.getItem();
            if (stack.getItem() instanceof SplashBathWaterBottleItem splashItem) {
                applySplash(splashItem);
            }
            this.level().broadcastEntityEvent(this, (byte)3);
            this.discard();
        }
    }

    private void applySplash(SplashBathWaterBottleItem splashItem) {
        AABB aabb = this.getBoundingBox().inflate(4.0D, 2.0D, 4.0D);
        List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, aabb);

        if (!list.isEmpty()) {
            for (LivingEntity livingentity : list) {
                if (livingentity.isAffectedByPotions()) {
                    double d0 = this.distanceToSqr(livingentity);
                    if (d0 < 16.0D) {
                        splashItem.applyEffect(livingentity);
                        
                        // Apply this native bath bottle's configured dirtiness reduction.
                        if (HotBathConfig.isDirtinessEnabled() && livingentity instanceof ServerPlayer serverPlayer) {
                            DirtinessData data = DirtinessCapability.getOrNull(serverPlayer);
                            if (data != null) {
                                long gameTime = serverPlayer.level().getGameTime();
                                data.reduceDirtiness(gameTime, com.crabmod.hotbath.custom_fluid.BuiltinBathDefinitions.splashCleaning(splashItem));
                                DirtinessNetworking.syncToClient(serverPlayer);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            ItemStack stack = this.getItem();

            if (stack.getItem() instanceof SplashBathWaterBottleItem splashItem) {
                int color = splashItem.getColor();
                double r = ((color >> 16) & 0xFF) / 255.0;
                double g = ((color >> 8) & 0xFF) / 255.0;
                double b = (color & 0xFF) / 255.0;

                // Effect particles using vanilla ENTITY_EFFECT with dynamic color (like vanilla potion)
                // In 1.20.1 Forge, ENTITY_EFFECT uses speed parameters as RGB color
                for(int k = 0; k < 100; ++k) {
                    double d3 = this.random.nextDouble() * 4.0D;
                    double d4 = this.random.nextDouble() * Math.PI * 2.0D;
                    double d5 = Math.cos(d4) * d3;
                    double d7 = Math.sin(d4) * d3;
                    this.level().addParticle(ParticleTypes.ENTITY_EFFECT, this.getX() + d5 * 0.1D, this.getY() + 0.3D, this.getZ() + d7 * 0.1D, r, g, b);
                }

                // Steam particles - Concentrated Center (Rising faster)
                for(int k = 0; k < 10; ++k) {
                    double radius = this.random.nextDouble() * 0.5D;
                    double angle = this.random.nextDouble() * Math.PI * 2.0D;
                    double offsetX = Math.cos(angle) * radius;
                    double offsetZ = Math.sin(angle) * radius;

                    this.level().addParticle(ParticleRegister.STEAM_PARTICLE.get(), 
                        this.getX() + offsetX, 
                        this.getY() + 0.2D, 
                        this.getZ() + offsetZ, 
                        0.0D, 0.1D + this.random.nextDouble() * 0.05D, 0.0D);
                }

                // Steam particles - Dispersed Surroundings (Rising slower)
                for(int k = 0; k < 15; ++k) {
                    double radius = 0.5D + this.random.nextDouble() * 2.0D;
                    double angle = this.random.nextDouble() * Math.PI * 2.0D;
                    double offsetX = Math.cos(angle) * radius;
                    double offsetZ = Math.sin(angle) * radius;

                    this.level().addParticle(ParticleRegister.STEAM_PARTICLE.get(), 
                        this.getX() + offsetX, 
                        this.getY() + 0.1D, 
                        this.getZ() + offsetZ, 
                        0.0D, 0.02D + this.random.nextDouble() * 0.03D, 0.0D);
                }
            } else {
                // Default splash particles
                for(int i = 0; i < 8; ++i) {
                    this.level().addParticle(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.2D, this.getZ(), 
                        ((double)this.random.nextFloat() - 0.5D) * 0.08D, 
                        ((double)this.random.nextFloat() - 0.5D) * 0.08D, 
                        ((double)this.random.nextFloat() - 0.5D) * 0.08D);
                }
            }
            
            // Also add item break particles
            for(int j = 0; j < 8; ++j) {
                 this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack), this.getX(), this.getY() + 0.2D, this.getZ(), ((double)this.random.nextFloat() - 0.5D) * 0.15D, 0.15D, ((double)this.random.nextFloat() - 0.5D) * 0.15D);
            }
        }
    }
}










