package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.util.CustomFluidHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import vectorwing.farmersdelight.common.registry.ModEffects;

/**
 * Event handler for Farmer's Delight integration.
 * 
 * Features:
 * - Grant Nourishment effect when bathing in hot water
 *   Nourishment now covers Farmer's Delight's retired Comfort role
 */
public class FarmersDelightEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // How often to apply Nourishment effect (every 40 ticks = 2 seconds)
    private static final int EFFECT_APPLY_INTERVAL = 40;
    
    // Nourishment effect duration when in bath (5 seconds, will be refreshed)
    private static final int NOURISHMENT_DURATION = 100;
    
    // Nourishment effect amplifier (0 = level I)
    private static final int NOURISHMENT_AMPLIFIER = 0;
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        CompatManager.safeEventCall("farmersdelight", "onPlayerTick", () -> {
            if (event.getEntity().level().isClientSide()) return;
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            
            boolean isInBath = com.crabmod.hotbath.api.HotBathApi.allowsModIntegrations(player)&&CustomFluidHandler.isPlayerInHotBath(player);
            
            if (isInBath) {
                if (player.tickCount % EFFECT_APPLY_INTERVAL == 0) {
                    grantNourishmentEffect(player);
                }
            }
        });
    }
    
    /**
     * Grant the Nourishment effect to the player while bathing.
     * This represents the relaxing, healing nature of a hot bath.
     */
    private static void grantNourishmentEffect(ServerPlayer player) {
        try {
            Holder<MobEffect> nourishmentEffect = ModEffects.NOURISHMENT;
            if (nourishmentEffect != null) {
                // Only apply if player doesn't already have a longer duration
                MobEffectInstance currentEffect = player.getEffect(nourishmentEffect);
                if (currentEffect == null || currentEffect.getDuration() < NOURISHMENT_DURATION) {
                    player.addEffect(new MobEffectInstance(
                        nourishmentEffect,
                        NOURISHMENT_DURATION,
                        NOURISHMENT_AMPLIFIER,
                        false,  // Not ambient
                        true,   // Show particles
                        true    // Show icon
                    ));
                    LOGGER.debug("Granted Nourishment effect to player {} in hot bath", player.getName().getString());
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not apply Nourishment effect: {}", e.getMessage());
        }
    }
}
