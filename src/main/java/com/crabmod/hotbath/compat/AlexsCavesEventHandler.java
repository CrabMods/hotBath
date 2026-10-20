package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.HotBathConfig;
import com.crabmod.hotbath.dirtiness.DirtinessCapability;
import com.crabmod.hotbath.dirtiness.DirtinessData;
import com.crabmod.hotbath.entity.HotSpringCatBehavior;
import com.crabmod.hotbath.util.CustomFluidHandler;
import com.github.alexmodguy.alexscaves.server.entity.living.GammaroachEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.GummyBearEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.RaycatEntity;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Random;

/**
 * Event handler for Alex's Caves integration.
 * 
 * Features:
 * - GummyBear takes damage in hot water (melting! 0.5 damage per second)
 * - Herbal bath can cure the IRRADIATED effect (reduces level by 1 every 5 seconds)
 * - Gammaroach is attracted to dirty players (similar to cockroach behavior)
 * - Gammaroach attacks players with fly status (extremely dirty)
 * - Raycat sits near hot springs (uses shared HotSpringCatBehavior)
 */
public class AlexsCavesEventHandler {
    
    private static final Random RANDOM = new Random();
    
    // Tick intervals for performance
    private static final int GUMMY_DAMAGE_INTERVAL = 20; // 1 second
    private static final int RADIATION_CURE_INTERVAL = 100; // 5 seconds
    private static final int GAMMAROACH_CHECK_INTERVAL = 40; // 2 seconds
    
    // Gammaroach behavior constants
    private static final double GAMMAROACH_ATTRACTION_RANGE = 16.0;
    private static final double GAMMAROACH_MIN_DISTANCE = 2.0;
    private static final double GAMMAROACH_MAX_DISTANCE = 5.0;
    
    // ==================== Main Entity Tick Handler ====================
    
    /**
     * Handle all entity tick events for Alex's Caves entities.
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        CompatManager.safeEventCall("alexscaves", "onLivingTick", () -> {
            if (event.getEntity().level().isClientSide()) return;
            
            LivingEntity entity = event.getEntity();
            
            // Handle GummyBear melting
            if (entity instanceof GummyBearEntity gummyBear) {
                handleGummyBear(gummyBear);
            }
            // Handle Gammaroach for players
            else if (entity instanceof ServerPlayer player) {
                handleGammaroachAttraction(player);
            }
            // Handle Raycat hot spring sitting
            else if (entity instanceof RaycatEntity raycat) {
                handleRaycat(raycat);
            }
            
            // Handle radiation cure for any living entity
            handleRadiationCure(entity);
        });
    }
    
    // ==================== GummyBear Melting ====================
    
    private static void handleGummyBear(GummyBearEntity gummyBear) {
        if (gummyBear.tickCount % GUMMY_DAMAGE_INTERVAL != 0) return;
        if (com.crabmod.hotbath.api.HotBathApi.allowsModIntegrations(gummyBear)&&CustomFluidHandler.isEntityInHotBathLiquid(gummyBear)) {
            gummyBear.hurt(gummyBear.level().damageSources().magic(), 0.5F);
        }
    }

    // ==================== Radiation Cure in Herbal Bath ====================
    
    private static void handleRadiationCure(LivingEntity living) {
        // Only check every 5 seconds for performance
        if (living.tickCount % RADIATION_CURE_INTERVAL != 0) return;

        // Check if entity has IRRADIATED effect
        MobEffectInstance radiation = living.getEffect(ACEffectRegistry.IRRADIATED.get());
        if (radiation == null) return;

        // Check if entity is in herbal bath
        if (com.crabmod.hotbath.api.HotBathApi.allowsModIntegrations(living)&&CustomFluidHandler.isEntityInHerbalBathBlock(living)) {
            int currentLevel = radiation.getAmplifier();
            int duration = radiation.getDuration();
            
            // Remove current effect and sync to clients for non-player entities
            living.removeEffect(ACEffectRegistry.IRRADIATED.get());
            if (!(living instanceof ServerPlayer) && living.level() instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().broadcastAndSend(living,
                        new ClientboundRemoveMobEffectPacket(living.getId(), ACEffectRegistry.IRRADIATED.get()));
            }

            // If level > 0, apply reduced level effect
            if (currentLevel > 0) {
                living.addEffect(new MobEffectInstance(
                        ACEffectRegistry.IRRADIATED.get(),
                        duration,
                        currentLevel - 1,
                        false,
                        true,
                        true
                ));
            }
            // If level was 0, effect is fully removed (already done above)
        }
    }
    
    // ==================== Gammaroach Attraction to Dirty Players ====================
    
    private static void handleGammaroachAttraction(ServerPlayer player) {
        // Check if dirtiness system is enabled
        if (!HotBathConfig.isDirtinessEnabled()) return;
        
        // Only check every CHECK_INTERVAL ticks for performance
        if (player.tickCount % GAMMAROACH_CHECK_INTERVAL != 0) return;
        
        // Get dirtiness data
        DirtinessData data = DirtinessCapability.getOrNull(player);
        if (data == null) return;
        
        long gameTime = player.level().getGameTime();
        boolean hasFlies = data.shouldSpawnFlies(gameTime);
        float dirtiness = data.getDirtiness(gameTime);
        
        // Only attract if player is at least 80% dirty
        if (dirtiness < 0.8f) return;
        
        // Find nearby gammaroaches
        AABB searchBox = player.getBoundingBox().inflate(GAMMAROACH_ATTRACTION_RANGE);
        List<GammaroachEntity> gammaroaches = player.level().getEntitiesOfClass(
                GammaroachEntity.class,
                searchBox,
                roach -> roach.isAlive()
        );
        
        for (GammaroachEntity roach : gammaroaches) {
            double distance = roach.distanceTo(player);
            
            if (hasFlies) {
                // Extremely dirty - gammaroach attacks!
                if (roach.getTarget() == null && RANDOM.nextFloat() < 0.5f) {
                    roach.setTarget(player);
                }
            } else {
                // Just dirty - gammaroach loiters nearby (like cockroach)
                if (RANDOM.nextFloat() > dirtiness * 0.5f) continue;
                
                if (distance > GAMMAROACH_MAX_DISTANCE) {
                    // Too far - move closer
                    Vec3 direction = player.position().subtract(roach.position()).normalize();
                    double targetDist = GAMMAROACH_MIN_DISTANCE + RANDOM.nextDouble() * 
                            (GAMMAROACH_MAX_DISTANCE - GAMMAROACH_MIN_DISTANCE);
                    Vec3 targetPos = player.position().subtract(direction.scale(targetDist));
                    targetPos = targetPos.add(
                            (RANDOM.nextDouble() - 0.5) * 2.0,
                            0,
                            (RANDOM.nextDouble() - 0.5) * 2.0
                    );
                    roach.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 0.8);
                } else if (distance < GAMMAROACH_MIN_DISTANCE) {
                    // Too close - back off
                    Vec3 direction = roach.position().subtract(player.position()).normalize();
                    double targetDist = GAMMAROACH_MIN_DISTANCE + RANDOM.nextDouble() * 2.0;
                    Vec3 targetPos = player.position().add(direction.scale(targetDist));
                    roach.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.0);
                }
            }
        }
    }
    
    // ==================== Raycat Hot Spring Sitting ====================
    
    /**
     * Raycat will sit near hot springs, similar to how vanilla cats sit on chests.
     * Uses shared HotSpringCatBehavior for consistent behavior with vanilla cats.
     */
    private static void handleRaycat(RaycatEntity raycat) {
        // Create adapter for Raycat (same interface as vanilla Cat)
        HotSpringCatBehavior.SittableEntity adapter = createRaycatAdapter(raycat);
        
        // Process sitting behavior using shared logic
        boolean isSitting = HotSpringCatBehavior.processCatTick(raycat, adapter);
        
        // If not sitting, try to attract to hot spring
        if (!isSitting) {
            HotSpringCatBehavior.attractToHotSpring(raycat, adapter);
        }
    }
    
    /**
     * Create adapter for RaycatEntity to work with HotSpringCatBehavior.
     */
    private static HotSpringCatBehavior.SittableEntity createRaycatAdapter(RaycatEntity raycat) {
        return new HotSpringCatBehavior.SittableEntity() {
            @Override
            public boolean isInSittingPose() {
                return raycat.isInSittingPose();
            }
            
            @Override
            public void setInSittingPose(boolean sitting) {
                raycat.setInSittingPose(sitting);
            }
            
            @Override
            public boolean isTame() {
                return raycat.isTame();
            }
            
            @Override
            public boolean isOrderedToSit() {
                return raycat.isOrderedToSit();
            }
        };
    }
}
