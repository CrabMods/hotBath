package com.crabmod.hotbath.fluid_blocks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interface for tracking when entities enter and stay in bath areas.
 * Uses memory cache instead of PersistentData for better performance.
 */
public interface IInsideAreaTracker {
    
    /**
     * Result of tracking an entity inside an area.
     */
    record InsideAreaResult(
            boolean isFirstEnter,
            boolean shouldProcess,
            int stayedTicks,
            int totalEnterCount
    ) {
    }
    
    /**
     * Internal state cache for tracking area entry.
     */
    record AreaState(int lastInsideTick, int stayedTime, int enterCount) {}
    
    /**
     * Global cache: AreaKey -> (PlayerUUID -> AreaState)
     */
    Map<String, Map<UUID, AreaState>> AREA_STATE_CACHE = new ConcurrentHashMap<>();

    default int reenterThresholdTicks() {
        return 10;
    }

    /**
     * Track any entity inside the area (players, mobs, etc.)
     */
    Map<net.minecraft.world.entity.LivingEntity, Map<String, AreaState>> MOB_AREA_STATE_CACHE =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    default InsideAreaResult trackInside(Entity entity) {
        if (entity instanceof net.minecraft.world.entity.LivingEntity living && !(entity instanceof ServerPlayer)) {
            var cache = MOB_AREA_STATE_CACHE.computeIfAbsent(living, k -> new java.util.HashMap<>());
            String key = getAreaKey();
            return advance(cache.get(key), entity.tickCount, reenterThresholdTicks(), state -> cache.put(key, state));
        }
        var cache = AREA_STATE_CACHE.computeIfAbsent(getAreaKey(), k -> new ConcurrentHashMap<>());
        UUID uuid = entity.getUUID();
        return advance(cache.get(uuid), entity.tickCount, reenterThresholdTicks(), state -> cache.put(uuid, state));
    }

    private static InsideAreaResult advance(AreaState state, int currentTick, int threshold,
                                            java.util.function.Consumer<AreaState> save) {
        int lastInsideTick = state != null ? state.lastInsideTick() : 0;
        int stayedTime = state != null ? state.stayedTime() : 0;
        int enterCount = state != null ? state.enterCount() : 0;

        if (lastInsideTick == currentTick) {
            return new InsideAreaResult(false, false, stayedTime, enterCount);
        }

        boolean isFirstEnter = lastInsideTick == 0
                || currentTick < lastInsideTick
                || (currentTick - lastInsideTick) > threshold;

        int newEnterCount = enterCount;
        int newStayedTime;

        if (isFirstEnter) {
            newEnterCount++;
            newStayedTime = 0;
        } else {
            newStayedTime = stayedTime + 1;
        }

        save.accept(new AreaState(currentTick, newStayedTime, newEnterCount));
        return new InsideAreaResult(isFirstEnter, true, newStayedTime, newEnterCount);
    }

    default String getAreaKey() {
        return getClass().getSimpleName();
    }
    
    /**
     * Clean up player data from all area caches.
     * Should be called when player logs out or dies.
     */
    static void cleanupPlayer(UUID playerUUID) {
        AREA_STATE_CACHE.values().forEach(cache -> cache.remove(playerUUID));
    }
    
    /**
     * Get the enter count for a player in a specific area.
     * Useful for advancement checks without triggering a full track.
     */
    default int getEnterCount(ServerPlayer player) {
        String key = getAreaKey();
        Map<UUID, AreaState> playerCache = AREA_STATE_CACHE.get(key);
        if (playerCache == null) return 0;
        AreaState state = playerCache.get(player.getUUID());
        return state != null ? state.enterCount() : 0;
    }
}
