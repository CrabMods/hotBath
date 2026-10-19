package com.crabmod.hotbath.dirtiness;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.HotBathConfig;
import com.crabmod.hotbath.events.enter_fluid_events.PeonyBathEvents;
import com.crabmod.hotbath.fluid_blocks.IInsideAreaTracker;
import com.crabmod.hotbath.util.AdvancementHelper;
import com.crabmod.hotbath.util.HealthRegenHandler;
import com.crabmod.hotbath.util.HotbathFluidHelper;
import com.crabmod.hotbath.util.HungerRegenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-side handler for dirtiness mechanics.
 * 
 * Features:
 * - Dynamic dirtiness: Environmental factors affect how fast players get dirty
 * - Gradual bathing: Standing in hot bath fluids gradually cleans the player
 * - Combat effects: Fighting adds instant dirt
 * - Periodic sync to client for visual updates
 * 
 * Performance optimizations:
 * - Only calculate multipliers once per second (20 ticks)
 * - Use WeakHashMap for per-player data to avoid memory leaks
 * - Cache biome checks until player moves significantly
 */
@Mod.EventBusSubscriber(modid = HotBath.MOD_ID)
public class DirtinessHandler {
    
    // Sync interval in ticks (every 1 second = 20 ticks)
    private static final int SYNC_INTERVAL = 20;
    
    // Dirt update interval (every 1 second for performance)
    private static final int DIRT_UPDATE_INTERVAL = 20;
    
    // Per-player factors tracking (weak references for cleanup)
    private static final Map<UUID, DirtinessFactors> playerFactors = new WeakHashMap<>();
    
    /**
     * Get or create DirtinessFactors for a player
     */
    private static DirtinessFactors getFactors(ServerPlayer player) {
        return playerFactors.computeIfAbsent(player.getUUID(), uuid -> new DirtinessFactors());
    }
    
    /**
     * Initialize dirtiness data when player logs in
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!HotBathConfig.isDirtinessEnabled()) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            DirtinessCapability.get(player).ifPresent(data -> {
                long gameTime = player.level().getGameTime();
                long seed = player.getUUID().getMostSignificantBits() ^ player.getUUID().getLeastSignificantBits();
                data.initialize(gameTime, seed);
            });
            
            // Create factors for this player
            getFactors(player);
            
            // Sync to client
            DirtinessNetworking.syncToClient(player);
        }
    }
    
    /**
     * Cleanup when player logs out
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerUUID = player.getUUID();
            playerFactors.remove(playerUUID);
            // Cleanup all memory caches to prevent memory leaks
            HealthRegenHandler.cleanup(playerUUID);
            HungerRegenHandler.cleanup(playerUUID);
            AdvancementHelper.cleanup(playerUUID);
            IInsideAreaTracker.cleanupPlayer(playerUUID);
            PeonyBathEvents.cleanup(playerUUID);
        }
    }
    
    /**
     * Copy dirtiness data on death/respawn (dimension change)
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original && 
            event.getEntity() instanceof ServerPlayer newPlayer) {
            
            // Get data from original player
            DirtinessCapability.get(original).ifPresent(originalData -> {
                DirtinessCapability.get(newPlayer).ifPresent(newData -> {
                    // Copy the data
                    newData.copyFrom(originalData);
                });
            });
            
            // Clean up memory caches on death (not dimension change)
            if (event.isWasDeath()) {
                UUID playerUUID = original.getUUID();
                HealthRegenHandler.cleanup(playerUUID);
                HungerRegenHandler.cleanup(playerUUID);
                AdvancementHelper.cleanup(playerUUID);
            }
        }
    }
    
    /**
     * Main tick handler - handles both dirt accumulation and bathing
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!HotBathConfig.isDirtinessEnabled()) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        
        DirtinessCapability.get(player).ifPresent(data -> {
            DirtinessFactors factors = getFactors(player);
            long gameTime = player.level().getGameTime();
            
            // Check if player is in hot bath fluid - apply gradual cleaning
            if (isInHotBathFluid(player)) {
                handleBathing(player, data, gameTime);
            } else {
                // Not bathing - accumulate dirt based on environment
                handleDirtAccumulation(player, data, factors, gameTime);
            }
        });
    }
    
    /**
     * Handle bathing logic - gradual cleaning
     */
    private static void handleBathing(ServerPlayer player, DirtinessData data, long gameTime) {
        progressBathing(player, data, gameTime, isPlayerMoving(player));
    }

    /**
     * Applies one tick of gradual dirtiness cleaning from an external bath container.
     *
     * <p>External integrations should validate their own container geometry and fluid
     * contents, then call this once per server tick while the player is bathing.</p>
     *
     * @param player the player to clean
     * @param isMoving whether the player is moving, which accelerates cleaning
     * @return true if dirtiness was reduced
     */
    public static boolean applyExternalBathing(ServerPlayer player, boolean isMoving) {
        if (!HotBathConfig.isDirtinessEnabled() || player == null) {
            return false;
        }

        long gameTime = player.level().getGameTime();
        return DirtinessCapability.get(player)
                .map(data -> progressBathing(player, data, gameTime, isMoving))
                .orElse(false);
    }

    private static boolean progressBathing(ServerPlayer player, DirtinessData data, long gameTime, boolean isMoving) {
        boolean wasClean = data.isClean(gameTime);

        // Gradually clean the player
        if (data.progressBath(gameTime, isMoving)) {
            // Sync more frequently during bathing for smooth visual feedback
            if (player.tickCount % 5 == 0) { // Every 0.25 seconds during bathing
                DirtinessNetworking.syncToClient(player);
            }
            
            // Notify if player just became clean
            if (!wasClean && data.isClean(gameTime)) {
                // Could add achievement/advancement/sound here
            }

            return true;
        }

        return false;
    }
    
    /**
     * Handle dirt accumulation with dynamic multipliers
     */
    private static void handleDirtAccumulation(ServerPlayer player, DirtinessData data, 
                                                DirtinessFactors factors, long gameTime) {
        // Only update dirt every DIRT_UPDATE_INTERVAL ticks for performance
        if (player.tickCount % DIRT_UPDATE_INTERVAL == 0) {
            // Check flies state BEFORE adding dirt
            boolean hadFlies = data.shouldSpawnFlies(gameTime);
            
            // Calculate environmental multiplier
            float multiplier = factors.calculateMultiplier(player, gameTime);
            
            // Add dirt based on multiplier (pass gameTime for fly tracking)
            data.addDirt(multiplier, DIRT_UPDATE_INTERVAL, gameTime);
            
            // Check if flies state just became active
            boolean hasFlies = data.shouldSpawnFlies(gameTime);
            if (!hadFlies && hasFlies) {
                // Award "Something Smells..." advancement
                AdvancementHelper.tryAwardAdvancement(player, "hotbath:something_smells", "code_triggered");
            }
            
            // Sync to client (includes fly status for client-side particle spawning)
            DirtinessNetworking.syncToClient(player);
        }
    }
    
    /**
     * Handle player taking damage - adds instant dirt
     */
    @SubscribeEvent
    public static void onPlayerHurt(LivingDamageEvent event) {
        if (!HotBathConfig.isDirtinessEnabled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        long gameTime = player.level().getGameTime();
        
        // Mark combat activity
        DirtinessFactors factors = getFactors(player);
        factors.markCombat(gameTime);
        
        // Add instant dirt from getting hurt (2%)
        DirtinessCapability.get(player).ifPresent(data -> {
            data.addInstantDirt(DirtinessFactors.getInstantHurtPercent());
            
            // Sync immediately for visual feedback
            DirtinessNetworking.syncToClient(player);
        });
    }
    
    /**
     * Handle player killing a mob - adds instant dirt
     */
    @SubscribeEvent
    public static void onMobKilled(LivingDeathEvent event) {
        if (!HotBathConfig.isDirtinessEnabled()) return;
        
        // Check if the killer is a player
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            long gameTime = player.level().getGameTime();
            
            // Mark combat activity
            DirtinessFactors factors = getFactors(player);
            factors.markCombat(gameTime);
            
            // Add instant dirt from killing (3%)
            DirtinessCapability.get(player).ifPresent(data -> {
                data.addInstantDirt(DirtinessFactors.getInstantKillPercent());
                
                // Sync immediately for visual feedback
                DirtinessNetworking.syncToClient(player);
            });
        }
    }
    
    /**
     * Check if player is moving (for accelerated bathing)
     * Returns true if player has significant horizontal or vertical movement
     */
    private static boolean isPlayerMoving(ServerPlayer player) {
        // Check movement delta
        double dx = player.getX() - player.xOld;
        double dy = player.getY() - player.yOld;
        double dz = player.getZ() - player.zOld;
        double speed = dx * dx + dy * dy + dz * dz;
        
        // Threshold for "moving" - small movements don't count
        // Walking speed is about 0.1 blocks/tick, so 0.01 squared
        return speed > 0.001;
    }
    
    /**
     * Check if player is standing in a hot bath fluid.
     * This includes:
     * - Built-in hot bath fluids (hot water, honey bath, milk bath, herbal bath, peony bath, rose bath)
     * - Custom fluids from data packs (DynamicCustomFluid)
     */
    private static boolean isInHotBathFluid(ServerPlayer player) {
        // Check the block at player's feet position
        BlockPos feetPos = player.blockPosition();
        if (isHotbathFluidAtPosition(player, feetPos)) {
            return true;
        }
        
        // Also check slightly above (for eye-level immersion)
        BlockPos eyePos = BlockPos.containing(player.getEyePosition());
        if (isHotbathFluidAtPosition(player, eyePos)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if there's a hotbath fluid at the given position.
     */
    private static boolean isHotbathFluidAtPosition(ServerPlayer player, BlockPos pos) {
        FluidState fluidState = player.level().getFluidState(pos);
        
        // Check if the fluid itself is a hotbath fluid (built-in or custom)
        if (!fluidState.isEmpty()) {
            if (HotbathFluidHelper.isHotbathFluid(fluidState.getType())
                    && com.crabmod.hotbath.api.HotBathApi.isCleansingFluid(fluidState.getType())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Called when player takes a bath in any hot bath fluid (instant clean)
     */
    public static void onPlayerBath(ServerPlayer player) {
        DirtinessCapability.get(player).ifPresent(data -> {
            data.takeBath(player.level().getGameTime());
            
            // Immediately sync to client for instant visual feedback
            DirtinessNetworking.syncToClient(player);
        });
    }
    
    /**
     * Get current dirtiness for a player (0.0 to 1.0)
     */
    public static float getDirtiness(Player player) {
        if (player.level().isClientSide) {
            // Client side - use cached value
            return DirtinessClientData.getDirtiness(player.getUUID());
        } else {
            // Server side - calculate from data
            return DirtinessCapability.get(player)
                    .map(data -> data.getDirtiness(player.level().getGameTime()))
                    .orElse(0.0f);
        }
    }
    
    /**
     * Add dirt to a player from external sources (for other mod compatibility)
     */
    public static void addDirtToPlayer(ServerPlayer player, float percentage) {
        DirtinessCapability.get(player).ifPresent(data -> {
            data.addInstantDirt(percentage);
            DirtinessNetworking.syncToClient(player);
        });
    }
}
