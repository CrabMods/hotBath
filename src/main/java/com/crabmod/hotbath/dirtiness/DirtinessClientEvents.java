package com.crabmod.hotbath.dirtiness;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.compat.EmfDirtinessCompat;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client-side events for dirtiness rendering system.
 * Uses MOD bus for EntityRenderersEvent.AddLayers registration.
 */
@EventBusSubscriber(modid = HotBath.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class DirtinessClientEvents {
    
    /**
     * Add the dirt overlay render layer to player renderers
     */
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        EmfDirtinessCompat.initialize();
        // Add to default player renderer
        PlayerRenderer defaultRenderer = event.getSkin(PlayerSkin.Model.WIDE);
        if (defaultRenderer != null) {
            defaultRenderer.addLayer(new DirtinessOverlayRenderer(defaultRenderer));
        }
        
        // Add to slim player renderer
        PlayerRenderer slimRenderer = event.getSkin(PlayerSkin.Model.SLIM);
        if (slimRenderer != null) {
            slimRenderer.addLayer(new DirtinessOverlayRenderer(slimRenderer));
        }
        
        HotBath.LOGGER.info("Dirtiness render layers registered successfully");
    }
}
