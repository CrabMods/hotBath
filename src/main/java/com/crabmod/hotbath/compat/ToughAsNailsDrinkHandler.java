package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.items.BathWaterEffects;
import com.crabmod.hotbath.util.CustomFluidHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * Handler for ToughAsNails drinking integration.
 * Applies our bath water effects when players drink from our bath water sources.
 */
public class ToughAsNailsDrinkHandler {

    /**
     * When a player finishes drinking any item while in a hot bath block,
     * apply the appropriate bath water temperature effects.
     * This covers ToughAsNails' hand drinking feature when used in our bath water.
     */
    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        CompatManager.safeEventCall("toughasnails", "onItemUseFinish", () -> {
            // Only process on server side
            if (event.getEntity().level().isClientSide()) {
                return;
            }
            
            // Only process for players
            if (!(event.getEntity() instanceof Player player)) {
                return;
            }
            
            // Check if player is in any hot bath block
            if (com.crabmod.hotbath.api.HotBathApi.allowsModIntegrations(player)&&CustomFluidHandler.isPlayerInHotBath(player)) {
                BathWaterEffects.applyTemperatureEffectsOnly(player);
                
                // If LSO is loaded, also restore thirst
                if (LegendarySurvivalOverhaulIntegration.isLSOLoaded()) {
                    LSOApiHelper.addThirst(player);
                }
            }
        });
    }
}










