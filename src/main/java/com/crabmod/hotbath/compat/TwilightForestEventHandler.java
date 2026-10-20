package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import com.crabmod.hotbath.util.CustomFluidHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import twilightforest.init.TFMobEffects;
import twilightforest.init.TFParticleType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Event handler for Twilight Forest integration.
 * 
 * Features:
 * - Remove Frosted effect when entering hot bath
 * - Grant frost resistance buff after bathing
 * - Spawn firefly particles around bath pools in Twilight Forest dimension
 */
public class TwilightForestEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();
    
    // Twilight Forest dimension key
    private static final ResourceLocation TWILIGHT_FOREST_DIMENSION = ResourceLocation.fromNamespaceAndPath("twilightforest", "twilight_forest");
    
    // How often to check for effects (every 20 ticks = 1 second)
    private static final int EFFECT_CHECK_INTERVAL = 20;
    
    // How often to spawn firefly particles (every 10 ticks = 0.5 seconds)
    private static final int PARTICLE_SPAWN_INTERVAL = 10;
    
    // Search radius for firefly particles around bath pools
    private static final int FIREFLY_PARTICLE_RADIUS = 8;
    
    // Track players who were in bath last tick
    private static final Map<UUID, Boolean> playerWasInBath = new HashMap<>();
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        CompatManager.safeEventCall("twilightforest", "onPlayerTick", () -> {
            if (event.getEntity().level().isClientSide()) return;
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            
            boolean isInBath = com.crabmod.hotbath.api.HotBathApi.allowsModIntegrations(player)&&CustomFluidHandler.isPlayerInHotBath(player);
            UUID playerId = player.getUUID();
            
            if (isInBath) {
                if (player.tickCount % EFFECT_CHECK_INTERVAL == 0) {
                    removeFrostedEffect(player);
                }
                
                playerWasInBath.put(playerId, true);
            } else {
                playerWasInBath.put(playerId, false);
            }
            
            if (player.tickCount % PARTICLE_SPAWN_INTERVAL == 0) {
                if (isInTwilightForest(player.level())) {
                    spawnFireflyParticles(player);
                }
            }
        });
    }
    
    /**
     * Remove the Frosted effect from the player.
     * The Frosted effect is a harmful effect from Twilight Forest that slows and freezes players.
     */
    private static void removeFrostedEffect(ServerPlayer player) {
        try {
            // Get the Frosted effect from Twilight Forest
            Holder<MobEffect> frostedEffect = TFMobEffects.FROSTY;
            if (frostedEffect != null && player.hasEffect(frostedEffect)) {
                player.removeEffect(frostedEffect);
                LOGGER.debug("Removed Frosted effect from player {} in hot bath", player.getName().getString());
            }
        } catch (Exception e) {
            LOGGER.debug("Could not check/remove Frosted effect: {}", e.getMessage());
        }
    }
    
    /**
     * Check if the level is the Twilight Forest dimension.
     */
    private static boolean isInTwilightForest(Level level) {
        ResourceKey<Level> dimensionKey = level.dimension();
        return dimensionKey.location().equals(TWILIGHT_FOREST_DIMENSION);
    }
    
    /**
     * Spawn firefly-like particles around nearby bath pools in Twilight Forest.
     */
    private static void spawnFireflyParticles(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        
        BlockPos playerPos = player.blockPosition();
        
        // Search for nearby hot bath blocks
        for (int x = -FIREFLY_PARTICLE_RADIUS; x <= FIREFLY_PARTICLE_RADIUS; x++) {
            for (int z = -FIREFLY_PARTICLE_RADIUS; z <= FIREFLY_PARTICLE_RADIUS; z++) {
                for (int y = -2; y <= 2; y++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    BlockState state = serverLevel.getBlockState(checkPos);
                    
                    if (state.getBlock() instanceof AbstractHotbathBlock) {
                        // Found a bath block - spawn firefly particles above it
                        if (RANDOM.nextFloat() < 0.075f) { // 7.5% chance per bath block (reduced for performance)
                            spawnFireflyParticle(serverLevel, checkPos);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Spawn a single firefly-like particle effect above a bath block.
     */
    private static void spawnFireflyParticle(ServerLevel level, BlockPos bathPos) {
        double x = bathPos.getX() + 0.5 + (RANDOM.nextDouble() - 0.5) * 2.0;
        double y = bathPos.getY() + 1.0 + RANDOM.nextDouble() * 2.0;
        double z = bathPos.getZ() + 0.5 + (RANDOM.nextDouble() - 0.5) * 2.0;
        
        // Use Twilight Forest's WANDERING_FIREFLY particles for authentic firefly effect
        level.sendParticles(
            TFParticleType.WANDERING_FIREFLY.get(),
            x, y, z,
            1, // count
            0.0, 0.0, 0.0, // offset
            0.0 // speed
        );
    }
    
    /**
     * Clean up tracking data when player disconnects.
     * Called from main mod class.
     */
    public static void onPlayerLoggedOut(UUID playerId) {
        playerWasInBath.remove(playerId);
    }
    
    /**
     * Check if an entity is a Twilight Forest ice mob that should take damage in hot bath.
     * Ice mobs include: IceCrystal, StableIceCore, UnstableIceCore, SnowGuardian, and SnowQueen.
     * @param entity The entity to check
     * @return true if the entity is an ice mob
     */
    public static boolean isIceMob(net.minecraft.world.entity.Entity entity) {
        // Check for BaseIceMob (parent class of IceCrystal, StableIceCore, UnstableIceCore, SnowGuardian)
        if (entity instanceof twilightforest.entity.monster.BaseIceMob) {
            return true;
        }
        // Check for SnowQueen (boss entity, not a subclass of BaseIceMob)
        if (entity instanceof twilightforest.entity.boss.SnowQueen) {
            return true;
        }
        return false;
    }
}
