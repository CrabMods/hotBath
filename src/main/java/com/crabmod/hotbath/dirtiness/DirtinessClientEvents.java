package com.crabmod.hotbath.dirtiness;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.compat.EmfDirtinessCompat;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side events for dirtiness rendering system.
 * Uses MOD bus for EntityRenderersEvent.AddLayers registration.
 */
@Mod.EventBusSubscriber(modid = HotBath.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DirtinessClientEvents {
    
    /**
     * Add the dirt overlay render layer to player renderers
     */
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        EmfDirtinessCompat.initialize();
        // Add to default player renderer
        PlayerRenderer defaultRenderer = event.getSkin("default");
        if (defaultRenderer != null) {
            defaultRenderer.addLayer(new DirtinessOverlayRenderer(defaultRenderer));
        }
        
        // Add to slim player renderer
        PlayerRenderer slimRenderer = event.getSkin("slim");
        if (slimRenderer != null) {
            slimRenderer.addLayer(new DirtinessOverlayRenderer(slimRenderer));
        }
        
        HotBath.LOGGER.info("Dirtiness render layers registered successfully");
    }
}
