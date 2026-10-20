package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.util.CustomFluidHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import toughasnails.api.temperature.IPlayerTemperatureModifier;
import toughasnails.api.temperature.TemperatureLevel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player temperature modifier for Tough As Nails integration.
 * When a player is inside a hot bath block, their temperature is set to WARM.
 * Only applies to built-in hot baths and custom fluids with temperature >= 35°C.
 */
public class HotBathTANPlayerModifier implements IPlayerTemperatureModifier {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Track when players were last seen in a hot bath
    private static final Map<UUID, Long> LAST_IN_BATH = new ConcurrentHashMap<>();
    private static final long LINGER_DURATION_MS = 10_000;

    public static void cleanup(Player player) {
        LAST_IN_BATH.remove(player.getUUID());
    }

    @Override
    public TemperatureLevel modify(Player player, TemperatureLevel current) {
        if(!com.crabmod.hotbath.api.HotBathApi.allowsModIntegrations(player)) {
            cleanup(player);return current;
        }
        // Check if the player is inside a HOT bath block (not just any bath block)
        // This will return false for custom fluids with temperature < 35°C
        boolean inBath = CustomFluidHandler.isPlayerInHotBath(player);
        long currentTime = System.currentTimeMillis();
        UUID uuid = player.getUUID();

        if (inBath) {
            if (!LAST_IN_BATH.containsKey(uuid)) {
                 LOGGER.info("Player {} entered hot bath. Applying WARM temperature.", player.getName().getString());
            }
            // Update last seen time
            LAST_IN_BATH.put(uuid, currentTime);
            // Return WARM temperature level (corresponding to comfortable 37°C body temperature)
            return TemperatureLevel.WARM;
        } else {
            // Check for linger effect
            Long lastSeen = LAST_IN_BATH.get(uuid);
            if (lastSeen != null) {
                if (currentTime - lastSeen < LINGER_DURATION_MS) {
                    return TemperatureLevel.WARM;
                } else {
                    // Cleanup if expired to save memory
                    LAST_IN_BATH.remove(uuid);
                    LOGGER.info("Player {} hot bath effect expired.", player.getName().getString());
                }
            }
        }

        // If not inside a hot bath and no linger effect, return current temperature unchanged
        return current;
    }
}
